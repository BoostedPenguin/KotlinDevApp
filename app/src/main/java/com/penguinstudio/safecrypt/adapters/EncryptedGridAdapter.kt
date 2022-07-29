package com.penguinstudio.safecrypt.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.EncryptedModel
import com.penguinstudio.safecrypt.models.MediaType
import com.penguinstudio.safecrypt.utilities.getEncryptionKey
import com.penguinstudio.safecrypt.utilities.loadImage
import org.bytedeco.javacv.FFmpegFrameGrabber


class EncryptedGridAdapter constructor(
    private var listener: AdapterListeners,
    private var fullRequest: RequestBuilder<Drawable>
    ) :
    RecyclerView.Adapter<EncryptedGridAdapter.MediaHolder>(),
    ListPreloader.PreloadModelProvider<EncryptedModel> {

    interface AdapterListeners {
        fun onClickListener(position: Int, media: EncryptedModel)
        fun onLongClickListener(position: Int, media: EncryptedModel)
    }

    private var images: ArrayList<EncryptedModel> = ArrayList()
    private var isSelectionMode: Boolean = false

    fun setImages(images:  ArrayList<EncryptedModel>, isSelectionMode: Boolean = false) {
        calculateDiff(this.images, images)

        this.images = images
        this.isSelectionMode = isSelectionMode
    }


    @SuppressLint("NotifyDataSetChanged")
    fun toggleSelectionMode(isSelectionMode: Boolean, selectedPosition: Int = -1) {
        this.isSelectionMode = isSelectionMode

        if(!isSelectionMode)
            images.forEach {
                if(it.isSelected) it.isSelected = false
            }
        else
            if(selectedPosition != -1)
                images[selectedPosition].isSelected = true

        notifyDataSetChanged()
    }

    private fun calculateDiff(old: List<EncryptedModel>, new: List<EncryptedModel>) {
        val diffCallback = ItemDiffUtilCallback(old, new)
        val diffResults = DiffUtil.calculateDiff(diffCallback)
        diffResults.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EncryptedGridAdapter.MediaHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.encrypted_pictures_grid_item, parent, false)

        return MediaHolder(itemView)
    }

    override fun onBindViewHolder(holder: EncryptedGridAdapter.MediaHolder, position: Int) {
        holder.bind((images[position]))
    }

    override fun getItemCount(): Int {
        return images.size
    }

    fun getImages() : ArrayList<EncryptedModel> {
        return this.images
    }

    inner class MediaHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private lateinit var encryptedModel: EncryptedModel

        private var imageView: ImageView = itemView.findViewById(R.id.encryptedPicturesInnerImage)
        private var checkBox: CheckBox = itemView.findViewById(R.id.encryptedPicturesCheckbox)
        private var videoLayoutCard: CardView = itemView.findViewById(R.id.encryptedVideoLayoutParentCard)


        init {
            imageView.setOnClickListener {
                val position = bindingAdapterPosition

                if(position != RecyclerView.NO_POSITION) {
                    listener.onClickListener(position, encryptedModel)
                }
            }

            imageView.setOnLongClickListener {
                val position = bindingAdapterPosition

                if(position != RecyclerView.NO_POSITION) {
                    listener.onLongClickListener(position, encryptedModel)
                }

                true
            }
            checkBox.setOnClickListener {
                val position = bindingAdapterPosition

                if(position != RecyclerView.NO_POSITION) {
                    listener.onClickListener(position, encryptedModel)
                }
            }
        }

        fun bind(media: EncryptedModel) {
            this.encryptedModel = media

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

            if(encryptedModel.mediaType == MediaType.VIDEO) {
                videoLayoutCard.visibility = View.VISIBLE
            }
            else {
                videoLayoutCard.visibility = View.GONE
            }

            fullRequest.loadImage(encryptedModel, imageView, true, encryptedModel.mediaType, itemView.context.getEncryptionKey())
        }
    }

    override fun getPreloadItems(position: Int): MutableList<EncryptedModel> {
        return images.subList(position, position + 1)
    }

    override fun getPreloadRequestBuilder(item: EncryptedModel): RequestBuilder<Drawable> {
        return fullRequest.clone()
    }
}
