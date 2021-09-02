package com.penguinstudio.safecrypt.repository

import android.app.RecoverableSecurityException
import android.os.Build
import androidx.activity.result.IntentSenderRequest
import com.penguinstudio.safecrypt.models.AlbumModel
import com.penguinstudio.safecrypt.models.EncryptedModel
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.services.MediaDecryptionService
import com.penguinstudio.safecrypt.services.MediaEncryptionService
import com.penguinstudio.safecrypt.services.MediaFetchingService
import com.penguinstudio.safecrypt.services.NoDefaultDirFound
import com.penguinstudio.safecrypt.services.glide_service.IPicture
import com.penguinstudio.safecrypt.utilities.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val mediaFetchingService: MediaFetchingService,
    private val encryptionService: MediaEncryptionService,
    private val decryptionService: MediaDecryptionService
    ) {

    private suspend fun encryptionProcessHandler(media: List<IPicture>, mediaTo: MediaMode) : EncryptionResource {
        val successResponse = EncryptionResource.complete(null)
        media.forEach {
            val result = try {
                when(mediaTo) {
                    MediaMode.NORMAL_MEDIA -> {
                        decryptionService.decryptMedia(it)
                    }
                    MediaMode.ENCRYPTED_MEDIA -> {
                        encryptionService.encryptImage(it)
                    }
                }
                EncryptionResource.complete(null)
            }
            // Recoverable delete
            catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException = e as?
                            RecoverableSecurityException ?: throw RuntimeException(e.message, e)

                    val intentSender =
                        recoverableSecurityException.userAction.actionIntent.intentSender


                    val intentSenderRequest =
                        IntentSenderRequest.Builder(intentSender).build()

                    EncryptionResource.deleteRecoverable(intentSenderRequest)
                } else {
                    throw RuntimeException(e.message, e)
                }
            }
            // No default directory / Permissions
            catch (e: NoDefaultDirFound) {
                EncryptionResource.requestStorage()
            }
            // All other
            catch (e: Exception) {
                throw e
            }

            if(result.status != EncryptionStatus.OPERATION_COMPLETE) return result
        }
        return successResponse
    }

    suspend fun getMedia(): Resource<CollectionResponse<AlbumModel>> {
        return try {
            val result = mediaFetchingService.getAllVideosWithAlbums()
            Resource.success(CollectionResponse(result))

        } catch (ex: Exception) {
            Resource.error(ex.message.toString(), null)
        }
    }

    suspend fun getEncryptedMediaUris() : Resource<CollectionResponse<EncryptedModel>> {
        return try {
            val result = mediaFetchingService.getAllEncryptedMedia()
            Resource.success(CollectionResponse(result))
        }
        catch (ex: Exception) {
            Resource.error(ex.message.toString(), null)
        }
    }

    suspend fun encryptSelectedMedia(media: List<MediaModel>) :
            EncryptionResource {
        return encryptionProcessHandler(media, MediaMode.ENCRYPTED_MEDIA)
    }

    suspend fun decryptSelectedMedia(media: List<EncryptedModel>) :
            EncryptionResource {
        return encryptionProcessHandler(media, MediaMode.NORMAL_MEDIA)
    }
}