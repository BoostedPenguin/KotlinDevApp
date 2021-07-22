package com.penguinstudio.safecrypt.services

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.documentfile.provider.DocumentFile
import com.penguinstudio.safecrypt.models.AlbumModel
import com.penguinstudio.safecrypt.models.EncryptedModel
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.models.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaService @Inject constructor(
    @ApplicationContext private val context: Context
    ) {
    suspend fun getAllVideosWithAlbums(): ArrayList<AlbumModel> {
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
                val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val mediaNameColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val mediaType = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val durationColumn: Int =
                    it.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)


                while (it.moveToNext()) {
                    // Get the field values
                    val bucketName = it.getString(bucketNameColumn)
                    val imageId = it.getLong(idColumn)
                    val mediaName = it.getString(mediaNameColumn)
                    val dateAdded = it.getLongOrNull(dateAddedColumn)

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
                                dateAdded
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
                                dateAdded
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

    suspend fun getAllEncryptedMedia() : ArrayList<EncryptedModel> = withContext(Dispatchers.IO) {
        val sp: SharedPreferences = context.getSharedPreferences("DirPermission", Context.MODE_PRIVATE)
        val uriTree = sp.getString("uriTree", "")

        if (TextUtils.isEmpty(uriTree)) {
            return@withContext ArrayList()
        } else {
            val uri: Uri = Uri.parse(uriTree)

            // Root directory > where to look for encrypted files
            val root = DocumentFile.fromTreeUri(context, uri)

            val uris = arrayListOf<EncryptedModel>()

            root?.listFiles()?.forEach {
                uris.add(EncryptedModel(it.uri))
            }
            return@withContext uris
        }
    }
}