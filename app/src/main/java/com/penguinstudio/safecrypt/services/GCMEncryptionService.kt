package com.penguinstudio.safecrypt.services

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStore
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

class GCMEncryptionService @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        private const val AndroidKeyStore = "AndroidKeyStore"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val KEY_ALIAS = "MyKey"
        const val ENC_FILE_EXTENSION = "enc"

        fun generateKey() {
            val keyStore = KeyStore.getInstance(AndroidKeyStore)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator: KeyGenerator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(false)
                        .build()
                )
                keyGenerator.generateKey()
            }
        }
    }
    fun getSecretKey(): Key {
        val keyStore = KeyStore.getInstance(AndroidKeyStore)
        keyStore.load(null)

        return keyStore.getKey(KEY_ALIAS, null)
    }

    fun getCipherInputStream(uri: Uri): InputStream {
        val inputStream = context.contentResolver?.openInputStream(uri)

        val iv = ByteArray(GCM_IV_LENGTH)

        inputStream?.read(iv, 0, iv.size)

        val decryptCipher = Cipher.getInstance(AES_MODE)

        decryptCipher.init(Cipher.DECRYPT_MODE,
            getSecretKey(), GCMParameterSpec(128, iv))

        return CipherInputStream(inputStream, decryptCipher)
    }

    fun decryptFileToByteArray(uri: Uri): ByteArray {
        // Configure cipher crypt

        val inputStream = context.contentResolver.openInputStream(uri)!!

        val decryptCipher = Cipher.getInstance(AES_MODE)

        val iv = ByteArray(12)

        // Read the IV and use it for init
        inputStream.read(iv, 0, iv.size)

        val gcmIv: AlgorithmParameterSpec = GCMParameterSpec(128, iv)

        decryptCipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmIv)

//        val endArray = decryptCipher.doFinal(inputStream.readBytes())

        val buffer = ByteArray(8192)
        val outputStream = ByteArrayOutputStream()

        var read: Int

        val cis = CipherInputStream(inputStream, decryptCipher)

        while (cis.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
        }
        cis.close()

        val endArray = outputStream.toByteArray()

        return endArray
    }


    fun encryptData (
        inputStream: InputStream,
        outputStream: OutputStream) : Boolean {

        // Read/Write buffer
        val buffer = ByteArray(8192)

        // Configure cipher crypt
        val iv = ByteArray(GCM_IV_LENGTH) //NEVER REUSE THIS IV WITH SAME KEY

        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(128, iv)
        val encryptCipher = Cipher.getInstance(AES_MODE)
        encryptCipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec)


        // Prepend IV
        outputStream.write(iv)
        var nread: Int

        while (inputStream.read(buffer).also { nread = it } > 0) {
            val enc: ByteArray = encryptCipher.update(buffer, 0, nread)
            outputStream.write(enc)
        }
        val enc: ByteArray = encryptCipher.doFinal()
        outputStream.write(enc)

        return true
    }
}