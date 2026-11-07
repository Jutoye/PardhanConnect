package com.pardhancodes.pardhanconnect.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pardhancodes.pardhanconnect.bluetooth.BluetoothManager
import com.pardhancodes.pardhanconnect.data.model.BluetoothDeviceDomain
import com.pardhancodes.pardhanconnect.data.model.ConnectionState
import com.pardhancodes.pardhanconnect.data.model.PingPongResult
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Main ViewModel exposing Bluetooth StateFlows to Jetpack Compose UI.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val bluetoothManager = BluetoothManager(application.applicationContext)

    val connectionState: StateFlow<ConnectionState> = bluetoothManager.connectionState
    val discoveredDevices: StateFlow<List<BluetoothDeviceDomain>> = bluetoothManager.discoveredDevices
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>> = bluetoothManager.pairedDevices
    val isDiscovering: StateFlow<Boolean> = bluetoothManager.isDiscovering
    val isAdvertising: StateFlow<Boolean> = bluetoothManager.isAdvertising
    val isBluetoothEnabled: StateFlow<Boolean> = bluetoothManager.isBluetoothEnabled
    val hasAllPermissions: StateFlow<Boolean> = bluetoothManager.hasAllPermissions
    val lastPingPongResult: StateFlow<PingPongResult?> = bluetoothManager.lastPingPongResult
    val rawPacketLogs: StateFlow<List<String>> = bluetoothManager.rawPacketLog
    val localNodeId: String = bluetoothManager.localNodeId

    init {
        // Automatically start background services when permissions are granted
        refreshStateAndStart()
    }

    fun refreshStateAndStart() {
        bluetoothManager.permissionManager.refreshState()
        if (bluetoothManager.hasAllPermissions.value && bluetoothManager.isBluetoothEnabled.value) {
            bluetoothManager.startServices(viewModelScope)
        }
    }

    fun startDiscovery() {
        bluetoothManager.discoveryManager.startDiscovery()
    }

    fun stopDiscovery() {
        bluetoothManager.discoveryManager.stopDiscovery()
    }

    fun connectToDevice(device: BluetoothDeviceDomain) {
        viewModelScope.launch {
            bluetoothManager.connectToDevice(device, viewModelScope)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothManager.disconnect(viewModelScope)
        }
    }

    fun sendPing() {
        viewModelScope.launch {
            bluetoothManager.pingPeer(viewModelScope)
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.tearDown()
    }
}
