package com.pardhancodes.pardhanconnect.data.model

import android.bluetooth.BluetoothDevice

/**
 * Domain representation of a discovered or paired Bluetooth device.
 */
data class BluetoothDeviceDomain(
    val name: String,
    val address: String,
    val isPardhanDevice: Boolean = false,
    val isPaired: Boolean = false,
    val isConnected: Boolean = false,
    val rssi: Int? = null,
    val bondState: Int = BluetoothDevice.BOND_NONE
)

/**
 * Represents the lifecycle connection state of the RFCOMM engine.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    data class Connecting(val device: BluetoothDeviceDomain) : ConnectionState()
    data class Handshaking(val device: BluetoothDeviceDomain, val step: String) : ConnectionState()
    data class Connected(
        val device: BluetoothDeviceDomain,
        val handshake: HandshakeInfo
    ) : ConnectionState()
    data class Error(val message: String, val canRetry: Boolean = true) : ConnectionState()
}

/**
 * Validated handshake metadata exchanged upon RFCOMM socket establishment.
 */
data class HandshakeInfo(
    val protocolVersion: String,
    val appName: String,
    val peerSessionId: String,
    val peerNodeId: String,
    val localSessionId: String,
    val establishedAt: Long = System.currentTimeMillis()
)

/**
 * Measurement result of a bidirectional PING/PONG packet test.
 */
data class PingPongResult(
    val sequenceNumber: Long,
    val latencyMs: Long,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
