package com.penguinstudio.safecrypt.services

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.utilities.EncryptionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

class NoDefaultDirFound(message: String) : Exception(message)

@Singleton
class MediaEncryptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gcmEncryptionService: GCMEncryptionService
    ) {

    suspend fun encryptImage(
        image: MediaModel,
    ) = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()


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
            val encryptedEmptyFile = createEncryptedFile(root, image.mediaName)


            val result = async(Dispatchers.IO) {
                val outputStream = context.contentResolver.openOutputStream(encryptedEmptyFile.uri)
                    ?: throw FileNotFoundException()

                val encryptedStatus = gcmEncryptionService.encryptData(image.mediaUri, image.mediaType, outputStream)
                outputStream.flush()
                outputStream.close()

                return@async encryptedStatus
            }

            // Delete original non-encrypted file
            val resultAwaited = result.await()
            if(resultAwaited) deleteOriginalNonEncryptedFile(image.mediaUri)
        }
        Log.d("loadTime", "Time it took to encrypt media: ${System.currentTimeMillis() - startTime}ms")
        return@withContext EncryptionStatus.OPERATION_COMPLETE

    }

    /**
     * Create a file in the specified directory and write the encrypted bytes
     */
    private suspend fun createEncryptedFile(
        root: DocumentFile,
        imageName: String
    ): DocumentFile = withContext(Dispatchers.IO) {

        val encryptedFile = async(Dispatchers.IO) {

            return@async root.createFile(
                CBCEncryptionService.ENC_FILE_EXTENSION,
                imageName.plus(".${CBCEncryptionService.ENC_FILE_EXTENSION}")
            )
                ?: throw NoDefaultDirFound("Couldn't create a file in the specified location")
        }

        return@withContext encryptedFile.await()
    }

    private fun deleteOriginalNonEncryptedFile(
        originalFileUri: Uri,
    ): Boolean {
        val resolver = context.contentResolver

        val numImagesRemoved = resolver.delete(
            originalFileUri,
            null,
            null
        )
        return numImagesRemoved == 1
    }
}