package com.pardhancodes.pardhanconnect.bluetooth

import android.content.Context
import com.pardhancodes.pardhanconnect.data.model.BluetoothDeviceDomain
import com.pardhancodes.pardhanconnect.data.model.ConnectionState
import com.pardhancodes.pardhanconnect.data.model.PingPongResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Facade coordinating all Bluetooth sub-systems in Pardhan Connect.
 */
class BluetoothManager(
    private val context: Context,
    val localNodeId: String = "Pardhan_" + (1000..9999).random()
) {
    val permissionManager = BluetoothPermissionManager(context)
    val handshakeManager = HandshakeManager(localNodeId)

    val discoveryManager = BluetoothDiscoveryManager(
        context = context,
        bluetoothAdapter = permissionManager.bluetoothAdapter,
        permissionManager = permissionManager
    )

    val serverManager = BluetoothServerManager(
        bluetoothAdapter = permissionManager.bluetoothAdapter,
        permissionManager = permissionManager
    )

    val clientManager = BluetoothClientManager(
        bluetoothAdapter = permissionManager.bluetoothAdapter,
        permissionManager = permissionManager
    )

    val connectionManager = BluetoothConnectionManager(handshakeManager)

    // Observable StateFlows
    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState
    val discoveredDevices: StateFlow<List<BluetoothDeviceDomain>> = discoveryManager.discoveredDevices
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>> = discoveryManager.pairedDevices
    val isDiscovering: StateFlow<Boolean> = discoveryManager.isDiscovering
    val isAdvertising: StateFlow<Boolean> = discoveryManager.isAdvertising
    val isServerListening: StateFlow<Boolean> = serverManager.isListening
    val isBluetoothEnabled: StateFlow<Boolean> = permissionManager.isBluetoothEnabled
    val hasAllPermissions: StateFlow<Boolean> = permissionManager.hasAllPermissions
    val lastPingPongResult: StateFlow<PingPongResult?> = connectionManager.lastPingPongResult
    val rawPacketLog: StateFlow<List<String>> = connectionManager.rawPacketLog

    /**
     * Initializes background services (BLE advertising + RFCOMM server socket listener).
     */
    fun startServices(scope: CoroutineScope) {
        permissionManager.refreshState()
        if (permissionManager.hasAllPermissions.value && permissionManager.isBluetoothEnabled.value) {
            // 1. Start background RFCOMM server so device can accept incoming connections
            serverManager.startListening(scope) { socket ->
                connectionManager.attachSocket(socket, isServerSide = true, scope = scope)
            }
            // 2. Start BLE advertising to assist nearby peers in finding this device
            discoveryManager.startAdvertising()
            // 3. Load paired devices
            discoveryManager.refreshPairedDevices()
        }
    }

    /**
     * Connects to a target peer as an RFCOMM client.
     */
    fun connectToDevice(target: BluetoothDeviceDomain, scope: CoroutineScope) {
        discoveryManager.stopDiscovery()
        clientManager.connectToDevice(
            target = target,
            scope = scope,
            onConnected = { socket ->
                connectionManager.attachSocket(socket, isServerSide = false, scope = scope)
            },
            onError = { errMsg ->
                // Error state is already emitted by clientManager
            }
        )
    }

    /**
     * Disconnects current connection and resumes server listening.
     */
    fun disconnect(scope: CoroutineScope) {
        connectionManager.disconnect()
        // Resume server listener so we are ready for new connections
        startServices(scope)
    }

    /**
     * Sends a test PING packet over the active connection.
     */
    fun pingPeer(scope: CoroutineScope) {
        connectionManager.performPingTest(scope)
    }

    /**
     * Cleans up all resources upon application termination.
     */
    fun tearDown() {
        discoveryManager.cleanUp()
        serverManager.stopListening()
        clientManager.cancelConnection()
        connectionManager.disconnect()
    }
}
