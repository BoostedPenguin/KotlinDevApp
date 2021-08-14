package com.penguinstudio.safecrypt.models

import android.net.Uri
enum class MediaType {
    IMAGE, VIDEO
}
data class MediaModel(
    var id: Long,
    val mediaUri: Uri,
    var albumName: String?,
    var mediaType: MediaType,
    var videoDuration: Long?,
    var mediaName: String,

    val details: MediaModelDetails,

    var isSelected: Boolean = false,
)

data class MediaModelDetails(
    val dateAdded: Long? = null,
    val relativePath: String? = null,
    val size: String? = null,
    val width: String? = null,
    val height: String? = null,
)