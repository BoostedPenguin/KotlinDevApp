package com.penguinstudio.safecrypt.ui.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penguinstudio.safecrypt.models.AlbumModel
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.repository.MediaRepository
import com.penguinstudio.safecrypt.utilities.GalleryType
import com.penguinstudio.safecrypt.utilities.MediaResponse
import com.penguinstudio.safecrypt.utilities.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

interface IPicturesViewModel {
    val selectedAlbum: AlbumModel?

    fun setSelectedAlbum(selectedAlbum: AlbumModel)

    fun clearSelectedAlbum()

    var itemSelectionMode: Boolean

    fun clearSelections()

    val selectedItems: MutableLiveData<MutableList<MediaModel>>

    fun addMediaToSelection(position: Int, media: MediaModel)

    fun addAllMediaToSelection(media: ArrayList<MediaModel>)

    fun removeMediaFromSelection(position: Int, media: MediaModel)

    fun setSelectedMedia(selectedMedia: MediaModel)

}

interface ISelectedMediaViewModel {
    val selectedMedia: MediaModel?

    fun setSelectedMedia(selectedMedia: MediaModel)

    fun clearSelectedMedia()
}

@HiltViewModel
class GalleryViewModel @Inject constructor(private val mediaRepository: MediaRepository) :
    ViewModel(), IPicturesViewModel, ISelectedMediaViewModel {

    private val _albums = MutableLiveData<Resource<MediaResponse>>()
    val albums: LiveData<Resource<MediaResponse>> = _albums


    /**
     * Gallery type handler TODO Will create a new fragment and viewmodel for encrypted
     */
    private var gType: GalleryType = GalleryType.NORMAL
    val galleryType: GalleryType
        get() {
            return gType
        }


    fun setGalleryType(galleryType: GalleryType, context: Context) {
        this.gType = galleryType
        getMedia(context)
    }


    fun getMedia(context: Context) {
        viewModelScope.launch {

            _albums.postValue(Resource.loading(null))

            mediaRepository.getMedia(context).let {
                _albums.postValue(it)
            }
        }
    }


    /**
     * Pictures fragment data
     */

    private var _selectedAlbum: AlbumModel? = null
    override val selectedAlbum: AlbumModel?
        get() {
            return _selectedAlbum
        }

    override fun setSelectedAlbum(selectedAlbum: AlbumModel) {
        _selectedAlbum = selectedAlbum
    }

    override fun clearSelectedAlbum() {
        _selectedAlbum = null
    }

    override var itemSelectionMode = false
    override val selectedItems: MutableLiveData<MutableList<MediaModel>> = MutableLiveData()


    override fun clearSelections() {
        selectedItems.value?.clear()
        selectedItems.notifyObserver()
    }

    override fun addMediaToSelection(position: Int, media: MediaModel) {
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