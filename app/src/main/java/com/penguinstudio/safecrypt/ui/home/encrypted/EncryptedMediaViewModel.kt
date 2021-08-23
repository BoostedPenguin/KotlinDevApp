package com.penguinstudio.safecrypt.ui.home.encrypted

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penguinstudio.safecrypt.models.EncryptedModel
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.repository.MediaRepository
import com.penguinstudio.safecrypt.services.MediaEncryptionService
import com.penguinstudio.safecrypt.ui.home.ISelectionViewModel
import com.penguinstudio.safecrypt.utilities.CollectionResponse
import com.penguinstudio.safecrypt.utilities.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject




@HiltViewModel
class EncryptedMediaViewModel @Inject constructor(
    private val mediaRepository: MediaRepository)
    : ViewModel(), ISelectionViewModel<EncryptedModel> {

    private val _encryptedFiles = MutableLiveData<Resource<CollectionResponse<EncryptedModel>>>()
    val encryptedFiles: LiveData<Resource<CollectionResponse<EncryptedModel>>> = _encryptedFiles

    fun getEncryptedFiles() {
        _encryptedFiles.postValue(Resource.loading(null))

        viewModelScope.launch {
            mediaRepository.getEncryptedMediaUris().let {
                _encryptedFiles.postValue(it)
            }
        }
    }

    override val itemSelectionMode = MutableLiveData<Boolean>()

    override fun clearSelections() {
        selectedItems.clear()
    }

    override val selectedItems: MutableList<EncryptedModel> = mutableListOf()


    override fun addMediaToSelection(position: Int, media: EncryptedModel) {
        media.isSelected = true

        selectedItems.add(media)
    }

    override fun addAllMediaToSelection(media: ArrayList<EncryptedModel>) {
        selectedItems.let {
            for(obj in media) {
                if(!it.contains(obj)) {
                    it.add(obj)
                    obj.isSelected = true
                }
            }
        }
    }

    override fun removeMediaFromSelection(position: Int, media: EncryptedModel) {
        selectedItems.removeAll {
            it == media
        }
        media.isSelected = false
    }
}