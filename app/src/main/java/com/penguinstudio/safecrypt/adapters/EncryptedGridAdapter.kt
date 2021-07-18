package com.penguinstudio.safecrypt.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.bitmap_recycle.ByteArrayAdapter
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.AlbumModel
import com.penguinstudio.safecrypt.models.MediaModel


class EncryptedGridAdapter(private var listener: EncryptedGridAdapter.AdapterListeners?) : RecyclerView.Adapter<EncryptedGridAdapter.MediaHolder>() {
    interface AdapterListeners {
        fun onImageClickListener(position: Int, album: AlbumModel)
    }

    private var _images: ArrayList<MediaModel> = ArrayList()

    // TODO
    // This is meant for testing only, it contains only bitmap extracted from decrypted file
    // Require more info
    private var images: MutableList<ByteArray> = ArrayList()

    fun setImages(images:  MutableList<ByteArray>?) {
        if (images != null) {
            this.images = images
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EncryptedGridAdapter.MediaHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.pictures_grid_fragment, parent, false)

        return MediaHolder(itemView)
    }

    override fun onBindViewHolder(holder: EncryptedGridAdapter.MediaHolder, position: Int) {
        val currentImage = images[position]

        Glide.with(holder.itemView)
            .load(currentImage)
            .fitCenter()
            .thumbnail(0.1f)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class MediaHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var imageView: ImageView = itemView.findViewById(R.id.picturesInnerImage)
        init {

        }
    }
}
