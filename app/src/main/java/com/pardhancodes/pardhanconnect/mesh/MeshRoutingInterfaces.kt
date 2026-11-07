package com.pardhancodes.pardhanconnect.mesh

import java.util.UUID

/**
 * Pardhan Connect Phase 1 Mesh Architectural Interfaces.
 * Prepares the abstraction layer for multi-hop mesh routing in Phase 2
 * without simulating or faking active forwarding in Phase 1.
 */

/**
 * Uniquely identifies a peer in the Pardhan mesh network.
 */
interface PeerIdentity {
    val nodeId: String
    val bluetoothAddress: String
    val alias: String?
    val firstSeenTimestamp: Long
}

/**
 * Encapsulates an active peer link.
 */
interface PeerConnection {
    val peer: PeerIdentity
    val isConnected: Boolean
    val linkRssi: Int?
    val roundTripLatencyMs: Long
    fun sendDirect(bytes: ByteArray): Boolean
    fun closeConnection()
}

/**
 * Header and envelope requirements for mesh routed packets.
 */
interface MeshEnvelope {
    val messageId: UUID
    val originNodeId: String
    val destinationNodeId: String // Broadcast or specific unicast peer
    val hopCount: Int
    val maxHops: Int
    val payloadType: Int
    val createdTimestamp: Long
    val payloadBytes: ByteArray
}

/**
 * Decides whether this local node is eligible to relay/forward a packet.
 */
interface RelayEligibilityDecider {
    /**
     * Determines whether the local device should relay [envelope]
     * based on battery level, hop count, and routing policy.
     */
    fun isEligibleToRelay(envelope: MeshEnvelope, batteryPercent: Int, isCharging: Boolean): Boolean
}

/**
 * Prevents broadcast storms and infinite routing loops by tracking observed message identifiers.
 */
interface DuplicatePacketCache {
    /**
     * Returns true if [messageId] was already processed or seen within the deduplication window.
     */
    fun isDuplicate(messageId: UUID): Boolean

    /**
     * Records [messageId] into the cache.
     */
    fun recordMessage(messageId: UUID)

    /**
     * Evicts expired identifiers.
     */
    fun evictExpired(maxAgeMillis: Long)
}

/**
 * Enforces hop limits to prevent packets circulating indefinitely.
 */
interface HopCountEnforcer {
    val defaultMaxHops: Int get() = 7

    /**
     * Returns a new envelope with incremented hop count, or null if [defaultMaxHops] exceeded.
     */
    fun incrementHop(envelope: MeshEnvelope): MeshEnvelope?
}

/**
 * Core routing table and forwarding engine contract for Phase 2 implementation.
 */
interface MessageRoutingEngine {
    fun registerPeer(connection: PeerConnection)
    fun unregisterPeer(nodeId: String)
    fun routeOutbound(destinationNodeId: String, payload: ByteArray): Boolean
    fun handleInbound(incomingEnvelope: MeshEnvelope, ingressPeer: PeerIdentity)
}

/**
 * Concrete Phase 1 LRU Deduplication Cache implementation.
 */
class InMemoryDuplicateCache(private val capacity: Int = 1000) : DuplicatePacketCache {
    private val seenMessageIds = LinkedHashSet<UUID>(capacity)

    @Synchronized
    override fun isDuplicate(messageId: UUID): Boolean {
        return seenMessageIds.contains(messageId)
    }

    @Synchronized
    override fun recordMessage(messageId: UUID) {
        if (seenMessageIds.size >= capacity) {
            val oldest = seenMessageIds.iterator().next()
            seenMessageIds.remove(oldest)
        }
        seenMessageIds.add(messageId)
    }

    @Synchronized
    override fun evictExpired(maxAgeMillis: Long) {
        // In-memory LRU handles eviction by capacity
    }
}
