package com.penguinstudio.safecrypt.adapters

import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
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
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.models.MediaType
import java.util.concurrent.TimeUnit


class PhotoGridAdapter constructor(private var listener: AdapterListeners?) : RecyclerView.Adapter<PhotoGridAdapter.ImageHolder>() {
    interface AdapterListeners {
        fun onClickListener(position: Int, media: MediaModel)
        fun onLongClickListener(position: Int, media: MediaModel)
    }
    private var images: ArrayList<MediaModel> = ArrayList()
    private var isSelectionMode: Boolean = false


    fun toggleSelectionMode(isSelectionMode: Boolean) {
        this.isSelectionMode = isSelectionMode
        images.forEach {
            it.isSelected = false
        }
        notifyDataSetChanged()
    }


    fun setImages(images: ArrayList<MediaModel>?) {
        if (images != null) {
            this.images = images
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.pictures_grid_fragment, parent, false)

        return ImageHolder(itemView)
    }

    override fun onBindViewHolder(holder: ImageHolder, position: Int) {
        val currentImage = images[position]
        holder.media = currentImage

        if(isSelectionMode) {
            holder.checkBox.visibility = View.VISIBLE

            holder.checkBox.isChecked = images[position].isSelected

            if(images[position].isSelected) {
                holder.imageView.setColorFilter(Color.parseColor("#4D000000"), PorterDuff.Mode.SRC_ATOP);

                holder.checkBox.isChecked = true
            }
            else {
                holder.imageView.clearColorFilter()

                holder.checkBox.isChecked = false
            }

        }
        else {
            holder.imageView.clearColorFilter()
            holder.checkBox.visibility = View.INVISIBLE
            holder.checkBox.isChecked = false
        }

        when(holder.media.mediaType) {
            MediaType.VIDEO -> {
                holder.media.videoDuration?.let {

                    val formattedDuration = String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(it),
                        TimeUnit.MILLISECONDS.toSeconds(it) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(it))
                    )

                    holder.videoTextViewDuration.text = formattedDuration
                    holder.videoLayoutCard.visibility = View.VISIBLE
                }
            }
            else -> holder.videoLayoutCard.visibility = View.GONE
        }

        Glide.with(holder.itemView)
            .load(currentImage.mediaUri)
            .fitCenter()
            .thumbnail(0.1f)
            .into(holder.imageView);
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal lateinit var media: MediaModel
        internal var imageView: ImageView = itemView.findViewById(R.id.picturesInnerImage)
        internal var checkBox: CheckBox = itemView.findViewById(R.id.picturesCheckbox)
        internal var videoLayoutCard: CardView = itemView.findViewById(R.id.videoLayoutParentCard)
        internal var videoTextViewDuration: TextView = itemView.findViewById(R.id.videoDuration)

        init {
            imageView.setOnClickListener {
                val position = adapterPosition

                if( listener != null && position != RecyclerView.NO_POSITION) {
                    listener!!.onClickListener(position, media)
                }
            }

            imageView.setOnLongClickListener {
                val position = adapterPosition

                if( listener != null && position != RecyclerView.NO_POSITION) {
                    listener!!.onLongClickListener(position, media)
                }

                true
            }
            checkBox.setOnClickListener {
                val position = adapterPosition

                if( listener != null && position != RecyclerView.NO_POSITION) {
                    listener!!.onClickListener(position, media)
                }
            }
        }
    }
}