package com.penguinstudio.safecrypt.services.glide_service

import android.content.Context
import android.util.Log
import androidx.annotation.Nullable
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.penguinstudio.safecrypt.services.GCMEncryptionService
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.nio.ByteBuffer

class GlideModuleByteBuffer constructor(private val context: Context) :
    ModelLoader<IPicture, ByteBuffer> {
    @Nullable

    override fun buildLoadData(
        model: IPicture,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<ByteBuffer> =
        ModelLoader.LoadData(ObjectKey(model), MyDataFetcher(model, context))

    override fun handles(s: IPicture): Boolean {
        return true
    }

    class MyDataFetcher (private val file: IPicture, private var context: Context, private var time: Long = 0
    ) : DataFetcher<ByteBuffer> {


        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface GCMEncryptionServiceEntryPoint {
            fun get(): GCMEncryptionService
        }

        private var isCanceled = false
        override fun loadData(
            priority: Priority,
            callback: DataFetcher.DataCallback<in ByteBuffer?>
        ) {

            var byteBuffer: ByteBuffer? = null
            if (!isCanceled) {
                val gcmEncryptionService = EntryPoints.get(context, GCMEncryptionServiceEntryPoint::class.java).get()

                time = System.currentTimeMillis()
                //Log.e("loadTime", " - ${System.currentTimeMillis() - time}ms")
                val byteData = gcmEncryptionService.decryptFileToByteArray(file.uri)


                byteBuffer = ByteBuffer.wrap(byteData)
            }
            callback.onDataReady(byteBuffer)
        }

        override fun cleanup() {
            Log.e("loadTime", " - ${System.currentTimeMillis() - time}ms")
        }

        override fun cancel() {
            isCanceled = true
        }

        override fun getDataClass(): Class<ByteBuffer> {
            return ByteBuffer::class.java
        }

        override fun getDataSource(): DataSource {
            //return LOCAL;
            return DataSource.LOCAL
            //return DATA_DISK_CACHE;
            //return RESOURCE_DISK_CACHE;
            //return MEMORY_CACHE;
        }
    }

    class LoaderFactory constructor(private var context: Context) :
        ModelLoaderFactory<IPicture, ByteBuffer> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<IPicture, ByteBuffer> {
            return GlideModuleByteBuffer(context)
        }

        override fun teardown() {}
    }
}