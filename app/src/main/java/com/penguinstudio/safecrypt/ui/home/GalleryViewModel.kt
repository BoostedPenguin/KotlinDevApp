package com.penguinstudio.safecrypt.ui.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penguinstudio.safecrypt.models.AlbumModel
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
}

@HiltViewModel
class GalleryViewModel @Inject constructor(private val mediaRepository: MediaRepository) :
    ViewModel(), IPicturesViewModel {

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
}