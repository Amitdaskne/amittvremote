package com.amit.amittvremote.protocol

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class TvCommandClient(private val ip: String) {
    private var sslSocket: SSLSocket? = null
    private var outputStream: OutputStream? = null

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            sslSocket = sslContext.socketFactory.createSocket(ip, 6466) as SSLSocket
            sslSocket?.startHandshake()
            outputStream = sslSocket?.outputStream
            true
        } catch (e: Exception) {
            Log.e("TvCommandClient", "Connect failed", e)
            false
        }
    }

    suspend fun sendKey(keyCode: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // RemoteMessage protobuf format for key press (simplified)
            // Real protobuf has specific field numbers. Using a placeholder byte array.
            // You may need to adjust this based on remotemessage.proto
            val keyPayload = byteArrayOf(8, 2, 16, -56, 1, 82, 5, 10, 2, 8, keyCode.toByte())
            outputStream?.write(keyPayload.size)
            outputStream?.write(keyPayload)
            outputStream?.flush()
            true
        } catch (e: Exception) {
            Log.e("TvCommandClient", "Send key failed", e)
            false
        }
    }

    fun disconnect() {
        try { sslSocket?.close() } catch (_: Exception) {}
    }
}
