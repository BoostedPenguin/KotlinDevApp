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
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaService @Inject constructor() {
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
                MediaStore.Files.FileColumns.DISPLAY_NAME,
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
                var mediaName = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val mediaType = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val durationColumn: Int =
                    it.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)


                while (it.moveToNext()) {
                    // Get the field values
                    val bucketName = it.getString(bucketNameColumn)
                    val imageId = it.getLong(idColumn)
                    val mediaName = it.getString(mediaName)



                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Files.getContentUri("external"),
                        imageId
                    )


                    var media: MediaModel

                    when(it.getInt(mediaType)) {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> {
                            media = MediaModel(
                                Integer.valueOf(imageId.toInt()),
                                contentUri,
                                bucketName,
                                MediaType.IMAGE,
                                null,
                                mediaName,
                                )
                        }
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                            media = MediaModel(
                                Integer.valueOf(imageId.toInt()),
                                contentUri,
                                bucketName,
                                MediaType.VIDEO,
                                it.getLong(durationColumn),
                                mediaName,
                            )
                        }
                        else -> {
                            throw IllegalArgumentException("Something wrong happened.")
                        }
                    }

                    //TODO If this causes performance issues defer it to on album click

                    if (albumsNames.contains(bucketName)) {
                        for (album in allAlbums) {
                            if (album.name == bucketName) {
                                album.albumMedia.add(media)
                                break
                            }
                        }
                    } else {
                        val album = AlbumModel()

                        album.id = media.id
                        album.name = bucketName
                        album.coverUri = media.mediaUri
                        album.albumMedia.add(media)

                        allAlbums.add(album)
                        albumsNames.add(bucketName)
                    }
                }
            }
            return@withContext allAlbums
        }
    }
}