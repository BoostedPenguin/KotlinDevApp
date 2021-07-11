package com.penguinstudio.safecrypt.services

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.documentfile.provider.DocumentFile
import com.penguinstudio.safecrypt.models.MediaModel
import java.io.OutputStream
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

interface EncryptionManagerListeners {
    fun onRequestSaveStorage()
    fun onDeleteRecoverableSecurityAlert(senderRequest: IntentSenderRequest)
    fun onOperationComplete(position: Int)
}
@Singleton
class MediaEncryptionService @Inject constructor() {

    fun encryptImage(
        position: Int,
        image: MediaModel,
        context: Context,
        requestSavedStorageListener: EncryptionManagerListeners
    ) {

        // Default Storage location
        val sp: SharedPreferences =
            context.getSharedPreferences("DirPermission", Context.MODE_PRIVATE)

        val uriTree = sp.getString("uriTree", "")


        if (TextUtils.isEmpty(uriTree)) {

            // Request default storage location
            // Will stop executing further encryption
            requestSavedStorageListener.onRequestSaveStorage()
        } else {

            val uri: Uri = Uri.parse(uriTree)

            val inputStream = context.contentResolver.openInputStream(image.mediaUri)

            val inputBytes = inputStream?.readBytes()

            inputStream?.close()

            if (inputBytes == null) {
                Toast.makeText(context, "Input file has no content", Toast.LENGTH_SHORT).show()
                return
            }

            // Encrypted bytes output
            val startTime = System.currentTimeMillis()

            val encryptedBytes = EncryptionService.encryptBytes(inputBytes)
            Log.d("loadTime", "Encrypting bytes takes: ${System.currentTimeMillis() - startTime} ms")

            val root = DocumentFile.fromTreeUri(context, uri)

            if(root == null) {
                Toast.makeText(context, "Root directory was null. Operation aborted.", Toast.LENGTH_SHORT).show()
                return
            }

            val encryptedFile = createEncryptedFile(context, encryptedBytes, root, image.mediaName)

            // If un-successfully created an encrypted file stop execution
            if (encryptedFile == null) {
                Toast.makeText(
                    context,
                    "Unsuccessful attempt to create an encrypted file. Operation aborted.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // If successfully deleted the original file
            if (deleteOriginalNonEncryptedFile(context, image.mediaUri, requestSavedStorageListener)) {
                requestSavedStorageListener.onOperationComplete(
                    position,
                )
            }
        }
    }

    /**
     * Create a file in the specified directory and write the encrypted bytes
     */
    private fun createEncryptedFile(
        context: Context,
        encryptedByteArray: ByteArray,
        root: DocumentFile,
        imageName: String
    ): DocumentFile? {
        var encryptedFile: DocumentFile? = null
        var outputStream: OutputStream? = null

        try {

            encryptedFile = root.createFile(EncryptionService.ENC_FILE_EXTENSION, imageName.plus(".${EncryptionService.ENC_FILE_EXTENSION}"))

            if(encryptedFile != null) {
                outputStream = context.contentResolver.openOutputStream(encryptedFile.uri)
                outputStream?.write(encryptedByteArray)
            }

        } catch (e: Exception) {
            e.message?.let { Log.d("Exception", it) }
        } finally {
            outputStream?.flush()
            outputStream?.close()
            return encryptedFile
        }
    }

    private fun deleteOriginalNonEncryptedFile(
        context: Context,
        originalFileUri: Uri,
        requestSavedStorageListener: EncryptionManagerListeners?
    ): Boolean {
        val resolver = context.contentResolver

        try {
            val numImagesRemoved = resolver.delete(
                originalFileUri,
                null,
                null
            )
            return numImagesRemoved == 1
        } catch (e: SecurityException) {

            // TODO Check if this would work on android 11
            // Should create an intent for permissions for a given file
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recoverableSecurityException = e as?
                        RecoverableSecurityException ?: throw RuntimeException(e.message, e)

                val intentSender =
                    recoverableSecurityException.userAction.actionIntent.intentSender

                val intentSenderRequest =
                    IntentSenderRequest.Builder(intentSender).build()

                requestSavedStorageListener?.onDeleteRecoverableSecurityAlert(
                    intentSenderRequest
                )
            } else {
                throw RuntimeException(e.message, e)
            }
            return false
        }
    }
}