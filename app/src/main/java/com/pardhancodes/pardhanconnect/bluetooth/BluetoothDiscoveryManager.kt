package com.pardhancodes.pardhanconnect.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.ParcelUuid
import android.util.Log
import com.pardhancodes.pardhanconnect.data.model.BluetoothDeviceDomain
import com.pardhancodes.pardhanconnect.data.model.PardhanProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages Dual Bluetooth Discovery (Classic Inquiry + BLE Scanner & BLE Advertiser).
 */
class BluetoothDiscoveryManager(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val permissionManager: BluetoothPermissionManager
) {
    companion object {
        private const val TAG = "PardhanDiscovery"
    }

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceDomain>> = _discoveredDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>> = _pairedDevices.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var isReceiverRegistered = false

    // BLE Scanner & Advertiser
    private var bleScanner: BluetoothLeScanner? = null
    private var bleAdvertiser: BluetoothLeAdvertiser? = null

    // BroadcastReceiver for Classic Bluetooth discovery
    private val classicDiscoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val rssi: Short =
                        intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)

                    device?.let { addOrUpdateDiscoveredDevice(it, rssi.toInt(), isPardhan = false) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isDiscovering.value = true
                    Log.d(TAG, "Classic Bluetooth discovery started")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isDiscovering.value = false
                    Log.d(TAG, "Classic Bluetooth discovery finished")
                }
            }
        }
    }

    // BLE Scan Callback (identifies devices broadcasting Pardhan Service UUID)
    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                // Devices matching the Pardhan Service UUID filter are identified
                addOrUpdateDiscoveredDevice(device, result.rssi, isPardhan = true)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            _errorMessage.value = "BLE scan failed with error code $errorCode"
            Log.e(TAG, "BLE scan failed: $errorCode")
        }
    }

    // BLE Advertising Callback (advertises Pardhan presence without exposing personal data)
    private val bleAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            _isAdvertising.value = true
            Log.d(TAG, "Pardhan BLE advertisement started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            _isAdvertising.value = false
            _errorMessage.value = "BLE Advertising failed: $errorCode"
            Log.e(TAG, "BLE advertise failed: $errorCode")
        }
    }

    /**
     * Loads bonded/paired devices from the local BluetoothAdapter.
     */
    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (!permissionManager.checkPermissions()) return
        val bonded = bluetoothAdapter?.bondedDevices.orEmpty()
        _pairedDevices.value = bonded.map { device ->
            BluetoothDeviceDomain(
                name = device.name ?: "Unknown Device",
                address = device.address,
                isPardhanDevice = device.name?.contains("Pardhan", ignoreCase = true) == true,
                isPaired = true,
                bondState = BluetoothDevice.BOND_BONDED
            )
        }
    }

    /**
     * Starts dual discovery:
     * 1. Classic Bluetooth startDiscovery() for legacy RFCOMM pairing.
     * 2. BLE Scanner filtered by Pardhan Service UUID for rapid detection.
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (!permissionManager.checkPermissions()) {
            _errorMessage.value = "Bluetooth permissions required before scanning."
            return
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _errorMessage.value = "Bluetooth is disabled. Please enable Bluetooth."
            return
        }

        // Register receiver for classic discovery
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(classicDiscoveryReceiver, filter)
            isReceiverRegistered = true
        }

        // Cancel previous discovery if running
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        // Clear previous unbonded discoveries
        _discoveredDevices.value = emptyList()
        refreshPairedDevices()

        // 1. Classic discovery
        val started = bluetoothAdapter.startDiscovery()
        if (!started) {
            Log.w(TAG, "Failed to initiate Classic discovery")
        }

        // 2. BLE Scanner with Pardhan UUID filter
        try {
            bleScanner = bluetoothAdapter.bluetoothLeScanner
            val scanFilter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(PardhanProtocol.SERVICE_UUID))
                .build()
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            bleScanner?.startScan(listOf(scanFilter), scanSettings, bleScanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for BLE scan", e)
        } catch (e: Exception) {
            Log.w(TAG, "BLE scan not supported or failed to start", e)
        }

        _isDiscovering.value = true
    }

    /**
     * Stops both Classic and BLE discovery.
     */
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            bleScanner?.stopScan(bleScanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping discovery", e)
        } finally {
            _isDiscovering.value = false
        }
    }

    /**
     * Starts BLE advertising using the shared application service UUID.
     * Crucial: Advertises purely the service UUID; no personal user data is exposed.
     */
    @SuppressLint("MissingPermission")
    fun startAdvertising() {
        if (!permissionManager.checkPermissions() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return
        }

        bleAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (bleAdvertiser == null) {
            Log.w(TAG, "BLE Advertising not supported on this device")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false) // Never leak device or user name in raw beacon
            .addServiceUuid(ParcelUuid(PardhanProtocol.SERVICE_UUID))
            .build()

        try {
            bleAdvertiser?.startAdvertising(settings, data, bleAdvertiseCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission missing for startAdvertising", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting BLE advertising", e)
        }
    }

    /**
     * Stops BLE advertising.
     */
    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        try {
            bleAdvertiser?.stopAdvertising(bleAdvertiseCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping advertising", e)
        } finally {
            _isAdvertising.value = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun addOrUpdateDiscoveredDevice(device: BluetoothDevice, rssi: Int, isPardhan: Boolean) {
        val name = device.name ?: "Unknown Device"
        val isPardhanIdentified = isPardhan || name.contains("Pardhan", ignoreCase = true)

        _discoveredDevices.update { list ->
            val index = list.indexOfFirst { it.address == device.address }
            val item = BluetoothDeviceDomain(
                name = name,
                address = device.address,
                isPardhanDevice = isPardhanIdentified,
                isPaired = device.bondState == BluetoothDevice.BOND_BONDED,
                isConnected = false,
                rssi = if (rssi != Short.MIN_VALUE.toInt()) rssi else null,
                bondState = device.bondState
            )

            if (index >= 0) {
                // Update existing item while preserving Pardhan identification
                val existing = list[index]
                list.toMutableList().apply {
                    this[index] = item.copy(
                        isPardhanDevice = existing.isPardhanDevice || item.isPardhanDevice
                    )
                }
            } else {
                list + item
            }
        }
    }

    fun cleanUp() {
        stopDiscovery()
        stopAdvertising()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(classicDiscoveryReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering receiver", e)
            }
            isReceiverRegistered = false
        }
    }
}
