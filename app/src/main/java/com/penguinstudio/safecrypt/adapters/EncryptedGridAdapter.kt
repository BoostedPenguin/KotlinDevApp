package com.penguinstudio.safecrypt.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.AlbumModel
import com.penguinstudio.safecrypt.models.EncryptedModel


class EncryptedGridAdapter(private var listener: EncryptedGridAdapter.AdapterListeners?) : RecyclerView.Adapter<EncryptedGridAdapter.MediaHolder>() {
    interface AdapterListeners {
        fun onImageClickListener(position: Int, album: AlbumModel)
    }

    private var images: MutableList<EncryptedModel> = ArrayList()

    fun setImages(images:  MutableList<EncryptedModel>?) {
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
            .inflate(R.layout.pictures_grid_item, parent, false)

        return MediaHolder(itemView)
    }

    override fun onBindViewHolder(holder: EncryptedGridAdapter.MediaHolder, position: Int) {
        val currentImage = images[position]

        holder.videoLayoutCard.visibility = View.GONE
        holder.checkBox.visibility = View.INVISIBLE
        holder.checkBox.isChecked = false

        Glide.with(holder.itemView)
            .load(currentImage)
            .fitCenter()
            .placeholder(R.drawable.ic_baseline_image_24)
            .thumbnail(0.1f)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class MediaHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var imageView: ImageView = itemView.findViewById(R.id.picturesInnerImage)

        internal var checkBox: CheckBox = itemView.findViewById(R.id.picturesCheckbox)
        internal var videoLayoutCard: CardView = itemView.findViewById(R.id.videoLayoutParentCard)
        internal var videoTextViewDuration: TextView = itemView.findViewById(R.id.videoDuration)

        init {

        }
    }
}
