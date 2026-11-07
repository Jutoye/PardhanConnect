package com.pardhancodes.pardhanconnect.bluetooth

import android.util.Log
import com.pardhancodes.pardhanconnect.data.model.HandshakeInfo
import com.pardhancodes.pardhanconnect.data.model.PardhanProtocol
import com.pardhancodes.pardhanconnect.data.model.PingPongResult
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Validates protocol negotiation, security handshake, and PING/PONG latency probes.
 */
class HandshakeManager(
    val localNodeId: String = "node_" + UUID.randomUUID().toString().take(6)
) {
    companion object {
        private const val TAG = "PardhanHandshake"
    }

    private var currentSessionId: String = UUID.randomUUID().toString()

    /**
     * Executes the Initiator (Client) side of the handshake:
     * 1. Sends HANDSHAKE_INIT with protocol version, app name, session ID, local node ID.
     * 2. Reads HANDSHAKE_ACK from peer.
     * 3. Validates handshake metadata and ensures protocol match.
     */
    suspend fun executeClientHandshake(
        inputStream: InputStream,
        outputStream: OutputStream
    ): Result<HandshakeInfo> = withContext(Dispatchers.IO) {
        try {
            currentSessionId = UUID.randomUUID().toString()
            Log.d(TAG, "Initiating client handshake with session: $currentSessionId")

            // Send INIT
            val initPayload = PardhanProtocol.createHandshakeInitPayload(currentSessionId, localNodeId)
            val initPacket = PardhanProtocol.Packet(PardhanProtocol.PacketType.HANDSHAKE_INIT, initPayload)
            val encodedInit = PardhanProtocol.encodePacket(initPacket)
            outputStream.write(encodedInit)
            outputStream.flush()

            // Wait for ACK
            val ackPacket = PardhanProtocol.readNextPacket(inputStream)
            if (ackPacket.type != PardhanProtocol.PacketType.HANDSHAKE_ACK) {
                return@withContext Result.failure(
                    IllegalStateException("Expected HANDSHAKE_ACK, received: ${ackPacket.type}")
                )
            }

            val json = JSONObject(ackPacket.payloadAsString())
            val protocolVersion = json.optString("protocol_version")
            val appName = json.optString("app_name")
            val peerSessionId = json.optString("session_id")
            val peerNodeId = json.optString("node_id")
            val accepted = json.optBoolean("accepted", false)

            if (!accepted) {
                return@withContext Result.failure(
                    IllegalStateException("Peer rejected handshake")
                )
            }

            if (protocolVersion != PardhanProtocol.PROTOCOL_VERSION) {
                return@withContext Result.failure(
                    IllegalStateException("Protocol version mismatch: Expected ${PardhanProtocol.PROTOCOL_VERSION}, got $protocolVersion")
                )
            }

            if (appName != PardhanProtocol.APPLICATION_NAME) {
                return@withContext Result.failure(
                    IllegalStateException("Application name mismatch: $appName")
                )
            }

            val handshakeInfo = HandshakeInfo(
                protocolVersion = protocolVersion,
                appName = appName,
                peerSessionId = peerSessionId,
                peerNodeId = peerNodeId,
                localSessionId = currentSessionId
            )
            Log.d(TAG, "Client handshake successful with peer node: $peerNodeId")
            Result.success(handshakeInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Client handshake failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Executes the Responder (Server) side of the handshake:
     * 1. Awaits HANDSHAKE_INIT from client.
     * 2. Validates incoming metadata.
     * 3. Replies with HANDSHAKE_ACK.
     */
    suspend fun executeServerHandshake(
        inputStream: InputStream,
        outputStream: OutputStream
    ): Result<HandshakeInfo> = withContext(Dispatchers.IO) {
        try {
            currentSessionId = UUID.randomUUID().toString()
            Log.d(TAG, "Server waiting for client HANDSHAKE_INIT...")

            // Read INIT
            val initPacket = PardhanProtocol.readNextPacket(inputStream)
            if (initPacket.type != PardhanProtocol.PacketType.HANDSHAKE_INIT) {
                return@withContext Result.failure(
                    IllegalStateException("Expected HANDSHAKE_INIT, received: ${initPacket.type}")
                )
            }

            val json = JSONObject(initPacket.payloadAsString())
            val clientVersion = json.optString("protocol_version")
            val clientApp = json.optString("app_name")
            val clientSessionId = json.optString("session_id")
            val clientNodeId = json.optString("node_id")

            val isValid = (clientVersion == PardhanProtocol.PROTOCOL_VERSION &&
                    clientApp == PardhanProtocol.APPLICATION_NAME)

            // Send ACK
            val ackPayload = PardhanProtocol.createHandshakeAckPayload(currentSessionId, localNodeId, isValid)
            val ackPacket = PardhanProtocol.Packet(PardhanProtocol.PacketType.HANDSHAKE_ACK, ackPayload)
            val encodedAck = PardhanProtocol.encodePacket(ackPacket)
            outputStream.write(encodedAck)
            outputStream.flush()

            if (!isValid) {
                return@withContext Result.failure(
                    IllegalStateException("Incompatible protocol version ($clientVersion) or application ($clientApp)")
                )
            }

            val handshakeInfo = HandshakeInfo(
                protocolVersion = clientVersion,
                appName = clientApp,
                peerSessionId = clientSessionId,
                peerNodeId = clientNodeId,
                localSessionId = currentSessionId
            )
            Log.d(TAG, "Server handshake validated with client node: $clientNodeId")
            Result.success(handshakeInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Server handshake failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sends a PING packet and awaits the echoed PONG to verify real bidirectional data flow.
     */
    suspend fun sendPing(
        seq: Long,
        inputStream: InputStream,
        outputStream: OutputStream
    ): PingPongResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val pingPayload = PardhanProtocol.createPingPayload(seq)
            val pingPacket = PardhanProtocol.Packet(PardhanProtocol.PacketType.PING, pingPayload)
            val encodedPing = PardhanProtocol.encodePacket(pingPacket)

            outputStream.write(encodedPing)
            outputStream.flush()

            // Wait for matching PONG
            val responsePacket = PardhanProtocol.readNextPacket(inputStream)
            val latencyMs = System.currentTimeMillis() - startTime

            if (responsePacket.type == PardhanProtocol.PacketType.PONG) {
                val json = JSONObject(responsePacket.payloadAsString())
                val returnedSeq = json.optLong("seq")
                if (returnedSeq == seq) {
                    PingPongResult(
                        sequenceNumber = seq,
                        latencyMs = latencyMs,
                        success = true
                    )
                } else {
                    PingPongResult(
                        sequenceNumber = seq,
                        latencyMs = latencyMs,
                        success = false,
                        errorMessage = "Sequence mismatch (sent: $seq, received: $returnedSeq)"
                    )
                }
            } else {
                PingPongResult(
                    sequenceNumber = seq,
                    latencyMs = latencyMs,
                    success = false,
                    errorMessage = "Unexpected response type: ${responsePacket.type}"
                )
            }
        } catch (e: Exception) {
            val latencyMs = System.currentTimeMillis() - startTime
            PingPongResult(
                sequenceNumber = seq,
                latencyMs = latencyMs,
                success = false,
                errorMessage = e.localizedMessage ?: "PING IO error"
            )
        }
    }

    /**
     * Responds to an incoming PING by emitting the corresponding PONG packet.
     */
    suspend fun respondToPing(
        pingPacket: PardhanProtocol.Packet,
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        val json = JSONObject(pingPacket.payloadAsString())
        val seq = json.optLong("seq")
        val sentAt = json.optLong("sent_at")

        val pongPayload = PardhanProtocol.createPongPayload(seq, sentAt)
        val pongPacket = PardhanProtocol.Packet(PardhanProtocol.PacketType.PONG, pongPayload)
        val encodedPong = PardhanProtocol.encodePacket(pongPacket)

        outputStream.write(encodedPong)
        outputStream.flush()
    }
}
