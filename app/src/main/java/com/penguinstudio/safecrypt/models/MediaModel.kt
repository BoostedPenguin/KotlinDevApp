package com.penguinstudio.safecrypt.models

import android.net.Uri
import com.penguinstudio.safecrypt.services.glide_service.IPicture

enum class MediaType {
    IMAGE, VIDEO
}
data class MediaModel(
    var id: Long,
    override val uri: Uri,
    var albumName: String?,
    override var mediaType: MediaType,
    var videoDuration: Long?,
    var mediaName: String,

    val details: MediaModelDetails,

    var isSelected: Boolean = false,
) : IPicture

data class MediaModelDetails(
    val dateAdded: Long? = null,
    val relativePath: String? = null,
    val size: String? = null,
    val width: String? = null,
    val height: String? = null,
)