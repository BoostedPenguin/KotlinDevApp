package com.penguinstudio.safecrypt.services

import android.content.Context
import android.icu.util.Output
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.documentfile.provider.DocumentFile
import com.penguinstudio.safecrypt.models.MediaModel
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.InvalidKeyException
import java.security.Key
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton

class EncryptionService @Inject constructor() {
    companion object {
        private const val AndroidKeyStore = "AndroidKeyStore"
        private const val AES_MODE = "AES/GCM/NoPadding"
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

        fun getSecretKey(): Key {
            val keyStore = KeyStore.getInstance(AndroidKeyStore)
            keyStore.load(null)

            return keyStore.getKey(KEY_ALIAS, null)
        }

        fun encryptBytes(inputBytes: ByteArray) : ByteArray {
            try {
                // Init the cipher with the provided key and random IV
                val encryptCipher = Cipher.getInstance(AES_MODE)
                encryptCipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), SecureRandom())

                // Encrypt the given input stream file
                val encryptedBytes = encryptCipher.doFinal(inputBytes)

                val iv = encryptCipher.iv

                // Payload with 16bit IV + Encrypted content
                val combinedPayload = ByteArray(iv.size + encryptedBytes.size)

                //populate payload with prefix IV and encrypted data
                System.arraycopy(iv, 0, combinedPayload, 0, iv.size)
                System.arraycopy(encryptedBytes, 0, combinedPayload, iv.size, encryptedBytes.size)

                return combinedPayload

            }
            catch (e: InvalidKeyException) {
                throw InvalidKeyException("Key wasn't in the correct format")
            }
            catch (e: FileNotFoundException) {
                throw FileNotFoundException("The file / directory wasn't found. Manually create folder")
            }
        }

        fun decryptFileToBytes(inputBytes: ByteArray) : ByteArray {

            val encryptedPayload: ByteArray = inputBytes
            val iv = ByteArray(16)
            val encryptedBytes = ByteArray(encryptedPayload.size - iv.size)

            // Copy the IV key
            System.arraycopy(encryptedPayload, 0, iv, 0, 16)

            System.arraycopy(encryptedPayload, iv.size, encryptedBytes, 0, encryptedBytes.size)

            val decryptCipher = Cipher.getInstance(AES_MODE)
            decryptCipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

            return decryptCipher.doFinal(encryptedBytes)
        }


        fun encryptData (
            inputStream: InputStream,
            outputStream: OutputStream) : Boolean {

            // Read/Write buffer
            val buffer = ByteArray(8192)

            // Configure cipher crypt
            val encryptCipher = Cipher.getInstance(AES_MODE)
            encryptCipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), SecureRandom())

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
}