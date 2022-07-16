package com.penguinstudio.safecrypt.services

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyProperties
import android.util.Log
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.TransferListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.security.Key
import java.security.KeyStore
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptedDataSource(private val context: Context, private val key: Key) : DataSource {
    private var inputStream: CipherInputStream? = null
    private lateinit var uri: Uri

    override fun addTransferListener(transferListener: TransferListener) {}

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        try {



            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            val ivReaderStream = context.contentResolver.openInputStream(uri)!!

            val iv = ByteArray(12)
            ivReaderStream.read(iv, 0, iv.size)
            val gcmIv: AlgorithmParameterSpec = GCMParameterSpec(128, iv)

            cipher.init(Cipher.DECRYPT_MODE, key, gcmIv)
            inputStream = CipherInputStream(ivReaderStream, cipher)

            if (dataSpec.position != 0L) {
                inputStream?.forceSkip(dataSpec.position) // Needed for skipping
            }

        } catch (e: Exception) {
            Log.d("ERR",e.stackTraceToString())
        }
        return dataSpec.length
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int =
        if (readLength == 0) {
            0
        } else {
            inputStream?.read(buffer, offset, readLength) ?: 0
        }

    override fun getUri(): Uri? =
        uri

    @Throws(IOException::class)
    override fun close() {
        inputStream?.close()
    }
}

/**
 * Skip bytes by reading them to a specific point.
 * This is needed in GCM because the Authorisation Tag wont match when bytes are really skipped.
 */
fun CipherInputStream.forceSkip(bytesToSkip: Long): Long {
    var processedBytes = 0L
    while (processedBytes < bytesToSkip) {
        read()
        processedBytes++
    }

    return processedBytes
}