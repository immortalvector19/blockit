package com.blockit.service.vpn

import java.nio.ByteBuffer

/**
 * Pure Kotlin parser for DNS (RFC 1035) wire-format packets.
 */
object DnsPacketParser {

    /**
     * Extracts the DNS Question Name (e.g. "www.instagram.com") from a raw DNS payload.
     * DNS Header format:
     * - ID: 2 bytes
     * - Flags: 2 bytes
     * - QDCOUNT: 2 bytes (Question count)
     * - ANCOUNT: 2 bytes (Answer count)
     * - NSCOUNT: 2 bytes (Authority count)
     * - ARCOUNT: 2 bytes (Additional count)
     * Total header = 12 bytes.
     */
    fun extractQuestionName(dnsPayload: ByteArray, offset: Int = 0, length: Int = dnsPayload.size): String? {
        if (length < 12) return null

        val buffer = ByteBuffer.wrap(dnsPayload, offset, length)
        buffer.position(offset + 12) // Skip 12-byte DNS header

        val sb = StringBuilder()
        while (buffer.hasRemaining()) {
            val labelLen = buffer.get().toInt() and 0xFF
            if (labelLen == 0) {
                // End of QNAME
                break
            }
            if ((labelLen and 0xC0) == 0xC0) {
                // DNS compression pointer (not typically in Question section, but handle safely)
                break
            }
            if (buffer.remaining() < labelLen) {
                return null
            }
            if (sb.isNotEmpty()) {
                sb.append('.')
            }
            val labelBytes = ByteArray(labelLen)
            buffer.get(labelBytes)
            sb.append(String(labelBytes, Charsets.US_ASCII))
        }

        return if (sb.isNotEmpty()) sb.toString() else null
    }

    /**
     * Extracts the 16-bit Transaction ID from the DNS header.
     */
    fun extractTransactionId(dnsPayload: ByteArray, offset: Int = 0): Short {
        val buffer = ByteBuffer.wrap(dnsPayload, offset, 2)
        return buffer.short
    }
}
