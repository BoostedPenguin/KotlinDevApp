package com.penguinstudio.safecrypt.services

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.TextUtils
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.documentfile.provider.DocumentFile
import com.penguinstudio.safecrypt.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList

@Singleton
class MediaFetchingService @Inject constructor(
    @ApplicationContext private val context: Context
    ) {
    suspend fun getAllVideosWithAlbums(): ArrayList<AlbumModel> {
        return withContext(Dispatchers.IO) {

            val allAlbums: ArrayList<AlbumModel> = ArrayList()
            val albumsNames: ArrayList<String> = ArrayList()


            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATE_TAKEN,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DURATION,
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.WIDTH,
                MediaStore.Files.FileColumns.HEIGHT,
                MediaStore.Files.FileColumns.RELATIVE_PATH
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
                MediaStore.Files.FileColumns.DATE_TAKEN + " DESC" // Sort order.
            )

            query?.use { it ->
                val bucketNameColumn: Int = it.getColumnIndex(
                    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
                )
                val dateTakenColumn =
                    it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
                val dateAddedColumn =
                    it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val mediaNameColumn =
                    it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val mediaTypeColumn =
                    it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val durationColumn: Int =
                    it.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)

                val itemSizeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val itemWidthColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
                val itemHeightColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
                val relativePathColumn =
                    it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)


                while (it.moveToNext()) {
                    // Get the field values
                    val bucketName = it.getString(bucketNameColumn)
                    val imageId = it.getLong(idColumn)
                    val mediaName = it.getString(mediaNameColumn)

                    val dateAdded = it.getLongOrNull(dateAddedColumn)
                    val dateTaken =
                        it.getLongOrNull(dateTakenColumn) // Has 3 extra 0's, make sure to * 1000 for it

                    val size = it.getStringOrNull(itemSizeColumn)
                    val width = it.getStringOrNull(itemWidthColumn)
                    val height = it.getStringOrNull(itemHeightColumn)
                    val relativePath = it.getStringOrNull(relativePathColumn)


                    val contentUri = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                            ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                imageId
                            )
                        }
                        Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> {
                            ContentUris.withAppendedId(
                                MediaStore.Files.getContentUri("external"),
                                imageId
                            )
                        }
                        else -> throw Exception("Invalid build version")
                    }

                    var media: MediaModel

                    when (it.getInt(mediaTypeColumn)) {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> {

                            media = MediaModel(
                                imageId,
                                contentUri,
                                bucketName,
                                MediaType.IMAGE,
                                null,
                                mediaName,
                                MediaModelDetails(
                                    it.getStringOrNull(dateTakenColumn) ?: it.getLongOrNull(
                                        dateAddedColumn
                                    ).let { dateAddedValue ->
                                        if (dateAddedValue == null) return@let ""
                                        return@let dateAddedValue.toString()
                                    },
                                    relativePath,
                                    size,
                                    width,
                                    height
                                )
                            )
                        }
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                            media = MediaModel(
                                imageId,
                                contentUri,
                                bucketName,
                                MediaType.VIDEO,
                                it.getLong(durationColumn),
                                mediaName,
                                MediaModelDetails(
                                    it.getStringOrNull(dateTakenColumn) ?: it.getLongOrNull(
                                        dateAddedColumn
                                    ).let { dateAddedValue ->
                                        if (dateAddedValue == null) return@let ""
                                        return@let dateAddedValue.toString()
                                    },
                                    relativePath,
                                    size
                                )
                            )
                        }
                        else -> {
                            throw IllegalArgumentException("Something wrong happened.")
                        }
                    }

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
                        album.coverUri = media.uri
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

            root?.listFiles()?.forEach lit@ {

                // If it's not an encoded file continue


                val fileName = it.name ?: return@lit

                val startIndexOfExtension = fileName.indexOf(".${GCMEncryptionService.ENC_FILE_EXTENSION}")
                if(startIndexOfExtension <= 0)
                    return@lit

                val original = fileName.substring(0, startIndexOfExtension)

                if(!original.contains("."))
                    return@lit

                when {
                    isImageFile(original) -> {
                        uris.add(EncryptedModel(it.uri, fileName, MediaType.IMAGE, size = it.length().toString()))
                    }
                    isVideoFile(original) -> {
                        uris.add(EncryptedModel(it.uri, fileName, MediaType.VIDEO, size = it.length().toString()))
                    }
                    else -> return@lit
                }
            }
            return@withContext uris
        }
    }

    fun isImageFile(path: String?): Boolean {
        val mimeType: String = URLConnection.guessContentTypeFromName(path)
        return mimeType.startsWith("image")
    }

    fun isVideoFile(path: String?): Boolean {
        val mimeType: String = URLConnection.guessContentTypeFromName(path)
        return mimeType.startsWith("video")
    }
}