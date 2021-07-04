package com.penguinstudio.safecrypt.ui.home

import androidx.lifecycle.ViewModel
import com.penguinstudio.safecrypt.adapters.GalleryType

class GalleryViewModel : ViewModel() {

    private var gType: GalleryType = GalleryType.NORMAL
    val galleryType: GalleryType
        get() {
            return gType
        }

    fun setGalleryType(galleryType: GalleryType) {
        this.gType = galleryType
    }
}