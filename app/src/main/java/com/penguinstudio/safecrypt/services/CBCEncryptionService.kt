package com.penguinstudio.safecrypt.services

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.*
import java.security.InvalidKeyException
import java.security.Key
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject

class CBCEncryptionService @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        private const val ALGO_IMAGE_ENCRYPTOR = "AES/CBC/PKCS7Padding"
        private const val AndroidKeyStore = "AndroidKeyStore"
        private const val KEY_ALIAS = "MyKey"
        const val ENC_FILE_EXTENSION = "enc"

        fun generateKey() {
            val keyStore = KeyStore.getInstance(AndroidKeyStore)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator: KeyGenerator =
                    KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        AndroidKeyStore
                    )
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setRandomizedEncryptionRequired(false)
                        .build()
                )
                keyGenerator.generateKey()
            }
        }


        fun getSecretKey(): Key {
            val keyStore = KeyStore.getInstance(AndroidKeyStore)
            keyStore.load(null)

            return keyStore.getKey(KEY_ALIAS, null)
        }
    }

    fun getCipherInputStream(uri: Uri): InputStream {
        val inputStream = context.contentResolver?.openInputStream(uri)

        val iv = ByteArray(16)

        inputStream?.read(iv, 0, iv.size)

        val decryptCipher = Cipher.getInstance(ALGO_IMAGE_ENCRYPTOR)
        decryptCipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

        return CipherInputStream(inputStream, decryptCipher)
    }


    fun encryptBytes(inputStream: InputStream, outputStream: OutputStream) : Boolean {
        try {
            // Init the cipher with the provided key and random IV
            val encryptCipher = Cipher.getInstance(ALGO_IMAGE_ENCRYPTOR);
            encryptCipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), SecureRandom())

            // Encrypt the given input stream file
            val encryptedBytes = encryptCipher.doFinal(inputStream.readBytes())

            val iv = encryptCipher.iv

            // Payload with 16bit IV + Encrypted content
            val combinedPayload = ByteArray(iv.size + encryptedBytes.size)

            //populate payload with prefix IV and encrypted data
            System.arraycopy(iv, 0, combinedPayload, 0, iv.size);
            System.arraycopy(encryptedBytes, 0, combinedPayload, iv.size, encryptedBytes.size);


            outputStream.write(combinedPayload)
            return true

        }
        catch (e: InvalidKeyException) {
            throw InvalidKeyException("Key wasn't in the correct format")
        }
        catch (e: FileNotFoundException) {
            throw FileNotFoundException("The file / directory wasn't found. Manually create folder")
        }
    }

    fun decryptFileToBytes(inputStream: InputStream) : ByteArray {
        val encryptedPayload: ByteArray = inputStream.readBytes()
        val iv = ByteArray(16)
        val encryptedBytes = ByteArray(encryptedPayload.size - iv.size)

        // Copy the IV key
        System.arraycopy(encryptedPayload, 0, iv, 0, 16)

        System.arraycopy(encryptedPayload, iv.size, encryptedBytes, 0, encryptedBytes.size)

        val decryptCipher = Cipher.getInstance(ALGO_IMAGE_ENCRYPTOR)
        decryptCipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

        return decryptCipher.doFinal(encryptedBytes)
    }
}