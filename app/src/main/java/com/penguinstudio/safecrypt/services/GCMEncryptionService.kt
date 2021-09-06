package com.penguinstudio.safecrypt.services

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.Key
import java.security.KeyStore
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.penguinstudio.safecrypt.models.MediaType
import com.penguinstudio.safecrypt.services.glide_service.SafeCryptModelLoader
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.*
import java.lang.IllegalArgumentException
import javax.inject.Singleton

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

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ImageCompressorServicePoint {
        fun getService(): ImageCompressor
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

    fun decryptData (
        uri: Uri,
        outputStream: OutputStream) : Boolean {
        val startTime = System.currentTimeMillis()

        // Doesn't support videos yet

        val buffer = ByteArray(8192)

        val iv = ByteArray(GCM_IV_LENGTH)

        val inputStream = context.contentResolver.openInputStream(uri)!!

        val decryptCipher = Cipher.getInstance(AES_MODE)

        // Read the IV and use it for init
        inputStream.read(iv, 0, iv.size)

        val gcmIv: AlgorithmParameterSpec = GCMParameterSpec(128, iv)

        decryptCipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmIv)

        var read: Int

        val cis = CipherInputStream(inputStream, decryptCipher)

        while (cis.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
        }
        cis.close()
        outputStream.flush()
        outputStream.close()
        Log.d("loadTime", "Time it took to decrypt bytes: ${System.currentTimeMillis() - startTime}ms")

        return true
    }

    /** TODO Encrypts files
    * Do ##NOT Encrypt videos. At the moment is supports only images
    */
    fun encryptData (
        uri: Uri,
        mediaType: MediaType,
        outputStream: OutputStream) : Boolean {

        val imageCompressor = EntryPoints.get(context, ImageCompressorServicePoint::class.java).getService()

        // Read/Write buffer
        val buffer = ByteArray(8192)

        // Configure cipher crypt
        val iv = ByteArray(GCM_IV_LENGTH) //NEVER REUSE THIS IV WITH SAME KEY

        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(128, iv)
        val encryptCipher = Cipher.getInstance(AES_MODE)
        encryptCipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec)


        val inpStream = when(mediaType) {
            MediaType.IMAGE -> {
                val compressed = imageCompressor.getBitmapFormUri(uri)
                    ?: throw IllegalArgumentException("Couldn't open an input stream")

                val degree = imageCompressor.getBitmapDegree(uri)
                imageCompressor.rotateBitmapByDegree(compressed, degree.toFloat())
            }
            MediaType.VIDEO -> {
                context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Couldn't open an input stream")
            }
        }



        // Prepend IV
        outputStream.write(iv)
        var nread: Int


        while (inpStream.read(buffer).also { nread = it } > 0) {
            val enc: ByteArray = encryptCipher.update(buffer, 0, nread)
            outputStream.write(enc)
        }
        val enc: ByteArray = encryptCipher.doFinal()
        outputStream.write(enc)
        inpStream.close()

        return true
    }
}