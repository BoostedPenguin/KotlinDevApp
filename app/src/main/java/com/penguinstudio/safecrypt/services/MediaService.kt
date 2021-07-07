package com.penguinstudio.safecrypt.services

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.penguinstudio.safecrypt.models.AlbumModel
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.models.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaService @Inject constructor() {

    @Deprecated("Returns only albums with images")
    suspend fun getPhoneAlbums(context: Context): ArrayList<AlbumModel> {
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
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                )
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)


                while (it.moveToNext()) {
                    // Get the field values
                    val bucketName = it.getString(bucketNameColumn)
                    val imageId = it.getLong(idColumn)


                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        imageId
                    )

                    // Adding a new PhonePhoto object to phonePhotos vector
                    val phonePhoto = MediaModel()
                    phonePhoto.albumName = bucketName
                    phonePhoto.mediaUri = contentUri
                    phonePhoto.id = Integer.valueOf(imageId.toInt())

                    //TODO If this causes performance issues defer it to on album click

                    if (albumsNames.contains(bucketName)) {
                        for (album in phoneAlbums) {
                            if (album.name == bucketName) {
                                album.albumMedia.add(phonePhoto)
                                break
                            }
                        }
                    } else {
                        val album = AlbumModel()

                        album.id = phonePhoto.id
                        album.name = bucketName
                        album.coverUri = phonePhoto.mediaUri
                        album.albumMedia.add(phonePhoto)

                        phoneAlbums.add(album)
                        albumsNames.add(bucketName)
                    }
                }
            }
            return@withContext phoneAlbums
        }
    }

    suspend fun getAllVideosWithAlbums(context: Context): ArrayList<AlbumModel> {
        return withContext(Dispatchers.IO) {

            val allAlbums: ArrayList<AlbumModel> = ArrayList()
            val albumsNames: ArrayList<String> = ArrayList()

            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.DURATION,
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
            )
            val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

            val queryUri = MediaStore.Files.getContentUri("external")

            // Make the query.
            val query = context.contentResolver.query(
                queryUri,
                projection,  // Which columns to return
                selection,  // Which rows to return (all rows)
                null,  // Selection arguments (none)
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC" // Sort order.
            )

            query?.use {
                val bucketNameColumn: Int = it.getColumnIndex(
                    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
                )
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val mediaType = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val durationColumn: Int =
                    it.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)


                while (it.moveToNext()) {
                    // Get the field values
                    val bucketName = it.getString(bucketNameColumn)
                    val imageId = it.getLong(idColumn)



                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Files.getContentUri("external"),
                        imageId
                    )

                    // Adding a new PhonePhoto object to phonePhotos vector
                    val phonePhoto = MediaModel()
                    phonePhoto.albumName = bucketName
                    phonePhoto.mediaUri = contentUri
                    phonePhoto.id = Integer.valueOf(imageId.toInt())

                    when(it.getInt(mediaType)) {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> {
                            phonePhoto.mediaType = MediaType.IMAGE
                        }
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                            phonePhoto.mediaType = MediaType.VIDEO
                            phonePhoto.videoDuration = it.getLong(durationColumn)
                        }
                    }

                    //TODO If this causes performance issues defer it to on album click

                    if (albumsNames.contains(bucketName)) {
                        for (album in allAlbums) {
                            if (album.name == bucketName) {
                                album.albumMedia.add(phonePhoto)
                                break
                            }
                        }
                    } else {
                        val album = AlbumModel()

                        album.id = phonePhoto.id
                        album.name = bucketName
                        album.coverUri = phonePhoto.mediaUri
                        album.albumMedia.add(phonePhoto)

                        allAlbums.add(album)
                        albumsNames.add(bucketName)
                    }
                }
            }
            return@withContext allAlbums
        }
    }
}