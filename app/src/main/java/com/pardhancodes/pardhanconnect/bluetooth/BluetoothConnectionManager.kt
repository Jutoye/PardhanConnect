package com.pardhancodes.pardhanconnect.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.pardhancodes.pardhanconnect.data.model.BluetoothDeviceDomain
import com.pardhancodes.pardhanconnect.data.model.ConnectionState
import com.pardhancodes.pardhanconnect.data.model.PardhanProtocol
import com.pardhancodes.pardhanconnect.data.model.PingPongResult
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
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
 * Manages active RFCOMM socket lifecycle, background IO loops,
 * disconnect recovery, packet routing, and state emission.
 */
class BluetoothConnectionManager(
    private val handshakeManager: HandshakeManager
) {
    companion object {
        private const val TAG = "PardhanConnManager"
    }

    private var activeSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var ioJob: Job? = null
    private val pingSequence = AtomicLong(1)

    // Pings waiting for their PONG. Only the single read loop reads the socket;
    // it completes these when a matching PONG arrives.
    private val pendingPings = ConcurrentHashMap<Long, CompletableDeferred<Unit>>()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _lastPingPongResult = MutableStateFlow<PingPongResult?>(null)
    val lastPingPongResult: StateFlow<PingPongResult?> = _lastPingPongResult.asStateFlow()

    private val _rawPacketLog = MutableStateFlow<List<String>>(emptyList())
    val rawPacketLog: StateFlow<List<String>> = _rawPacketLog.asStateFlow()

    /**
     * Adopts an established BluetoothSocket (from either client or server role),
     * runs the handshake validation, and starts the IO message loop.
     */
    @SuppressLint("MissingPermission")
    fun attachSocket(
        socket: BluetoothSocket,
        isServerSide: Boolean,
        scope: CoroutineScope
    ) {
        // Prevent duplicate connection race condition
        if (activeSocket?.isConnected == true) {
            Log.w(TAG, "Already connected to a device. Closing existing socket first.")
            disconnect()
        }

        activeSocket = socket
        val deviceDomain = BluetoothDeviceDomain(
            name = socket.remoteDevice.name ?: "Unknown Peer",
            address = socket.remoteDevice.address,
            isConnected = true
        )

        _connectionState.value = ConnectionState.Connecting(deviceDomain)
        logPacket("Socket attached: ${deviceDomain.name} (${deviceDomain.address}) [Role: ${if (isServerSide) "Server" else "Client"}]")

        ioJob?.cancel()
        ioJob = scope.launch(Dispatchers.IO) {
            try {
                inputStream = socket.inputStream
                outputStream = socket.outputStream

                val inStream = inputStream ?: throw IOException("InputStream is null")
                val outStream = outputStream ?: throw IOException("OutputStream is null")

                // Step 2: Protocol Handshake
                _connectionState.value = ConnectionState.Handshaking(deviceDomain, "Validating protocol & session...")
                logPacket("Starting protocol handshake with peer...")

                val handshakeResult = if (isServerSide) {
                    handshakeManager.executeServerHandshake(inStream, outStream)
                } else {
                    handshakeManager.executeClientHandshake(inStream, outStream)
                }

                if (handshakeResult.isFailure) {
                    val err = handshakeResult.exceptionOrNull()?.message ?: "Handshake failed"
                    logPacket("Handshake rejected: $err")
                    _connectionState.value = ConnectionState.Error("Handshake rejected: $err")
                    disconnect()
                    return@launch
                }

                val handshakeInfo = handshakeResult.getOrThrow()
                logPacket("Handshake validated! Node: ${handshakeInfo.peerNodeId}, Session: ${handshakeInfo.peerSessionId.take(8)}...")

                _connectionState.value = ConnectionState.Connected(deviceDomain, handshakeInfo)

                // Step 3: Sustained IO Read Loop
                while (isActive && socket.isConnected) {
                    try {
                        val packet = PardhanProtocol.readNextPacket(inStream)
                        handleIncomingPacket(packet, outStream)
                    } catch (e: IOException) {
                        if (isActive) {
                            Log.w(TAG, "Socket read EOF/IOException: ${e.message}")
                            logPacket("Disconnected: Peer closed connection.")
                            _connectionState.value = ConnectionState.Disconnected
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e(TAG, "Error in connection IO coroutine", e)
                    logPacket("Connection error: ${e.localizedMessage}")
                    _connectionState.value = ConnectionState.Error(e.localizedMessage ?: "Connection error")
                }
            } finally {
                cleanupResources()
            }
        }
    }

    private suspend fun handleIncomingPacket(packet: PardhanProtocol.Packet, outStream: OutputStream) {
        logPacket("Rx [${packet.type.name}]: ${packet.payload.size} bytes")
        when (packet.type) {
            PardhanProtocol.PacketType.PING -> {
                // Instantly echo PONG
                logPacket("Responding to incoming PING...")
                handshakeManager.respondToPing(packet, outStream)
            }
            PardhanProtocol.PacketType.PONG -> {
                val seq = try {
                    JSONObject(packet.payloadAsString()).optLong("seq", -1L)
                } catch (e: Exception) {
                    -1L
                }
                logPacket("Received PONG packet from peer (seq $seq)")
                pendingPings[seq]?.complete(Unit)
            }
            PardhanProtocol.PacketType.DISCONNECT -> {
                logPacket("Peer requested disconnect")
                disconnect()
            }
            else -> {
                logPacket("Unhandled packet type: ${packet.type}")
            }
        }
    }

    /**
     * Executes a bidirectional PING/PONG packet test over the live socket.
     * The PING is written here; the PONG is read by the single IO read loop
     * (two readers on one stream would steal each other's packets).
     */
    fun performPingTest(scope: CoroutineScope) {
        val currentState = _connectionState.value
        if (currentState !is ConnectionState.Connected) {
            _lastPingPongResult.value = PingPongResult(
                sequenceNumber = 0,
                latencyMs = -1,
                success = false,
                errorMessage = "Cannot ping: No active RFCOMM connection"
            )
            return
        }

        val outStream = outputStream ?: return
        val seq = pingSequence.getAndIncrement()
        val waiter = CompletableDeferred<Unit>()
        pendingPings[seq] = waiter

        logPacket("Tx [PING #$seq]: Probing peer latency...")
        scope.launch(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            val result = try {
                val packet = PardhanProtocol.Packet(
                    PardhanProtocol.PacketType.PING,
                    PardhanProtocol.createPingPayload(seq)
                )
                synchronized(outStream) {
                    outStream.write(PardhanProtocol.encodePacket(packet))
                    outStream.flush()
                }
                val gotPong = withTimeoutOrNull(5000L) {
                    waiter.await()
                    true
                } ?: false
                val latency = System.currentTimeMillis() - start
                if (gotPong) {
                    PingPongResult(sequenceNumber = seq, latencyMs = latency, success = true)
                } else {
                    PingPongResult(
                        sequenceNumber = seq,
                        latencyMs = latency,
                        success = false,
                        errorMessage = "Timeout: no PONG within 5 seconds"
                    )
                }
            } catch (e: Exception) {
                PingPongResult(
                    sequenceNumber = seq,
                    latencyMs = System.currentTimeMillis() - start,
                    success = false,
                    errorMessage = e.localizedMessage ?: "PING IO error"
                )
            } finally {
                pendingPings.remove(seq)
            }
            _lastPingPongResult.value = result
            if (result.success) {
                logPacket("Rx [PONG #$seq]: Success! Round-trip latency: ${result.latencyMs}ms")
            } else {
                logPacket("Rx [PONG #$seq]: FAILED - ${result.errorMessage}")
            }
        }
    }

    /**
     * Safely closes socket and streams, transitioning state to Disconnected.
     */
    fun disconnect() {
        logPacket("Disconnecting RFCOMM socket...")
        cleanupResources()
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun cleanupResources() {
        ioJob?.cancel()
        ioJob = null

        try {
            outputStream?.flush()
            outputStream?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing output stream: ${e.message}")
        } finally {
            outputStream = null
        }

        try {
            inputStream?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing input stream: ${e.message}")
        } finally {
            inputStream = null
        }

        try {
            activeSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing BluetoothSocket: ${e.message}")
        } finally {
            activeSocket = null
        }
    }

    private fun logPacket(msg: String) {
        val entry = "[${System.currentTimeMillis() % 100000}] $msg"
        _rawPacketLog.value = (_rawPacketLog.value.takeLast(40) + entry)
    }
}
