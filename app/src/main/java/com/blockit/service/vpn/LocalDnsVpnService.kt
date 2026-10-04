package com.blockit.service.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.blockit.core.BlockItApplication
import com.blockit.data.local.db.entity.BlockEventEntity
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.TargetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class LocalDnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null
    private lateinit var ruleEvaluator: RuleEvaluator
    private lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        val container = BlockItApplication.from(this)
        ruleEvaluator = container.ruleEvaluator
        appScope = container.applicationScope
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        if (vpnJob == null || vpnJob?.isActive == false) {
            startVpnTunnel()
        }

        return START_STICKY
    }

    private fun startVpnTunnel() {
        vpnJob = appScope.launch(Dispatchers.IO) {
            try {
                val builder = Builder()
                    .setSession("BlockIt Local DNS")
                    .addAddress("10.254.1.1", 32)
                    .addRoute("10.254.1.1", 32)
                    .addDnsServer("10.254.1.1")
                    .setMtu(1500)
                    .setBlocking(true)

                vpnInterface = builder.establish() ?: return@launch
                val pfd = vpnInterface ?: return@launch

                val inputStream = FileInputStream(pfd.fileDescriptor)
                val outputStream = FileOutputStream(pfd.fileDescriptor)

                val packetBuffer = ByteArray(32767)

                // Upstream fallback resolver (Cloudflare 1.1.1.1)
                val upstreamDnsIp = InetAddress.getByName("1.1.1.1")

                while (coroutineContext[Job]?.isActive == true) {
                    val bytesRead = inputStream.read(packetBuffer)
                    if (bytesRead <= 0) continue

                    // Inspect IPv4 Header
                    val version = (packetBuffer[0].toInt() and 0xF0) shr 4
                    val protocol = packetBuffer[9].toInt() and 0xFF
                    val ipHeaderLen = (packetBuffer[0].toInt() and 0x0F) * 4

                    // Intercept UDP (17) on Port 53 (DNS)
                    if (version == 4 && protocol == 17 && bytesRead > ipHeaderLen + 8) {
                        val dstPort = ((packetBuffer[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or
                                (packetBuffer[ipHeaderLen + 3].toInt() and 0xFF)

                        if (dstPort == 53) {
                            val dnsOffset = ipHeaderLen + 8
                            val dnsLen = bytesRead - dnsOffset
                            val qname = DnsPacketParser.extractQuestionName(packetBuffer, dnsOffset, dnsLen)

                            if (qname != null) {
                                val eval = ruleEvaluator.isDomainBlocked(qname)
                                if (eval.isBlocked) {
                                    // Blocked: Synthesize NXDOMAIN response packet
                                    val nxDomainPayload = DnsPacketBuilder.buildNxDomainResponse(
                                        packetBuffer.copyOfRange(dnsOffset, dnsOffset + dnsLen),
                                        dnsLen
                                    )
                                    val fullPacket = DnsPacketBuilder.buildIpv4UdpPacket(
                                        packetBuffer,
                                        bytesRead,
                                        nxDomainPayload
                                    )
                                    outputStream.write(fullPacket)

                                    // Record audit log asynchronously
                                    val profile = eval.profileName ?: "Focus"
                                    launch(Dispatchers.IO) {
                                        try {
                                            val db = BlockItApplication.from(this@LocalDnsVpnService).database
                                            db.blockEventDao().insertEvent(
                                                BlockEventEntity(
                                                    timestamp = System.currentTimeMillis(),
                                                    targetType = TargetType.WEBSITE.name,
                                                    identifier = qname,
                                                    profileName = profile
                                                )
                                            )
                                        } catch (_: Exception) {}
                                    }
                                    continue
                                }
                            }

                            // Allowed: Forward upstream using protected socket
                            forwardDnsUpstream(
                                packetBuffer = packetBuffer,
                                dnsOffset = dnsOffset,
                                dnsLen = dnsLen,
                                bytesRead = bytesRead,
                                outputStream = outputStream,
                                upstreamIp = upstreamDnsIp
                            )
                            continue
                        }
                    }
                }
            } catch (_: Exception) {
                // VPN channel closed or error
            } finally {
                closeVpn()
            }
        }
    }

    private fun forwardDnsUpstream(
        packetBuffer: ByteArray,
        dnsOffset: Int,
        dnsLen: Int,
        bytesRead: Int,
        outputStream: FileOutputStream,
        upstreamIp: InetAddress
    ) {
        try {
            val socket = DatagramSocket()
            protect(socket) // Ensure upstream request escapes the local VPN tunnel
            socket.soTimeout = 2000

            val queryPacket = DatagramPacket(packetBuffer, dnsOffset, dnsLen, upstreamIp, 53)
            socket.send(queryPacket)

            val respBuffer = ByteArray(1500)
            val respPacket = DatagramPacket(respBuffer, respBuffer.size)
            socket.receive(respPacket)
            socket.close()

            val realResponsePayload = respBuffer.copyOfRange(0, respPacket.length)
            val responseIpPacket = DnsPacketBuilder.buildIpv4UdpPacket(
                packetBuffer,
                bytesRead,
                realResponsePayload
            )
            outputStream.write(responseIpPacket)
        } catch (_: Exception) {
            // DNS resolution timeout or error
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BlockIt Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps website and app blocking active locally"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BlockIt Protection Active")
            .setContentText("Local on-device website and app blocking is active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun closeVpn() {
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
    }

    override fun onDestroy() {
        super.onDestroy()
        vpnJob?.cancel()
        closeVpn()
    }

    companion object {
        private const val CHANNEL_ID = "blockit_vpn_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, LocalDnsVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocalDnsVpnService::class.java)
            context.stopService(intent)
        }
    }
}
