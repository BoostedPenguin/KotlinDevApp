package com.penguinstudio.safecrypt.models

import android.net.Uri
import com.penguinstudio.safecrypt.services.glide_service.IPicture

data class EncryptedModel(override val uri: Uri,
                          override val mediaName: String,
                          override val size: String?,
                          override var mediaType: MediaType = MediaType.IMAGE,
                          override var isSelected: Boolean = false,
                          ) : IPicture