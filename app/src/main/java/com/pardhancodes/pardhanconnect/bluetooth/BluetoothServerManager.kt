package com.pardhancodes.pardhanconnect.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.pardhancodes.pardhanconnect.data.model.PardhanProtocol
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages the RFCOMM Server Socket listening for incoming peer connections.
 */
class BluetoothServerManager(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val permissionManager: BluetoothPermissionManager
) {
    companion object {
        private const val TAG = "PardhanServer"
    }

    private var serverSocket: BluetoothServerSocket? = null
    private var listenJob: Job? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _serverError = MutableStateFlow<String?>(null)
    val serverError: StateFlow<String?> = _serverError.asStateFlow()

    /**
     * Starts listening for incoming RFCOMM connections on a background IO thread.
     * When a client connects, [onSocketAccepted] is invoked with the connected BluetoothSocket.
     */
    @SuppressLint("MissingPermission")
    fun startListening(
        scope: CoroutineScope,
        onSocketAccepted: (BluetoothSocket) -> Unit
    ) {
        if (!permissionManager.checkPermissions()) {
            _serverError.value = "Permissions missing for Bluetooth server."
            return
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _serverError.value = "Bluetooth is disabled."
            return
        }

        // Avoid starting duplicate listeners
        if (_isListening.value) {
            Log.d(TAG, "Server listener already running")
            return
        }

        listenJob?.cancel()
        listenJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    PardhanProtocol.DEFAULT_SERVICE_NAME,
                    PardhanProtocol.SERVICE_UUID
                )
                _isListening.value = true
                Log.d(TAG, "RFCOMM Server listening on UUID: ${PardhanProtocol.SERVICE_UUID}")

                while (isActive) {
                    val socket: BluetoothSocket = try {
                        // Blocking call; waits for an incoming connection
                        serverSocket?.accept() ?: break
                    } catch (e: IOException) {
                        if (isActive) {
                            Log.w(TAG, "Server accept failed: ${e.message}")
                            _serverError.value = "Server accept error: ${e.localizedMessage}"
                        }
                        break
                    }

                    Log.d(TAG, "Accepted incoming RFCOMM connection from ${socket.remoteDevice.address}")
                    withContext(Dispatchers.Main) {
                        onSocketAccepted(socket)
                    }

                    // For Phase 1 single-peer RFCOMM connection, pause server socket after accept
                    break
                }
            } catch (e: SecurityException) {
                _serverError.value = "SecurityException starting server: ${e.message}"
                Log.e(TAG, "SecurityException in server", e)
            } catch (e: IOException) {
                _serverError.value = "IOException starting server: ${e.message}"
                Log.e(TAG, "IOException in server", e)
            } catch (e: CancellationException) {
                Log.d(TAG, "Server listener coroutine cancelled")
            } finally {
                _isListening.value = false
            }
        }
    }

    /**
     * Safely closes the server socket and cancels the coroutine.
     */
    fun stopListening() {
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.w(TAG, "Error closing server socket: ${e.message}")
        } finally {
            serverSocket = null
            listenJob?.cancel()
            listenJob = null
            _isListening.value = false
        }
    }
}
