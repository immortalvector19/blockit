package com.blockit.service.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class DnsPacketParserTest {

    private fun createSampleDnsQuery(domain: String, txId: Short = 0x1234.toShort()): ByteArray {
        val parts = domain.split('.')
        var qnameLen = 1 // trailing 0
        for (part in parts) {
            qnameLen += 1 + part.length
        }
        val buffer = ByteBuffer.allocate(12 + qnameLen + 4)
        buffer.putShort(txId) // ID
        buffer.putShort(0x0100.toShort()) // Standard query, RD=1
        buffer.putShort(1.toShort()) // QDCOUNT = 1
        buffer.putShort(0.toShort()) // ANCOUNT = 0
        buffer.putShort(0.toShort()) // NSCOUNT = 0
        buffer.putShort(0.toShort()) // ARCOUNT = 0

        for (part in parts) {
            buffer.put(part.length.toByte())
            buffer.put(part.toByteArray(Charsets.US_ASCII))
        }
        buffer.put(0.toByte()) // null terminator
        buffer.putShort(1.toShort()) // QTYPE = A (1)
        buffer.putShort(1.toShort()) // QCLASS = IN (1)

        return buffer.array()
    }

    @Test
    fun extractQuestionName_extractsDomainSuccessfully() {
        val query = createSampleDnsQuery("instagram.com")
        val qname = DnsPacketParser.extractQuestionName(query)
        assertEquals("instagram.com", qname)

        val subQuery = createSampleDnsQuery("m.youtube.com")
        val subQname = DnsPacketParser.extractQuestionName(subQuery)
        assertEquals("m.youtube.com", subQname)
    }

    @Test
    fun extractTransactionId_extractsCorrectId() {
        val query = createSampleDnsQuery("reddit.com", txId = 0xABCD.toShort())
        val txId = DnsPacketParser.extractTransactionId(query)
        assertEquals(0xABCD.toShort(), txId)
    }

    @Test
    fun buildNxDomainResponse_setsRcode3AndPreservesId() {
        val query = createSampleDnsQuery("tiktok.com", txId = 0x5678.toShort())
        val response = DnsPacketBuilder.buildNxDomainResponse(query, query.size)

        assertNotNull(response)
        assertTrue(response.size >= 12)

        // Verify TX ID preserved
        val respTxId = ByteBuffer.wrap(response, 0, 2).short
        assertEquals(0x5678.toShort(), respTxId)

        // Flags: byte 2 should have QR bit (0x80), byte 3 should have RCODE 3 (0x03)
        val flagsByte2 = response[2].toInt() and 0xFF
        val flagsByte3 = response[3].toInt() and 0xFF

        assertEquals(0x81, flagsByte2) // QR=1, RD=1
        assertEquals(0x83, flagsByte3) // RA=1, RCODE=3 (NXDOMAIN)
    }

    @Test
    fun buildIpv4UdpPacket_swapsIpsAndPorts() {
        val incomingIp = ByteArray(28 + 20) { 0 }
        // IPv4 Header
        incomingIp[0] = 0x45.toByte() // Version 4, IHL 5
        incomingIp[9] = 17.toByte() // UDP

        // Src IP = 10.0.0.1, Dst IP = 10.0.0.2
        incomingIp[12] = 10; incomingIp[13] = 0; incomingIp[14] = 0; incomingIp[15] = 1
        incomingIp[16] = 10; incomingIp[17] = 0; incomingIp[18] = 0; incomingIp[19] = 2

        // UDP Header: Src Port = 54321 (0xD431), Dst Port = 53 (0x0035)
        incomingIp[20] = 0xD4.toByte(); incomingIp[21] = 0x31.toByte()
        incomingIp[22] = 0x00.toByte(); incomingIp[23] = 0x35.toByte()

        val dnsResponse = ByteArray(12) { 0 } // dummy 12-byte response
        val fullPacket = DnsPacketBuilder.buildIpv4UdpPacket(incomingIp, incomingIp.size, dnsResponse)

        assertTrue(fullPacket.size == 20 + 8 + 12)

        // Swapped IPs: Src should be 10.0.0.2, Dst should be 10.0.0.1
        assertEquals(10.toByte(), fullPacket[12])
        assertEquals(2.toByte(), fullPacket[15]) // New Src IP ends in 2
        assertEquals(10.toByte(), fullPacket[16])
        assertEquals(1.toByte(), fullPacket[19]) // New Dst IP ends in 1

        // Swapped Ports: Src should be 53 (0x0035), Dst should be 54321 (0xD431)
        assertEquals(0x00.toByte(), fullPacket[20])
        assertEquals(0x35.toByte(), fullPacket[21])
        assertEquals(0xD4.toByte(), fullPacket[22])
        assertEquals(0x31.toByte(), fullPacket[23])
    }
}
