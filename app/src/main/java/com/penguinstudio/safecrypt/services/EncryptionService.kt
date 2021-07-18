package com.penguinstudio.safecrypt.services

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.TextUtils
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.security.InvalidKeyException
import java.security.Key
import java.security.KeyStore
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton

class EncryptionService @Inject constructor() {
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

        fun storeNewKey(key: ByteArray) {
            val keyStore = KeyStore.getInstance(AndroidKeyStore)
            keyStore.load(null)

            if(keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.setKeyEntry(KEY_ALIAS, key, null)
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


        fun massDecrypt(context: Context) : MutableList<ByteArray> {
            val sp: SharedPreferences = context.getSharedPreferences("DirPermission", Context.MODE_PRIVATE)
            val uriTree = sp.getString("uriTree", "")

            if (TextUtils.isEmpty(uriTree)) {
                //chooseDefaultSaveLocation()
            } else {
                val startTime = System.currentTimeMillis()


                val uri: Uri = Uri.parse(uriTree)

                // Root directory > where to look for encrypted files
                val root = DocumentFile.fromTreeUri(context, uri)

                val decryptedBitmaps = mutableListOf<ByteArray>()

                root?.listFiles()?.forEach {
                    val inputStream = context.contentResolver.openInputStream(it.uri)
                        ?: throw IllegalArgumentException("No inp stream")

                    val bitmap = decryptFileToByteArray(inputStream)

                    inputStream.close()
                    decryptedBitmaps.add(bitmap)
                }
                Log.e("thefuck", "Time to decrypt images ${System.currentTimeMillis() - startTime} ms")

                return decryptedBitmaps
            }
            throw IllegalArgumentException("Shouldn't be here")
        }


        fun decryptFileToByteArray(inputStream: InputStream): ByteArray {
            // Configure cipher crypt

            val decryptCipher = Cipher.getInstance(AES_MODE)

            val iv = ByteArray(12)

            // Read the IV and use it for init
            inputStream.read(iv, 0, iv.size)

            val gcmIv: AlgorithmParameterSpec = GCMParameterSpec(128, iv)

            decryptCipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmIv)
            val startTime = System.currentTimeMillis()

            val result = decryptCipher.doFinal()



            Log.e("thefuck", "Time to decrypt one image ${System.currentTimeMillis() - startTime} ms")

            return result
        }

        fun decryptFileToBitmap(inputStream: InputStream): Bitmap {
            // Configure cipher crypt

            val decryptCipher = Cipher.getInstance(AES_MODE)

            val iv = ByteArray(12)

            // Read the IV and use it for init
            inputStream.read(iv, 0, iv.size)

            val gcmIv: AlgorithmParameterSpec = GCMParameterSpec(128, iv)

            decryptCipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmIv)


            val cis = CipherInputStream(inputStream, decryptCipher)


            //val result = cis.readBytes()

            val startTime = System.currentTimeMillis()

            //val g = BitmapFactory.decodeByteArray(result, 0, result.size)
            val g = BitmapFactory.decodeStream(cis)

            cis.close()

            Log.d("loadTime", "Time to decrypt ${System.currentTimeMillis() - startTime} ms")

            return g
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
}