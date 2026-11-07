package com.pardhancodes.pardhanconnect.data.model

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.json.JSONObject

/**
 * Pardhan Connect Bluetooth RFCOMM Framing & Protocol Specifications.
 */
object PardhanProtocol {
    const val PROTOCOL_VERSION = "PARDHAN-v1.0"
    const val APPLICATION_NAME = "Pardhan Connect"
    const val DEFAULT_SERVICE_NAME = "PardhanConnectRFCOMM"

    // Standard shared UUID for Pardhan Connect Bluetooth RFCOMM and BLE Discovery
    val SERVICE_UUID: UUID = UUID.fromString("7c844111-53b9-4a0b-9ef1-1f9e20a7b5e1")

    // Magic frame bytes: 'P', 'C'
    const val MAGIC_BYTE_1: Byte = 0x50
    const val MAGIC_BYTE_2: Byte = 0x43

    enum class PacketType(val code: Byte) {
        HANDSHAKE_INIT(0x01),
        HANDSHAKE_ACK(0x02),
        PING(0x03),
        PONG(0x04),
        DISCONNECT(0x05);

        companion object {
            fun fromCode(code: Byte): PacketType? = values().firstOrNull { it.code == code }
        }
    }

    data class Packet(
        val type: PacketType,
        val payload: ByteArray
    ) {
        fun payloadAsString(): String = String(payload, StandardCharsets.UTF_8)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Packet
            if (type != other.type) return false
            if (!payload.contentEquals(other.payload)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + payload.contentHashCode()
            return result
        }
    }

    /**
     * Serializes a packet into framed bytes:
     * [0x50, 0x43] [1-byte Type] [4-byte Length] [Payload...]
     */
    fun encodePacket(packet: Packet): ByteArray {
        val byteStream = ByteArrayOutputStream()
        val dataOut = DataOutputStream(byteStream)

        dataOut.writeByte(MAGIC_BYTE_1.toInt())
        dataOut.writeByte(MAGIC_BYTE_2.toInt())
        dataOut.writeByte(packet.type.code.toInt())
        dataOut.writeInt(packet.payload.size)
        dataOut.write(packet.payload)
        dataOut.flush()

        return byteStream.toByteArray()
    }

    /**
     * Reads and decodes the next valid packet from the RFCOMM input stream.
     * Blocks on read until complete packet arrives or stream throws EOF / IOException.
     */
    fun readNextPacket(inputStream: InputStream): Packet {
        val dataIn = DataInputStream(inputStream)

        // Find magic sequence: 0x50, 0x43
        while (true) {
            val b1 = dataIn.readByte()
            if (b1 == MAGIC_BYTE_1) {
                val b2 = dataIn.readByte()
                if (b2 == MAGIC_BYTE_2) {
                    break
                }
            }
        }

        val typeCode = dataIn.readByte()
        val packetType = PacketType.fromCode(typeCode)
            ?: throw IllegalArgumentException("Unknown Pardhan packet type code: $typeCode")

        val payloadLength = dataIn.readInt()
        if (payloadLength < 0 || payloadLength > 1024 * 1024) { // Max 1MB packet limit
            throw IllegalArgumentException("Invalid payload length: $payloadLength")
        }

        val payload = ByteArray(payloadLength)
        dataIn.readFully(payload)

        return Packet(packetType, payload)
    }

    // Helper builders for JSON payloads
    fun createHandshakeInitPayload(sessionId: String, nodeId: String): ByteArray {
        val json = JSONObject().apply {
            put("protocol_version", PROTOCOL_VERSION)
            put("app_name", APPLICATION_NAME)
            put("session_id", sessionId)
            put("node_id", nodeId)
            put("timestamp", System.currentTimeMillis())
        }
        return json.toString().toByteArray(StandardCharsets.UTF_8)
    }

    fun createHandshakeAckPayload(sessionId: String, nodeId: String, accepted: Boolean): ByteArray {
        val json = JSONObject().apply {
            put("protocol_version", PROTOCOL_VERSION)
            put("app_name", APPLICATION_NAME)
            put("session_id", sessionId)
            put("node_id", nodeId)
            put("accepted", accepted)
            put("timestamp", System.currentTimeMillis())
        }
        return json.toString().toByteArray(StandardCharsets.UTF_8)
    }

    fun createPingPayload(seq: Long): ByteArray {
        val json = JSONObject().apply {
            put("seq", seq)
            put("sent_at", System.currentTimeMillis())
        }
        return json.toString().toByteArray(StandardCharsets.UTF_8)
    }

    fun createPongPayload(seq: Long, originalSentAt: Long): ByteArray {
        val json = JSONObject().apply {
            put("seq", seq)
            put("sent_at", originalSentAt)
            put("received_at", System.currentTimeMillis())
        }
        return json.toString().toByteArray(StandardCharsets.UTF_8)
    }
}
