package com.penguinstudio.safecrypt.utilities

import androidx.activity.result.IntentSenderRequest

data class EncryptionResource(
    val status: EncryptionStatus,
    val intentSender: IntentSenderRequest? = null,
    val position: Int? = null
){
    companion object{

        fun complete(position: Int?): EncryptionResource{
            return EncryptionResource(EncryptionStatus.OPERATION_COMPLETE, null, position)
        }

        fun deleteRecoverable(intent: IntentSenderRequest?): EncryptionResource{
            return EncryptionResource(EncryptionStatus.DELETE_RECOVERABLE, intent)
        }

        fun requestStorage(): EncryptionResource{
            return EncryptionResource(EncryptionStatus.REQUEST_STORAGE, null)
        }

        fun loading(): EncryptionResource {
            return EncryptionResource(EncryptionStatus.LOADING)
        }
    }
}