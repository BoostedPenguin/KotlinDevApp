package com.penguinstudio.safecrypt.models

import android.net.Uri
enum class MediaType {
    IMAGE, VIDEO
}
data class MediaModel(
    var id: Int,
    val mediaUri: Uri,
    var albumName: String?,
    var mediaType: MediaType?,
    var videoDuration: Long?,
    var mediaName: String,
    var isSelected: Boolean = false,

    var selectedPosition: Int? = null,
)