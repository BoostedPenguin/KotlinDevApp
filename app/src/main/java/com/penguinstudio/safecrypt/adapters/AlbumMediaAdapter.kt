package com.penguinstudio.safecrypt.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.models.MediaType
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class AlbumMediaAdapter constructor(private var listener: AdapterListeners?,
                                    private var fullRequest: RequestBuilder<Drawable>,
                                    ) :
    RecyclerView.Adapter<AlbumMediaAdapter.ImageHolder>(),
    ListPreloader.PreloadModelProvider<MediaModel> {

    init {
        setHasStableIds(true)
    }

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
        adapterDataSetChanged()
    }


    fun setImages(images: ArrayList<MediaModel>, isSelectionMode: Boolean = false) {
        this.images = images
        this.isSelectionMode = isSelectionMode
        //adapterDataSetChanged()
    }

    /**
     * Use this instead of notifyDataSetChanged to ignore lint warnings
     * @see notifyDataSetChanged
     */
    @SuppressLint("NotifyDataSetChanged")
    fun adapterDataSetChanged() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.pictures_grid_item, parent, false)

        return ImageHolder(itemView)
    }

    override fun onBindViewHolder(holder: ImageHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int {
        return images.size
    }

    override fun getItemId(position: Int): Long {
        return images[position].id
    }

    fun getImages() : ArrayList<MediaModel> {
        return this.images
    }

    inner class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private lateinit var media: MediaModel
        private var imageView: ImageView = itemView.findViewById(R.id.picturesInnerImage)
        private var checkBox: CheckBox = itemView.findViewById(R.id.picturesCheckbox)
        private var videoLayoutCard: CardView = itemView.findViewById(R.id.videoLayoutParentCard)
        private var videoTextViewDuration: TextView = itemView.findViewById(R.id.videoDuration)

        fun bind(media: MediaModel) {
            this.media = media

            if(isSelectionMode) {
                checkBox.visibility = View.VISIBLE

                checkBox.isChecked = media.isSelected

                if(media.isSelected) {
                    imageView.setColorFilter(Color.parseColor("#4D000000"), PorterDuff.Mode.SRC_ATOP)

                    checkBox.isChecked = true
                }
                else {
                    imageView.clearColorFilter()

                    checkBox.isChecked = false
                }

            }
            else {
                if(imageView.colorFilter != null)
                    imageView.clearColorFilter()

                if(checkBox.visibility != View.INVISIBLE)
                    checkBox.visibility = View.INVISIBLE

                if(!checkBox.isChecked)
                    checkBox.isChecked = false
            }

            when(media.mediaType) {
                MediaType.VIDEO -> {
                    media.videoDuration?.let {

                        val formattedDuration = String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(it),
                            TimeUnit.MILLISECONDS.toSeconds(it) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(it))
                        )

                        videoTextViewDuration.text = formattedDuration
                        videoLayoutCard.visibility = View.VISIBLE
                    }
                }
                else -> videoLayoutCard.visibility = View.GONE
            }

            fullRequest
                .load(media.mediaUri)
                .placeholder(R.drawable.ic_baseline_image_24)
                .fitCenter()
                .into(imageView)
        }

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

    fun getPopupText(position: Int): String {
        if(images[position].details.dateAdded != null) {
            return SimpleDateFormat("MMM y", Locale.US)
                .format(images[position].details.dateAdded)
        }
        return ""
    }

    override fun getPreloadItems(position: Int): MutableList<MediaModel> {
        return images.subList(position, position + 1)
    }

    override fun getPreloadRequestBuilder(item: MediaModel): RequestBuilder<*> {
        return fullRequest.clone()
    }
}