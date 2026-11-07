package com.pardhancodes.pardhanconnect.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.pardhancodes.pardhanconnect.data.model.BluetoothDeviceDomain
import com.pardhancodes.pardhanconnect.data.model.PardhanProtocol
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages outgoing RFCOMM client connections to remote Pardhan Connect devices.
 */
class BluetoothClientManager(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val permissionManager: BluetoothPermissionManager
) {
    companion object {
        private const val TAG = "PardhanClient"
    }

    private var connectJob: Job? = null
    private var activeSocket: BluetoothSocket? = null

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _clientError = MutableStateFlow<String?>(null)
    val clientError: StateFlow<String?> = _clientError.asStateFlow()

    /**
     * Connects to a target remote device over RFCOMM.
     * Note: Cancels discovery first, as active discovery heavily degrades RFCOMM throughput and can cause timeouts.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(
        target: BluetoothDeviceDomain,
        scope: CoroutineScope,
        onConnected: (BluetoothSocket) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!permissionManager.checkPermissions()) {
            val err = "Bluetooth permissions missing."
            _clientError.value = err
            onError(err)
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val err = "Bluetooth is disabled."
            _clientError.value = err
            onError(err)
            return
        }

        connectJob?.cancel()
        _isConnecting.value = true
        _clientError.value = null

        connectJob = scope.launch(Dispatchers.IO) {
            var socket: BluetoothSocket? = null
            try {
                // Cancel discovery before connection attempt
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                }

                val remoteDevice: BluetoothDevice =
                    bluetoothAdapter.getRemoteDevice(target.address)

                Log.d(TAG, "Creating RFCOMM socket to ${target.name} (${target.address})")
                socket = remoteDevice.createRfcommSocketToServiceRecord(PardhanProtocol.SERVICE_UUID)
                activeSocket = socket

                // Blocking call: connect to remote RFCOMM server
                socket.connect()
                Log.d(TAG, "RFCOMM socket connected successfully to ${target.address}")

                _isConnecting.value = false
                withContext(Dispatchers.Main) {
                    onConnected(socket)
                }
            } catch (e: SecurityException) {
                val msg = "SecurityException connecting: ${e.message}"
                Log.e(TAG, msg, e)
                closeSocketQuietly(socket)
                _isConnecting.value = false
                _clientError.value = msg
                withContext(Dispatchers.Main) { onError(msg) }
            } catch (e: IOException) {
                val msg = "Could not establish RFCOMM connection: ${e.localizedMessage ?: "Connection failed"}"
                Log.e(TAG, msg, e)
                closeSocketQuietly(socket)
                _isConnecting.value = false
                _clientError.value = msg
                withContext(Dispatchers.Main) { onError(msg) }
            } catch (e: CancellationException) {
                Log.d(TAG, "Client connection coroutine cancelled")
                closeSocketQuietly(socket)
                _isConnecting.value = false
            }
        }
    }

    private fun closeSocketQuietly(socket: BluetoothSocket?) {
        try {
            socket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing socket: ${e.message}")
        }
        if (activeSocket == socket) {
            activeSocket = null
        }
    }

    fun cancelConnection() {
        connectJob?.cancel()
        connectJob = null
        closeSocketQuietly(activeSocket)
        _isConnecting.value = false
    }
}
