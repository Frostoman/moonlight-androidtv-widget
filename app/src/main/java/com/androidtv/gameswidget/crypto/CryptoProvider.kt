package com.androidtv.gameswidget.crypto

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date

/**
 * Generates and persists this device's client X.509 certificate + RSA key pair.
 *
 * The same identity is reused across pairings so the host keeps recognising us.
 * Mirrors moonlight-android's AndroidCryptoProvider: self-signed RSA-2048,
 * CN="NVIDIA GameStream Client", valid ~20 years, SHA256withRSA.
 */
class CryptoProvider(context: Context) {

    private val certFile = File(context.filesDir, "client.crt")
    private val keyFile = File(context.filesDir, "client.key")

    val clientCertificate: X509Certificate
    val clientPrivateKey: PrivateKey

    /** PEM-encoded ("-----BEGIN CERTIFICATE-----") cert bytes, as sent in the pair request. */
    val pemEncodedClientCertificate: ByteArray

    init {
        if (certFile.exists() && keyFile.exists()) {
            clientCertificate = loadCertificate(certFile.readBytes())
            clientPrivateKey = loadPrivateKey(keyFile.readBytes())
        } else {
            val kpg = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
            val kp = kpg.generateKeyPair()

            val now = System.currentTimeMillis()
            val notBefore = Date(now - 24L * 60 * 60 * 1000)
            val notAfter = Date(now + 20L * 365 * 24 * 60 * 60 * 1000)
            val dn = X500Name("CN=NVIDIA GameStream Client")

            val builder = JcaX509v3CertificateBuilder(
                dn, BigInteger.valueOf(now), notBefore, notAfter, dn, kp.public,
            )
            val signer = JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(kp.private)
            val cert = JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(builder.build(signer))

            clientCertificate = cert
            clientPrivateKey = kp.private

            certFile.writeBytes(cert.encoded)
            keyFile.writeBytes(kp.private.encoded)
        }
        pemEncodedClientCertificate = toPem(clientCertificate)
    }

    private fun loadCertificate(der: ByteArray): X509Certificate =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(der)) as X509Certificate

    private fun loadPrivateKey(pkcs8: ByteArray): PrivateKey =
        KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(pkcs8))

    private fun toPem(cert: X509Certificate): ByteArray {
        val sw = StringWriter()
        JcaPEMWriter(sw).use { it.writeObject(cert) }
        return sw.toString().toByteArray(Charsets.UTF_8)
    }
}
