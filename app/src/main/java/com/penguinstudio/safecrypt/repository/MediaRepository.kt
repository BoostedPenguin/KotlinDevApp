package com.penguinstudio.safecrypt.repository

import android.app.RecoverableSecurityException
import android.content.Context
import android.os.Build
import androidx.activity.result.IntentSenderRequest
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.services.MediaEncryptionService
import com.penguinstudio.safecrypt.services.MediaService
import com.penguinstudio.safecrypt.services.NoDefaultDirFound
import com.penguinstudio.safecrypt.utilities.EncryptionResource
import com.penguinstudio.safecrypt.utilities.MediaResponse
import com.penguinstudio.safecrypt.utilities.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val mediaService: MediaService,
    private val encryptionService: MediaEncryptionService,
    ) {

    suspend fun getMedia(): Resource<MediaResponse> {
        return try {
            val result = mediaService.getAllVideosWithAlbums()
            Resource.success(MediaResponse(result))

        } catch (ex: Exception) {
            Resource.error(ex.message.toString(), null)
        }
    }

    suspend fun encryptMedia(position: Int, media: MediaModel) :
            EncryptionResource {

        return try {
            encryptionService.encryptImage(media)

            EncryptionResource.complete(position)
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
    }

    //TODO
    suspend fun encryptAllMedia(mediaList: List<MediaModel>) : EncryptionResource {

        mediaList.forEach {
            try {
                encryptionService.encryptImage(it)
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

                    return EncryptionResource.deleteRecoverable(intentSenderRequest)
                } else {
                    throw RuntimeException(e.message, e)
                }
            }
            // No default directory / Permissions
            catch (e: NoDefaultDirFound) {
                return EncryptionResource.requestStorage()
            }
            // All other
            catch (e: Exception) {
                throw e
            }
        }

        return EncryptionResource.complete(0)
    }
}