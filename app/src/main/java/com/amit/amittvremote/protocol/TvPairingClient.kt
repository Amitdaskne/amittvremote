package com.amit.amittvremote.protocol

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.*
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.security.*
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.*
import kotlin.math.abs

class TvPairingClient(private val ip: String) {
    private val TAG = "TvPairingClient"
    private var sslSocket: SSLSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    init {
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
    }

    suspend fun connectAndRequestPairing(): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseSocket = Socket()
            baseSocket.connect(InetSocketAddress(ip, 6467), 10000)
            baseSocket.tcpNoDelay = true

            val sslContext = createSSLContext()
            sslSocket = sslContext.socketFactory.createSocket(baseSocket, ip, 6467, true) as SSLSocket
            sslSocket?.apply {
                useClientMode = true
                enabledProtocols = supportedProtocols
                startHandshake()
            }

            inputStream = BufferedInputStream(sslSocket?.inputStream)
            outputStream = BufferedOutputStream(sslSocket?.outputStream)

            // Send pairing sequence
            sendPairingRequest()
            sendOptions()
            sendConfiguration()
            outputStream?.flush()

            Log.d(TAG, "Pairing messages sent. Check TV screen.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            false
        }
    }

    suspend fun submitPairingCode(code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val alpha = computeAlpha(code)
            val secretMsg = buildSecret(alpha)
            val complete = addLengthPrefix(secretMsg)
            outputStream?.write(complete)
            outputStream?.flush()

            // Read ACK (optional)
            val buffer = ByteArray(100)
            inputStream?.read(buffer)
            Log.d(TAG, "Secret sent. Pairing should complete.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit code", e)
            false
        }
    }

    fun disconnect() {
        try { sslSocket?.close() } catch (_: Exception) {}
    }

    // --- Certificate & SSL Context ---
    private fun createSSLContext(): SSLContext {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC")
        keyPairGenerator.initialize(2048, SecureRandom())
        val keyPair = keyPairGenerator.generateKeyPair()
        val certificate = generateCertificate(keyPair)

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setKeyEntry("client", keyPair.private, "password".toCharArray(), arrayOf(certificate))

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, "password".toCharArray())

        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, trustAllCerts, SecureRandom())
        return sslContext
    }

    private fun generateCertificate(keyPair: KeyPair): X509Certificate {
        val now = Date()
        val notBefore = Date(now.time - 1000L * 60 * 60 * 24)
        val notAfter = Date(now.time + 1000L * 60 * 60 * 24 * 365 * 10)
        val serialNumber = BigInteger.valueOf(now.time)
        val subject = X500Name("CN=androidtv-remote")
        val publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)

        val certBuilder = X509v3CertificateBuilder(
            subject, serialNumber, notBefore, notAfter, subject, publicKeyInfo
        )
        try {
            certBuilder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))
            certBuilder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment))
            certBuilder.addExtension(Extension.extendedKeyUsage, true, ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth))
        } catch (_: Exception) {}

        val signer = JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyPair.private)
        val certHolder = certBuilder.build(signer)
        return JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder)
    }

    // --- Protobuf Message Builders ---
    private fun buildPairingRequest(): ByteArray {
        val payload = ByteArrayOutputStream()
        writeString(payload, 1, "androidtv-remote") // service name
        writeString(payload, 2, "Android Phone")    // client name
        return wrapInPairingMessage(1, payload.toByteArray())
    }

    private fun buildOptions(): ByteArray {
        val payload = ByteArrayOutputStream()
        writeVarint(payload, (1 shl 3) or 0) // preferred_role field
        writeVarint(payload, 1) // ROLE_TYPE_INPUT
        // input_encodings (field 2, repeated)
        val encoding = buildEncoding()
        writeVarint(payload, (2 shl 3) or 2)
        writeVarint(payload, encoding.size)
        payload.write(encoding)
        return wrapInPairingMessage(2, payload.toByteArray())
    }

    private fun buildConfiguration(): ByteArray {
        val payload = ByteArrayOutputStream()
        writeVarint(payload, (1 shl 3) or 0)
        writeVarint(payload, 1) // client_role = INPUT
        val encoding = buildEncoding()
        writeVarint(payload, (2 shl 3) or 2)
        writeVarint(payload, encoding.size)
        payload.write(encoding)
        return wrapInPairingMessage(3, payload.toByteArray())
    }

    private fun buildEncoding(): ByteArray {
        val out = ByteArrayOutputStream()
        writeVarint(out, (1 shl 3) or 0); writeVarint(out, 1) // HEXADECIMAL
        writeVarint(out, (2 shl 3) or 0); writeVarint(out, 6) // symbol_length
        return out.toByteArray()
    }

    private fun buildSecret(alpha: ByteArray): ByteArray {
        val payload = ByteArrayOutputStream()
        writeBytes(payload, 1, alpha)
        return wrapInPairingMessage(4, payload.toByteArray())
    }

    private fun wrapInPairingMessage(fieldNumber: Int, payload: ByteArray): ByteArray {
        val wrapper = ByteArrayOutputStream()
        writeVarint(wrapper, (fieldNumber shl 3) or 2)
        writeVarint(wrapper, payload.size)
        wrapper.write(payload)
        // status = OK (200, 1)
        writeVarint(wrapper, (200 shl 3) or 0)
        writeVarint(wrapper, 200); writeVarint(wrapper, 1)
        // protocol version = 2
        writeVarint(wrapper, (201 shl 3) or 0)
        writeVarint(wrapper, 2)
        return wrapper.toByteArray()
    }

    // --- Utility ---
    private fun addLengthPrefix(message: ByteArray): ByteArray {
        val result = ByteArray(message.size + 1)
        result[0] = message.size.toByte()
        System.arraycopy(message, 0, result, 1, message.size)
        return result
    }

    private fun writeString(out: OutputStream, field: Int, value: String) {
        writeBytes(out, field, value.toByteArray(Charsets.UTF_8))
    }

    private fun writeBytes(out: OutputStream, field: Int, bytes: ByteArray) {
        writeVarint(out, (field shl 3) or 2)
        writeVarint(out, bytes.size)
        out.write(bytes)
    }

    private fun writeVarint(out: OutputStream, value: Int) {
        var v = value
        while (v and 0x7F.inv() != 0) {
            out.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.write(v and 0x7F)
    }

    private fun computeAlpha(code: String): ByteArray {
        val codeBytes = code.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val clientCert = sslSocket?.session?.localCertificates?.get(0)
        val serverCert = sslSocket?.session?.peerCertificates?.get(0)
        if (clientCert != null && serverCert != null) {
            val digest = MessageDigest.getInstance("SHA-256")
            val clientPub = clientCert.publicKey as? java.security.interfaces.RSAPublicKey
            val serverPub = serverCert.publicKey as? java.security.interfaces.RSAPublicKey
            if (clientPub != null && serverPub != null) {
                digest.update(clientPub.modulus.abs().toByteArray())
                digest.update(clientPub.publicExponent.abs().toByteArray())
                digest.update(serverPub.modulus.abs().toByteArray())
                digest.update(serverPub.publicExponent.abs().toByteArray())
                digest.update(codeBytes)
                return digest.digest()
            }
        }
        return codeBytes
    }

    // Helper to send messages
    private suspend fun sendPairingRequest() = withContext(Dispatchers.IO) {
        val msg = buildPairingRequest()
        outputStream?.write(addLengthPrefix(msg))
    }
    private suspend fun sendOptions() = withContext(Dispatchers.IO) {
        val msg = buildOptions()
        outputStream?.write(addLengthPrefix(msg))
    }
    private suspend fun sendConfiguration() = withContext(Dispatchers.IO) {
        val msg = buildConfiguration()
        outputStream?.write(addLengthPrefix(msg))
    }
}
