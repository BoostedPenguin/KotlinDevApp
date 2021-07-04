package com.penguinstudio.safecrypt.models

import android.net.Uri

class AlbumModel {
    var id = 0
    var name: String? = null
    var coverUri: Uri? = null
    var coverPhotoBytes: ByteArray? = null
    var albumPhotos: ArrayList<PhotoModel> = ArrayList()
}