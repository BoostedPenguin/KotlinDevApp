package com.penguinstudio.safecrypt.utilities

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.RequestBuilder
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.MediaType
import com.penguinstudio.safecrypt.services.glide_service.GlideApp
import com.penguinstudio.safecrypt.services.glide_service.GlideRequest
import com.penguinstudio.safecrypt.services.glide_service.IPicture

private const val resolutionOverride: Int = 1200

fun Fragment.getGlideRequest() : GlideRequest<Drawable> {
    return GlideApp.with(this)
        .asDrawable()
        .placeholder(R.drawable.ic_baseline_image_24)
        .fitCenter()
}

fun RequestBuilder<Drawable>.loadImage(imageSrc: Any, loadInto: ImageView, type: MediaType? = null){
    this
        .load(imageSrc)
        .placeholder(if (type == MediaType.VIDEO) R.drawable.ic_baseline_video_library_24 else R.drawable.ic_baseline_image_24)
        .fitCenter()
        .into(loadInto)
}