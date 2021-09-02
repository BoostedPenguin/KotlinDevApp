package com.penguinstudio.safecrypt.services

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.EncryptedModel
import com.penguinstudio.safecrypt.services.glide_service.IPicture
import com.penguinstudio.safecrypt.utilities.EncryptionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaDecryptionService @Inject constructor(@ApplicationContext private val context: Context,
                                                 private val gcmEncryptionService: GCMEncryptionService,
                                                 ) {
    suspend fun decryptMedia(media: IPicture) = withContext(Dispatchers.IO) {
        // Check if user has permissions for the "encrypted folder"
        // If he doesn't redirect to select folder for images

        // Create a normal file .png / .jpeg || Empty at start
        // Write the decrypted information to the file

        // Delete the original ENCRYPTED file
        // Ask for external permission to delete file if deleting fails

        val startTime = System.currentTimeMillis()

        if(media !is EncryptedModel)
            throw IllegalArgumentException("Wrong model")


        // Get Default Encryption storage and store
        val sp: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.ENC_STORAGE_DIR_PERMISSIONS), Context.MODE_PRIVATE)

        val uriTree = sp.getString(context.getString(R.string.ENC_DIR_ROOT_TREE), "")

        // If it's empty throw catchable exception
        if (TextUtils.isEmpty(uriTree)) {
            throw NoDefaultDirFound("Please specify a default directory for storing encrypted media")
        } else {

            val uri: Uri = Uri.parse(uriTree)

            val root = DocumentFile.fromTreeUri(context, uri)

            // If you don't have rights to the root throw catchable exception
            if(root == null || !root.canRead() || !root.canWrite()) {
                throw NoDefaultDirFound("Please specify a default directory for storing encrypted media")
            }

            // Serialize encrypted file in given root location
            val decryptedEmptyFile = createDecryptedFile(root, media.imageName
                ?: throw IllegalArgumentException("File name was empty"))


            val result = async(Dispatchers.IO) {
                val outputStream = context.contentResolver.openOutputStream(decryptedEmptyFile.uri)
                    ?: throw FileNotFoundException()

                val encryptedStatus = gcmEncryptionService.decryptData(media.uri, media.mediaType, outputStream)
                outputStream.flush()
                outputStream.close()

                return@async encryptedStatus
            }

            // Delete original non-encrypted file
            val resultAwaited = result.await()
            if(resultAwaited) deleteOriginalNonEncryptedFile(media.uri)
        }
        Log.d("loadTime", "Time it took to encrypt media: ${System.currentTimeMillis() - startTime}ms")
        return@withContext EncryptionStatus.OPERATION_COMPLETE
    }

    /**
     * Create a file in the specified directory and write the decrypted bytes
     */
    private suspend fun createDecryptedFile(
        root: DocumentFile,
        imageName: String
    ): DocumentFile = withContext(Dispatchers.IO) {

        val decryptedFile = async(Dispatchers.IO) {

            // Check the file extension before the .enc extra file extension
            // Create a file with that extension

            val startIndexOfExtension = imageName.indexOf(".${GCMEncryptionService.ENC_FILE_EXTENSION}")
            if(startIndexOfExtension <= 0)
                throw IllegalArgumentException("The given file wasn't an encrypted file")

            // Search from the previous "." to the startindexofextension for a file extension and use that
            // Don't use regex for available mime types yet

            val original = imageName.substring(0, startIndexOfExtension)
            val extension = imageName.substring(imageName.lastIndexOf("."), original.length + 1)

            if(!original.contains(".")) throw IllegalArgumentException("Original file didn't have an extension")


            return@async root.createFile(
                extension,
                original)
                ?: throw NoDefaultDirFound("Couldn't create a file in the specified location")
        }

        return@withContext decryptedFile.await()
    }

    private fun deleteOriginalNonEncryptedFile(
        originalFileUri: Uri,
    ): Boolean {
        val srcDoc = DocumentFile.fromSingleUri(context, originalFileUri)
        srcDoc?.delete()

        return srcDoc?.delete() ?: false
    }
}