package com.penguinstudio.safecrypt.ui.home.encrypted

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penguinstudio.safecrypt.models.AlbumModel
import com.penguinstudio.safecrypt.models.EncryptedModel
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.repository.MediaRepository
import com.penguinstudio.safecrypt.services.MediaEncryptionService
import com.penguinstudio.safecrypt.services.glide_service.IPicture
import com.penguinstudio.safecrypt.ui.home.ISelectedMediaViewModel
import com.penguinstudio.safecrypt.ui.home.ISelectionViewModel
import com.penguinstudio.safecrypt.utilities.CollectionResponse
import com.penguinstudio.safecrypt.utilities.EncryptionResource
import com.penguinstudio.safecrypt.utilities.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject




@HiltViewModel
class EncryptedMediaViewModel @Inject constructor(
    private val mediaRepository: MediaRepository)
    : ViewModel(), ISelectionViewModel<EncryptedModel>, ISelectedMediaViewModel {

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


    override fun addMediaToSelection(media: EncryptedModel) {
        media.isSelected = true

        selectedItems.add(media)
    }

    override fun addAllMediaToSelection(media: MutableList<EncryptedModel>) {
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

    /**
     * Selected Media Implementation
     */
    private var _selectedMedia: EncryptedModel? = null
    override val allAlbumMedia: List<IPicture>
        get() {
            return encryptedFiles.value?.data?.collection?.toList()
                ?: emptyList()
        }
    override val selectedMedia: IPicture?
        get() {
            return _selectedMedia
        }

    override fun setSelectedMedia(selectedMedia: IPicture) {
        if(selectedMedia is EncryptedModel)
            this._selectedMedia = selectedMedia
    }

    override fun clearSelectedMedia() {
        _selectedMedia = null
    }

    /**
     * Decryption
     */

    private val _encryptionStatus = MutableLiveData<EncryptionResource?>()
    val encryptionStatus: LiveData<EncryptionResource?>
        get() {
            return _encryptionStatus
        }
    fun clearEncryptionStatus() {
        _encryptionStatus.postValue(null)
    }


    fun decryptSelectedMedia() {
        viewModelScope.launch {

            _encryptionStatus.postValue(EncryptionResource.loading())

            val selectedItems = selectedItems.toList()
            clearSelections()

            try {
                mediaRepository.decryptSelectedMedia(selectedItems).let {
                    _encryptionStatus.postValue(it)
                }
            }
            catch (ex: Exception) {
                _encryptionStatus.postValue(EncryptionResource.error("Unknown error occurred. Image wasn't encrypted."))

                Log.e("SafeCryptCritical", ex.message.toString())
            }
        }
    }
}