package com.penguinstudio.safecrypt.ui.home.encrypted

import androidx.lifecycle.ViewModel
import com.penguinstudio.safecrypt.repository.MediaRepository
import com.penguinstudio.safecrypt.services.MediaEncryptionService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject




@HiltViewModel
class EncryptedMediaViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,

    private val mediaEncryptionService: MediaEncryptionService)
    : ViewModel()  {

}