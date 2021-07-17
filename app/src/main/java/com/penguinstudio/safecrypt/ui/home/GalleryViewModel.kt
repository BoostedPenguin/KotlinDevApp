package com.penguinstudio.safecrypt.ui.home

import androidx.lifecycle.*
import com.penguinstudio.safecrypt.models.AlbumModel
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.repository.MediaRepository
import com.penguinstudio.safecrypt.services.MediaEncryptionService
import com.penguinstudio.safecrypt.utilities.EncryptionResource
import com.penguinstudio.safecrypt.utilities.MediaResponse
import com.penguinstudio.safecrypt.utilities.Resource
import com.penguinstudio.safecrypt.utilities.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

interface IPicturesViewModel {
    fun getMedia()

    val albums: LiveData<Resource<MediaResponse>>

    val selectedAlbum: LiveData<Resource<AlbumModel>>

    fun setSelectedAlbum(selectedItem: AlbumModel)

    fun clearSelectedAlbum()

    var itemSelectionMode: Boolean

    fun clearSelections()

    val selectedItems: MutableLiveData<MutableList<MediaModel>>

    fun addMediaToSelection(position: Int, media: MediaModel)

    fun addAllMediaToSelection(media: ArrayList<MediaModel>)

    fun removeMediaFromSelection(position: Int, media: MediaModel)

    fun setSelectedMedia(selectedMedia: MediaModel)

    fun encryptSelectedMedia()

    val encryptionStatus: LiveData<EncryptionResource?>

    fun clearEncryptionStatus()
}

interface ISelectedMediaViewModel {
    val selectedMedia: MediaModel?

    fun setSelectedMedia(selectedMedia: MediaModel)

    fun clearSelectedMedia()
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val mediaEncryptionService: MediaEncryptionService
) :
    ViewModel(), IPicturesViewModel, ISelectedMediaViewModel {

    /**
     * Get media from Phone media folders
     */
    private val _albums = MutableLiveData<Resource<MediaResponse>>()
    override val albums: LiveData<Resource<MediaResponse>> = _albums


    override fun getMedia() {
        viewModelScope.launch {

            // Use same value or null while loading to prevent observers from catching nulls
            _albums.postValue(Resource.loading(_albums.value?.data))

            mediaRepository.getMedia().let {
                _albums.postValue(it)
            }
        }
    }

    /**
     * Encryption
     */
    private val _encryptionStatus = MutableLiveData<EncryptionResource?>()
    override val encryptionStatus: LiveData<EncryptionResource?>
        get() {
            return _encryptionStatus
        }
    override fun clearEncryptionStatus() {
        _encryptionStatus.postValue(null)
    }


    override fun encryptSelectedMedia() {
        viewModelScope.launch {

            val content = selectedItems.value ?: return@launch
            _encryptionStatus.postValue(EncryptionResource.loading())

            mediaRepository.encryptSelectedMedia(content.toList()).let {
                _encryptionStatus.postValue(it)
            }
        }
    }

    /**
     * Pictures fragment data
     */
    override val selectedAlbum: LiveData<Resource<AlbumModel>> = Transformations.map(_albums) {
        _selectedAlbumName ?: return@map null

        return@map when(_albums.value?.status) {
            Status.SUCCESS -> {
                val result = it.data?.media?.find { album ->
                    album.name == _selectedAlbumName
                }
                Resource.success(result)
            }
            Status.LOADING -> {
                Resource.loading(null)
            }
            null, Status.ERROR -> {
                Resource.error("An unknown error occurred", null)
            }
        }
    }

    private var _selectedAlbumName: String? = null

    override fun setSelectedAlbum(selectedItem: AlbumModel) {
        _selectedAlbumName = selectedItem.name
    }

    override fun clearSelectedAlbum() {
        _selectedAlbumName = null
    }

    override var itemSelectionMode = false
    override val selectedItems: MutableLiveData<MutableList<MediaModel>> = MutableLiveData()


    override fun clearSelections() {
        selectedItems.value?.clear()
    }

    override fun addMediaToSelection(position: Int, media: MediaModel) {
        media.selectedPosition = position

        if(selectedItems.value == null) {
            selectedItems.value = mutableListOf(media)
        }
        else {
            selectedItems.value?.add(media)
            selectedItems.notifyObserver()
        }
    }

    override fun addAllMediaToSelection(media: ArrayList<MediaModel>) {
        if(selectedItems.value == null) {
            selectedItems.value = media
        }
        else {
            selectedItems.value!!.let {
                for(obj in media) {
                    if(!it.contains(obj)) {
                        it.add(obj)
                        obj.isSelected = true
                    }
                }
            }

            selectedItems.notifyObserver()
        }
    }

    override fun removeMediaFromSelection(position: Int, media: MediaModel) {
        selectedItems.value!!.removeAll {
            it.id == media.id
        }
        selectedItems.notifyObserver()
    }

    /**
     * Selected Media Implementation
     */
    private var _selectedMedia: MediaModel? = null
    override val selectedMedia: MediaModel?
        get() {
            return _selectedMedia
        }

    override fun setSelectedMedia(selectedMedia: MediaModel) {
        this._selectedMedia = selectedMedia
    }

    override fun clearSelectedMedia() {
        _selectedMedia = null
    }


    companion object {
        /**
         * Re-assigns value to itself to trigger observers
         */
        fun <T> MutableLiveData<T>.notifyObserver() {
            this.value = this.value
        }
    }
}