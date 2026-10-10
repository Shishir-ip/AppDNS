package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

object DnsPingTester {

    /**
     * Measures latency in milliseconds to the DNS server's port 53 (TCP handshake),
     * or port 853 for DoT, or port 443 for DoH servers.
     */
    suspend fun pingDnsServer(ipOrHost: String, port: Int = 53, timeoutMs: Int = 2500): Long =
        withContext(Dispatchers.IO) {
            try {
                var elapsed: Long
                Socket().use { socket ->
                    val address = InetSocketAddress(ipOrHost, port)
                    elapsed = measureTimeMillis {
                        socket.connect(address, timeoutMs)
                    }
                }
                elapsed
            } catch (e: Exception) {
                // Return -1 to signify unreachable or timeout
                -1L
            }
        }
}
