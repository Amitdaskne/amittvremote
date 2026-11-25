package com.amit.amittvremote.protocol

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object TvRemoteProtocol {
    private const val TAG = "TvRemoteProtocol"
    private const val PAIRING_PORT = 6467
    private const val COMMAND_PORT = 6466

    // Trust all certificates (minimal implementation)
    private fun getTrustAllContext(): SSLContext {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        return sslContext
    }

    suspend fun connectForPairing(ip: String): Result<PairingConnection> = withContext(Dispatchers.IO) {
        try {
            val socket = getTrustAllContext().socketFactory.createSocket(ip, PAIRING_PORT) as SSLSocket
            socket.startHandshake()
            PairingConnection(socket).let { Result.success(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Pairing connection failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendPairingCode(connection: PairingConnection, code: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val output = connection.socket.outputStream
                // Simplified: Send the code as a string (actual protocol is more complex)
                output.write(code.toByteArray())
                output.flush()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Sending pairing code failed", e)
                false
            }
        }
    }

    suspend fun connectForCommands(ip: String): Result<CommandConnection> = withContext(Dispatchers.IO) {
        try {
            val socket = getTrustAllContext().socketFactory.createSocket(ip, COMMAND_PORT) as SSLSocket
            socket.startHandshake()
            CommandConnection(socket).let { Result.success(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Command connection failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendKey(connection: CommandConnection, keyCode: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val output = connection.socket.outputStream
                // Simplified: Send keycode as a byte array (actual protocol uses protobuf)
                output.write(byteArrayOf(keyCode.toByte()))
                output.flush()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Sending key failed", e)
                false
            }
        }
    }

    data class PairingConnection(val socket: SSLSocket)
    data class CommandConnection(val socket: SSLSocket)

    // Common Android Key Codes
    object Keys {
        const val POWER = 26
        const val VOLUME_UP = 24
        const val VOLUME_DOWN = 25
        const val MUTE = 164
        const val DPAD_UP = 19
        const val DPAD_DOWN = 20
        const val DPAD_LEFT = 21
        const val DPAD_RIGHT = 22
        const val DPAD_CENTER = 23
        const val BACK = 4
        const val HOME = 3
        const val MENU = 82
        const val TV_INPUT = 178
    }
}
