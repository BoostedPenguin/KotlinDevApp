package com.penguinstudio.safecrypt.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.Key.CHARSET
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.FileLoader
import com.bumptech.glide.load.model.FileLoader.FileOpener
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.penguinstudio.safecrypt.services.EncryptionService
import java.io.*
import java.security.MessageDigest
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.GCMParameterSpec


/**
 * Glide 4.x custom processing file processing for Picture and File types, GlideModel
 */

//interface IPicture {
//    fun getFileName(): String?
//}
//
//@GlideModule
//class MyGlideModule : AppGlideModule() {
//
//    override fun applyOptions(context: Context, builder: GlideBuilder) {
//        val options = RequestOptions().format(DecodeFormat.PREFER_RGB_565)
//        builder.setDefaultRequestOptions(options)
//    }
//
//    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
//        registry.append(IPicture::class.java, InputStream::class.java, LoaderFactory())
//
//        registry.append(
//            File::class.java, InputStream::class.java,
//            FileLoader.Factory(object : FileOpener<InputStream> {
//
//                @Throws(FileNotFoundException::class)
//                override fun open(file: File): InputStream {
//                    // You can do file processing here, such as decryption.
//
//                    return TheFuck.getCipherInputStream(FileInputStream(file))
//                }
//
//                @Throws(IOException::class)
//                override fun close(inputStream: InputStream) {
//                    inputStream.close()
//                }
//
//                override fun getDataClass(): Class<InputStream> {
//                    return InputStream::class.java
//                }
//            })
//        )
//    }
//
//    override fun isManifestParsingEnabled(): Boolean {
//        return false
//    }
//
//    /**
//     * Open the list resolution
//     *
//     * Not open here, avoid adding the same modules twice
//     *
//     * @return
//     */
//}
//
//class TheFuck {
//    companion object {
//        private const val AndroidKeyStore = "AndroidKeyStore"
//        private const val AES_MODE = "AES/GCM/NoPadding"
//        private const val GCM_IV_LENGTH = 12
//        private const val KEY_ALIAS = "MyKey"
//        const val ENC_FILE_EXTENSION = "enc"
//
//        fun getCipherInputStream(inputStream: InputStream): CipherInputStream {
//            // Configure cipher crypt
//
//            val decryptCipher = Cipher.getInstance(AES_MODE)
//
//            val iv = ByteArray(12)
//
//            // Read the IV and use it for init
//            inputStream.read(iv, 0, iv.size)
//
//            val gcmIv: AlgorithmParameterSpec = GCMParameterSpec(128, iv)
//
//            decryptCipher.init(Cipher.DECRYPT_MODE, EncryptionService.getSecretKey(), gcmIv)
//
//
//            return CipherInputStream(inputStream, decryptCipher)
//        }
//    }
//}
//
//class MyModelLoader : ModelLoader<IPicture, InputStream> {
//    override fun buildLoadData(
//        model: IPicture,
//        width: Int,
//        height: Int,
//        options: Options
//    ): LoadData<InputStream> {
//
//        return LoadData(MyKey(model), MyDataFetcher(model))
//    }
//
//    override fun handles(model: IPicture): Boolean {
//        return true
//    }
//
//
//}
//class MyKey constructor(private val path: IPicture) : Key {
//
//    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
//        //messageDigest.update(path.getFileName().getBytes(CHARSET));
//    }
//}
//
//class MyDataFetcher constructor(private var file: IPicture) : DataFetcher<InputStream> {
//
//    private var isCanceled: Boolean = false
//    var mInputStream: InputStream? = null
//
//    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
//
//        // You can do some file processing here, such as file path processing, file decryption, etc.
//        try {
//            if (!isCanceled) {
//                val g = FileInputStream(file.getFileName())
//
//                mInputStream = TheFuck.getCipherInputStream(g)
//            }
//        } catch (e: FileNotFoundException) {
//            callback.onLoadFailed(e);
//            Log.e("err", e.message.toString());
//        }
//        callback.onDataReady(mInputStream);
//
//    }
//
//    override fun cleanup() {
//
//        if (mInputStream != null) {
//            try {
//                mInputStream!!.close();
//            } catch (e: IOException) {
//                Log.e("err", e.message.toString());
//            }
//        }
//    }
//
//    override fun cancel() {
//        isCanceled = true;
//    }
//
//    override fun getDataClass(): Class<InputStream> {
//        return InputStream::class.java
//    }
//
//
//    override fun getDataSource(): DataSource {
//        //return LOCAL;
//        //return REMOTE;
//        return DataSource.LOCAL
//        //return DATA_DISK_CACHE;
//        //return RESOURCE_DISK_CACHE;
//        //return MEMORY_CACHE;
//    }
//}
//
//class LoaderFactory : ModelLoaderFactory<IPicture, InputStream> {
//    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<IPicture, InputStream> {
//        return MyModelLoader()
//    }
//
//    override fun teardown() {
//        TODO("Not yet implemented")
//    }
//}