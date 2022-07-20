package com.penguinstudio.safecrypt.services

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.StorageConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class InnerVideoCompressor @Inject constructor() {

    fun notFun(uri: Uri, context: Context) {

        val content = listOf(uri)
        VideoCompressor.start(
            context = context, // => This is required
            uris = content, // => Source can be provided as content uris
            isStreamable = true,
            storageConfiguration = StorageConfiguration(
                saveAt = Environment.DIRECTORY_MOVIES, // => the directory to save the compressed video(s). Will be ignored if isExternal = false.
                isExternal = true, // => false means save at app-specific file directory. Default is true.
                fileName = "output-video.mp4" // => an optional value for a custom video name.
            ),
            configureWith = Configuration(
                quality = VideoQuality.LOW,
                isMinBitrateCheckEnabled = true,
//                videoBitrate = 3677198, /*Int, ignore, or null*/
                disableAudio = false, /*Boolean, or ignore*/
                keepOriginalResolution = false, /*Boolean, or ignore*/
//                videoWidth = 360.0, /*Double, ignore, or null*/
//                videoHeight = 480.0 /*Double, ignore, or null*/
            ),
            listener = object : CompressionListener {
                override fun onProgress(index: Int, percent: Float) {
                    // Update UI with progress value
//                    runOnUiThread {
//                    }
                }

                override fun onStart(index: Int) {
                    // Compression start
                }

                override fun onSuccess(index: Int, size: Long, path: String?) {
                    // On Compression success
                    Log.e("ma", path.toString())
                }

                override fun onFailure(index: Int, failureMessage: String) {
                    // On Failure
                }

                override fun onCancelled(index: Int) {
                    // On Cancelled
                }

            }
        )
    }
}