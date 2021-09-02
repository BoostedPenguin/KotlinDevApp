package com.penguinstudio.safecrypt.services.glide_service

import android.content.Context
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.penguinstudio.safecrypt.models.MediaType
import java.io.*
import java.nio.ByteBuffer


interface IPicture {
    val uri: Uri
    var mediaType: MediaType
    val mediaName: String?
    val size: String?
    var isSelected: Boolean
}

@GlideModule
class SafeCryptGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDefaultRequestOptions(
            RequestOptions()
                .format(DecodeFormat.PREFER_RGB_565))
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(IPicture::class.java, InputStream::class.java,
            SafeCryptModelLoader.LoaderFactory(context)
        )

//        registry.prepend(IPicture::class.java, ByteBuffer::class.java,
//            GlideModuleByteBuffer.LoaderFactory(context)
//        )

        super.registerComponents(context, glide, registry)

//        registry.append(
//            File::class.java, InputStream::class.java,
//            FileLoader.Factory(object : FileOpener<InputStream> {
//
//                @Throws(FileNotFoundException::class)
//                override fun open(file: File): InputStream {
//                    //return CBCEncryptionService.getCipherInputStream(file.toUri())
//                     return FileInputStream(file);
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
    }


    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}