package com.penguinstudio.safecrypt.ui.home

import androidx.lifecycle.ViewModel
import com.penguinstudio.safecrypt.repository.MediaRepository
import javax.inject.Inject

class EncryptedMediaViewModel @Inject constructor(private val mediaRepository: MediaRepository) : ViewModel() {

    fun getAllEncryptedMedia() {

    }
}