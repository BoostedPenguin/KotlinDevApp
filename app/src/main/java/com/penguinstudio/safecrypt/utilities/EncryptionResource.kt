package com.penguinstudio.safecrypt.utilities

import android.net.Uri
import androidx.activity.result.IntentSenderRequest

data class EncryptionResource(
    val status: EncryptionStatus,
    val mediaUris: List<Uri>? = null,
    val intentSender: IntentSenderRequest? = null,
    val positions: MutableList<Int>? = null
){
    companion object{

        fun complete(positions: MutableList<Int>?, mediaUris: List<Uri>?): EncryptionResource{
            return EncryptionResource(EncryptionStatus.OPERATION_COMPLETE, mediaUris, null, positions)
        }

        fun deleteRecoverable(intent: IntentSenderRequest?): EncryptionResource{
            return EncryptionResource(EncryptionStatus.DELETE_RECOVERABLE, null, intent)
        }

        fun requestStorage(): EncryptionResource{
            return EncryptionResource(EncryptionStatus.REQUEST_STORAGE)
        }

        fun loading(): EncryptionResource {
            return EncryptionResource(EncryptionStatus.LOADING)
        }
    }
}