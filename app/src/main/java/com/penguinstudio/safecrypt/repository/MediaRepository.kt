package com.penguinstudio.safecrypt.repository

import android.content.Context
import android.util.Log
import com.penguinstudio.safecrypt.services.MediaService
import com.penguinstudio.safecrypt.utilities.MediaResponse
import com.penguinstudio.safecrypt.utilities.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(private val mediaService: MediaService) {

    suspend fun getMedia(context: Context): Resource<MediaResponse> {
        return try {
            val result = mediaService.getAllVideosWithAlbums(context)
            Resource.success(MediaResponse(result))

        } catch (ex: Exception) {
            Resource.error(ex.message.toString(), null)
        }
    }
}