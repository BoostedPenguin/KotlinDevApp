package com.penguinstudio.safecrypt.services.glide_service

import android.content.Context
import androidx.annotation.Nullable
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.penguinstudio.safecrypt.services.CBCEncryptionService
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject


class SafeCryptModelLoader constructor(private val context: Context) : ModelLoader<IPicture, InputStream> {
    @Nullable

    override fun buildLoadData(
        model: IPicture,
        width: Int,
        height: Int,
        options: Options
    ): LoadData<InputStream> = LoadData(ObjectKey(model), MyDataFetcher(model, context))

    override fun handles(s: IPicture): Boolean {
        return true
    }

    class MyDataFetcher (private val file: IPicture, private var context: Context
    ) : DataFetcher<InputStream> {


        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface CBCEncryptionServiceEntryPoint {
            fun get(): CBCEncryptionService
        }

        private var isCanceled = false
        var mInputStream: InputStream? = null
        override fun loadData(
            priority: Priority,
            callback: DataFetcher.DataCallback<in InputStream?>
        ) {

            if (!isCanceled) {
                val cbcEncryptionService = EntryPoints.get(context, CBCEncryptionServiceEntryPoint::class.java).get()

                mInputStream = cbcEncryptionService.getCipherInputStream(file.uri)
            }
            callback.onDataReady(mInputStream)
        }

        override fun cleanup() {
            if (mInputStream != null) {
                try {
                    mInputStream!!.close()
                } catch (e: IOException) {
                }
            }
        }

        override fun cancel() {
            isCanceled = true
        }

        override fun getDataClass(): Class<InputStream> {
            return InputStream::class.java
        }

        override fun getDataSource(): DataSource {
            //return LOCAL;
            return DataSource.LOCAL
            //return DATA_DISK_CACHE;
            //return RESOURCE_DISK_CACHE;
            //return MEMORY_CACHE;
        }
    }

    class LoaderFactory constructor(private var context: Context) : ModelLoaderFactory<IPicture, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<IPicture, InputStream> {
            return SafeCryptModelLoader(context)
        }

        override fun teardown() {}
    }
}