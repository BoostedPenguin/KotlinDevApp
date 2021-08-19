package com.penguinstudio.safecrypt.services.glide_service

import android.content.Context
import android.util.Log
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
import com.penguinstudio.safecrypt.services.GCMEncryptionService
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
    ): LoadData<InputStream> = LoadData(ObjectKey(model.uri), MyDataFetcher(model, context))

    override fun handles(s: IPicture): Boolean {
        return true
    }

    class MyDataFetcher (private val file: IPicture, private var context: Context,  private var time: Long = 0
    ) : DataFetcher<InputStream> {


        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface GCMEncryptionServicePoint {
            fun get(): GCMEncryptionService
        }

        private var isCanceled = false
        var mInputStream: InputStream? = null
        override fun loadData(
            priority: Priority,
            callback: DataFetcher.DataCallback<in InputStream?>
        ) {

            if (!isCanceled) {
                //Log.e("loadTime", "Start loading data resource of ${file.uri} - ${System.currentTimeMillis() - time}ms")
                time = System.currentTimeMillis()

                val cbcEncryptionService = EntryPoints.get(context, GCMEncryptionServicePoint::class.java).get()

                mInputStream = cbcEncryptionService.getCipherInputStream(file.uri)
            }
            callback.onDataReady(mInputStream)
        }

        override fun cleanup() {
            Log.e("loadTime", "- ${System.currentTimeMillis() - time}ms")

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