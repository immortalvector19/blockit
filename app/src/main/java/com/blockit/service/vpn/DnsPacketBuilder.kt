package com.blockit.service.vpn

import java.nio.ByteBuffer

object DnsPacketBuilder {

    /**
     * Synthesizes an NXDOMAIN (RCODE 3 - Non-Existent Domain) response to block the DNS query.
     */
    fun buildNxDomainResponse(requestDnsPayload: ByteArray, requestLength: Int): ByteArray {
        if (requestLength < 12) return ByteArray(0)

        // Find end of Question section: starts at 12, ends at first 00 byte + 4 bytes (QTYPE + QCLASS)
        var qEnd = 12
        while (qEnd < requestLength && requestDnsPayload[qEnd] != 0.toByte()) {
            val len = requestDnsPayload[qEnd].toInt() and 0xFF
            qEnd += 1 + len
        }
        qEnd += 1 // Include zero terminator
        if (qEnd + 4 <= requestLength) {
            qEnd += 4 // Include 2-byte QTYPE and 2-byte QCLASS
        } else {
            qEnd = requestLength
        }

        val response = ByteArray(qEnd)
        System.arraycopy(requestDnsPayload, 0, response, 0, qEnd)

        // Set DNS Flags:
        // Byte 2: 0x81 (QR=1 response, RD=1)
        // Byte 3: 0x83 (RA=1, RCODE=3 NXDOMAIN)
        response[2] = 0x81.toByte()
        response[3] = 0x83.toByte()

        // QDCOUNT = 1
        response[4] = 0x00.toByte()
        response[5] = 0x01.toByte()

        // ANCOUNT = 0
        response[6] = 0x00.toByte()
        response[7] = 0x00.toByte()

        // NSCOUNT = 0
        response[8] = 0x00.toByte()
        response[9] = 0x00.toByte()

        // ARCOUNT = 0
        response[10] = 0x00.toByte()
        response[11] = 0x00.toByte()

        return response
    }

    /**
     * Wraps a DNS response payload into a complete IPv4 + UDP packet,
     * reversing the source/destination IPs and ports from the original incoming IPv4/UDP packet.
     */
    fun buildIpv4UdpPacket(
        incomingIpPacket: ByteArray,
        incomingLength: Int,
        dnsResponsePayload: ByteArray
    ): ByteArray {
        if (incomingLength < 28) return ByteArray(0) // Minimum IPv4 (20) + UDP (8)

        val ipHeaderLen = (incomingIpPacket[0].toInt() and 0x0F) * 4
        val totalPacketLen = ipHeaderLen + 8 + dnsResponsePayload.size
        val outPacket = ByteArray(totalPacketLen)

        // 1. Copy and reverse IPv4 Header
        System.arraycopy(incomingIpPacket, 0, outPacket, 0, ipHeaderLen)
        outPacket[0] = 0x45.toByte() // IPv4, Header length = 5 (20 bytes)
        outPacket[1] = 0x00.toByte()

        // Total Length (2 bytes at index 2..3)
        outPacket[2] = ((totalPacketLen shr 8) and 0xFF).toByte()
        outPacket[3] = (totalPacketLen and 0xFF).toByte()

        // TTL
        outPacket[8] = 64.toByte()
        // Protocol = UDP (17)
        outPacket[9] = 17.toByte()

        // Swap Source IP (index 12..15) and Destination IP (index 16..19)
        System.arraycopy(incomingIpPacket, 16, outPacket, 12, 4) // Source IP = Incoming Dest IP
        System.arraycopy(incomingIpPacket, 12, outPacket, 16, 4) // Dest IP = Incoming Source IP

        // Reset and compute IPv4 Header Checksum (index 10..11)
        outPacket[10] = 0
        outPacket[11] = 0
        val ipChecksum = computeIpChecksum(outPacket, 0, ipHeaderLen)
        outPacket[10] = ((ipChecksum shr 8) and 0xFF).toByte()
        outPacket[11] = (ipChecksum and 0xFF).toByte()

        // 2. Build UDP Header (8 bytes at index ipHeaderLen)
        // Swap Source Port (incoming 20..21) and Dest Port (incoming 22..23)
        outPacket[ipHeaderLen] = incomingIpPacket[ipHeaderLen + 2]
        outPacket[ipHeaderLen + 1] = incomingIpPacket[ipHeaderLen + 3]
        outPacket[ipHeaderLen + 2] = incomingIpPacket[ipHeaderLen]
        outPacket[ipHeaderLen + 3] = incomingIpPacket[ipHeaderLen + 1]

        // UDP Length = 8 + payload size
        val udpLen = 8 + dnsResponsePayload.size
        outPacket[ipHeaderLen + 4] = ((udpLen shr 8) and 0xFF).toByte()
        outPacket[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()

        // UDP Checksum (0 = optional in IPv4)
        outPacket[ipHeaderLen + 6] = 0
        outPacket[ipHeaderLen + 7] = 0

        // 3. Copy DNS payload
        System.arraycopy(dnsResponsePayload, 0, outPacket, ipHeaderLen + 8, dnsResponsePayload.size)

        return outPacket
    }

    private fun computeIpChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        while (i < offset + length - 1) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < offset + length) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.inv() and 0xFFFF).toInt()
    }
}
