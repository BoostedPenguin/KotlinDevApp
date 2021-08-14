package com.penguinstudio.safecrypt.models

import android.net.Uri
import com.penguinstudio.safecrypt.services.glide_service.IPicture

data class EncryptedModel(override val uri: Uri, var isSelected: Boolean = false) : IPicture