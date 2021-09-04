package com.penguinstudio.safecrypt.utilities

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.RequestBuilder
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.services.glide_service.GlideApp
import com.penguinstudio.safecrypt.services.glide_service.GlideRequest
import com.penguinstudio.safecrypt.services.glide_service.IPicture

private const val resolutionOverride: Int = 1200

fun Fragment.getGlideRequest() : GlideRequest<Drawable> {
    return GlideApp.with(this)
        .asDrawable()
        .override(resolutionOverride)
        .placeholder(R.drawable.ic_baseline_image_24)
        .fitCenter()
}

fun RequestBuilder<Drawable>.loadImage(imageSrc: Any, loadInto: ImageView){
    this
        .load(imageSrc)
        .placeholder(R.drawable.ic_baseline_image_24)
        .override(resolutionOverride)
        .fitCenter()
        .into(loadInto)
}