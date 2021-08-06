package com.penguinstudio.safecrypt.models

import android.net.Uri

class AlbumModel {
    var id: Long = 0
    var name: String? = null
    var coverUri: Uri? = null
    var albumMedia: ArrayList<MediaModel> = ArrayList()
}