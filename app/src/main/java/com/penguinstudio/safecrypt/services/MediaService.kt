package com.penguinstudio.safecrypt.services

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.penguinstudio.safecrypt.models.AlbumModel
import com.penguinstudio.safecrypt.models.PhotoModel
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaService @Inject constructor() {
    suspend fun getPhoneAlbums(context: Context) : ArrayList<AlbumModel> {
        return withContext(Dispatchers.IO) {

            // Creating arrays to hold the final albums objects and albums names
            val phoneAlbums: ArrayList<AlbumModel> = ArrayList()
            val albumsNames: ArrayList<String> = ArrayList()

            // which image properties are we querying
            val projection = arrayOf(
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media._ID,
            )

            // External storage selector
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            // Make the query.
            val query = context.contentResolver.query(
                imageCollection,
                projection,  // Which columns to return
                null,  // Which rows to return (all rows)
                null,  // Selection arguments (none)
                null // Ordering
            )

            query?.use {
                val bucketNameColumn: Int = it.getColumnIndex(
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                )
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)


                while(it.moveToNext()) {
                    // Get the field values
                    val bucketName = it.getString(bucketNameColumn)
                    val imageId = it.getLong(idColumn)


                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        imageId
                    )

                    // Adding a new PhonePhoto object to phonePhotos vector
                    val phonePhoto = PhotoModel()
                    phonePhoto.albumName = bucketName
                    phonePhoto.photoUri = contentUri
                    phonePhoto.id = Integer.valueOf(imageId.toInt())

                    //TODO If this causes performance issues defer it to on album click

                    if (albumsNames.contains(bucketName)) {
                        for (album in phoneAlbums) {
                            if (album.name == bucketName) {
                                album.albumPhotos.add(phonePhoto)
                                break
                            }
                        }
                    } else {
                        val album = AlbumModel()

                        album.id = phonePhoto.id
                        album.name = bucketName
                        album.coverUri = phonePhoto.photoUri
                        album.albumPhotos.add(phonePhoto)

                        phoneAlbums.add(album)
                        albumsNames.add(bucketName)
                    }
                }
            }
            Thread.sleep(5000)
            return@withContext phoneAlbums
        }
    }
}