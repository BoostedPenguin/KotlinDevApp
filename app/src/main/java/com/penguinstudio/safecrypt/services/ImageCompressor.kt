package com.penguinstudio.safecrypt.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Singleton

class ImageCompressor @Inject constructor(@ApplicationContext private val context: Context) {


    /**
     * Get pictures through uri and compress them
     *
     * @param uri
     */
    fun getBitmapFormUri(uri: Uri): Bitmap? {
        var input = context.contentResolver.openInputStream(uri)
        val onlyBoundsOptions = BitmapFactory.Options()
        onlyBoundsOptions.inJustDecodeBounds = true
        onlyBoundsOptions.inDither = true //optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions)
        input?.close()
        val originalWidth = onlyBoundsOptions.outWidth
        val originalHeight = onlyBoundsOptions.outHeight
        if (originalWidth == -1 || originalHeight == -1)
            throw IllegalArgumentException("Error trying to decode width / height of image")
        //Image resolution is based on 480x800
        val hh = 1600f //The height is set as 800f here
        val ww = 960f //Set the width here to 480f
        //Zoom ratio. Because it is a fixed scale, only one data of height or width is used for calculation
        var be = 1 //be=1 means no scaling
        if (originalWidth > originalHeight && originalWidth > ww) { //If the width is large, scale according to the fixed size of the width
            be = (originalWidth / ww).toInt()
        } else if (originalWidth < originalHeight && originalHeight > hh) { //If the height is high, scale according to the fixed size of the width
            be = (originalHeight / hh).toInt()
        }
        if (be <= 0) be = 1
        //Proportional compression
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inSampleSize = be //Set scaling
        bitmapOptions.inDither = true //optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //optional
        input = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions)
        input?.close()
        return bitmap?.let { compressImage(it) } //Mass compression again
    }

    /**
     * Mass compression method
     *
     * @param image
     * @return
     */
    private fun compressImage(image: Bitmap): Bitmap? {
        val baos = ByteArrayOutputStream()
        image.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            baos
        ) //Quality compression method, here 100 means no compression, store the compressed data in the BIOS
        var options = 100
        while (baos.toByteArray().size / 1024 > 10000) {  //Cycle to determine if the compressed image is greater than 100kb, greater than continue compression
            baos.reset() //Reset the BIOS to clear it
            //First parameter: picture format, second parameter: picture quality, 100 is the highest, 0 is the worst, third parameter: save the compressed data stream
            image.compress(
                Bitmap.CompressFormat.JPEG,
                options,
                baos
            ) //Here, the compression options are used to store the compressed data in the BIOS
            options -= 10 //10 less each time
        }

        val bs = ByteArrayInputStream(baos.toByteArray()) //Store the compressed data in ByteArrayInputStream
        baos.close()
        return BitmapFactory.decodeStream(
            bs,
            null,
            null
        ) //Generate image from ByteArrayInputStream data

    }

    /**
     * Rotate the picture at an angle
     *
     * @param bm     Pictures to rotate
     * @param degree Rotation angle
     * @return Rotated picture
     */
    fun rotateBitmapByDegree(bm: Bitmap, degree: Float): ByteArrayInputStream {
        var returnBm: Bitmap? = null

        // Generate rotation matrix according to rotation angle
        val matrix = Matrix()
        matrix.postRotate(degree)
        try {
            // Rotate the original image according to the rotation matrix and get a new image
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, matrix, true)
        } catch (e: OutOfMemoryError) {
        }
        if (returnBm == null) {
            returnBm = bm
        }
        if (bm != returnBm) {
            bm.recycle()
        }

        val baos = ByteArrayOutputStream()
        returnBm.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            baos
        )
        val returnValue = ByteArrayInputStream(baos.toByteArray())
        baos.close()
        return returnValue
    }

    /**
     * Read the rotation angle of the picture
     *
     * @return Rotation angle of picture
     */
    fun getBitmapDegree(uri: Uri): Int {
        var degree = 0
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Error opening input stream")
            // Read the picture from the specified path and obtain its EXIF information
            val exifInterface = ExifInterface(inputStream)
            // Get rotation information for pictures
            val orientation: Int = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
            }
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return degree
    }
}