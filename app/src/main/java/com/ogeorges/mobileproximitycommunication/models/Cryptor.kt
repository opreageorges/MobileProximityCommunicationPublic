package com.ogeorges.mobileproximitycommunication.models

import android.content.Context
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.math.BigInteger
import java.security.*
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.security.auth.x500.X500Principal

object Cryptor {
    private lateinit var messagesKeyPair:KeyPair
    private const val keyAlias = "MPCmessagesKEY"

    fun initKeys(context: Context){
        messagesKeyPair = loadKey() ?: generateKey(context)
    }

    private fun loadKey():KeyPair?{
        val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.aliases().toList().contains(keyAlias)) return null

        val entry: KeyStore.Entry
        try {
            entry = keyStore.getEntry(keyAlias, null)
        }
        catch (e:NullPointerException){
            return null
        }
        return KeyPair(keyStore.getCertificate(keyAlias).publicKey, (entry as KeyStore.PrivateKeyEntry).privateKey)
    }

    private fun generateKey(context: Context):KeyPair {
        val startDate = GregorianCalendar()
        val endDate = GregorianCalendar()
        endDate.add(Calendar.YEAR, 1)

        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        val parameterSpec: AlgorithmParameterSpec

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            parameterSpec = KeyGenParameterSpec.Builder(keyAlias,
                KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT).run {
                setCertificateSerialNumber(BigInteger.valueOf(420))
                setKeySize(4096)
                setCertificateSubject(X500Principal("CN=$keyAlias"))
                setDigests(KeyProperties.DIGEST_SHA256)
                setRandomizedEncryptionRequired(true)
                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                setKeyValidityStart(startDate.time)
                setKeyValidityEnd(endDate.time)
                build()
            }
        }
        else {
            @Suppress("DEPRECATION")
             parameterSpec = KeyPairGeneratorSpec.Builder(context)
                 .setAlias(keyAlias)
                 .setSerialNumber(BigInteger.valueOf(420))
                 .setSubject(X500Principal("CN=$keyAlias"))
                 .setStartDate(startDate.time)
                 .setEndDate(endDate.time)
                 .setKeySize(4096)
                 .build()
        }
        keyPairGenerator.initialize(parameterSpec)

        return keyPairGenerator.genKeyPair()

    }

    fun getMessagesPublicKey():PublicKey{
        return messagesKeyPair.public
    }

    fun encryptMessage(message: String, publicKey: PublicKey): String{
        val cipher: Cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val chunks = message.toByteArray().toList().chunked(256)
        val encryptedBytes: ArrayList<Byte> = ArrayList()
        for (chunk in chunks){
            val encryptedChunk = cipher.doFinal(chunk.toByteArray())
            encryptedChunk.forEach {
                encryptedBytes.add(it)
            }
        }

        return Base64.encodeToString(encryptedBytes.toByteArray(), Base64.NO_PADDING)
    }

    fun decryptMessage(message: String): String {
        val cipher: Cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, messagesKeyPair.private)
        val encryptedData = Base64.decode(message, Base64.NO_PADDING)

        val chunks = encryptedData.toList().chunked(512)
        val decryptedBytes: ArrayList<Byte> = ArrayList()
        for (chunk in chunks) {
            val encryptedChunk = cipher.doFinal(chunk.toByteArray())
            encryptedChunk.forEach {
                decryptedBytes.add(it)
            }
        }
        //        val decodedData = cipher.doFinal(encryptedData)
        return String(decryptedBytes.toByteArray())
    }

    fun byteArrayToStringLossless(byteArray: ByteArray):String{
        return byteArray.toList().map { it.toInt() }.joinToString(" ") { it.toString() }
    }

    fun stringToByteArrayLossless(string: String):ByteArray{
        return string.trim().split(" ").map { it.toInt() }.map { it.toByte() }.toByteArray()
    }

    fun stringToPublicKey(key:ByteArray): PublicKey{
        return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(key))
    }

}