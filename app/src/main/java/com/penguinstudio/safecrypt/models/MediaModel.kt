package com.penguinstudio.safecrypt.models

import android.net.Uri
enum class MediaType {
    IMAGE, VIDEO
}
class MediaModel {
    var id = 0
    var albumName: String? = null
    var mediaUri: Uri? = null
    var mediaType: MediaType? = null
    var videoDuration: Long? = null

    var isSelected: Boolean = false
}