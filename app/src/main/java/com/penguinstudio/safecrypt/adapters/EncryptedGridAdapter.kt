package com.penguinstudio.safecrypt.adapters

import android.annotation.SuppressLint
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
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.EncryptedModel
import com.penguinstudio.safecrypt.models.MediaType
import com.penguinstudio.safecrypt.services.EncryptedDataSource
import com.penguinstudio.safecrypt.utilities.MediaMode
import com.penguinstudio.safecrypt.utilities.loadImage
import java.security.KeyStore


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

    val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MOVIE)
        .build()

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
            .inflate(R.layout.pictures_grid_item, parent, false)

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
        private var selectedVideo: StyledPlayerView = itemView.findViewById(R.id.selected_video)

        private var imageView: ImageView = itemView.findViewById(R.id.picturesInnerImage)
        private var checkBox: CheckBox = itemView.findViewById(R.id.picturesCheckbox)
        private var videoLayoutCard: CardView = itemView.findViewById(R.id.videoLayoutParentCard)
        private var videoTextViewDuration: TextView = itemView.findViewById(R.id.videoDuration)

        private var player: SimpleExoPlayer? = null



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

        private fun createMediaSourceFactory(): MediaSourceFactory {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            val localKey = keyStore.getKey("MyKey", null)

            val aesDataSource: DataSource = EncryptedDataSource(itemView.context, localKey) // Use your Cipher instance

            val factory = DataSource.Factory {
                aesDataSource
            }

            return ProgressiveMediaSource.Factory(factory)
        }

        

        private fun createVideoPlayback() {
            player?.stop()
            player?.clearMediaItems()
            player?.release()
            player = null

            val mi = MediaItem.fromUri(encryptedModel.uri)

            player =
            SimpleExoPlayer.Builder(itemView.context)
                .setMediaSourceFactory(createMediaSourceFactory())
                .build()

            player?.setMediaItem(mi)

            player?.setAudioAttributes(audioAttributes, true)

            selectedVideo.player = player

            selectedVideo.setOnClickListener {

            }


            player?.prepare()

            player?.addListener(object: Player.Listener {
                override fun onIsLoadingChanged(isLoading: Boolean) {
                    if(!isLoading) {


                        imageView.visibility = View.GONE
                        selectedVideo.visibility = View.VISIBLE
                        //Glide.with(itemView.context).clear(imageView)

                    }
                }
            })
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

            videoLayoutCard.visibility = View.GONE


            if(this.encryptedModel.mediaType == MediaType.VIDEO) {
                createVideoPlayback()
                return
            }


            fullRequest.loadImage(encryptedModel, imageView, encryptedModel.mediaType)
        }
    }

    override fun getPreloadItems(position: Int): MutableList<EncryptedModel> {
        return images.subList(position, position + 1)
    }

    override fun getPreloadRequestBuilder(item: EncryptedModel): RequestBuilder<Drawable> {
        return fullRequest.clone()
    }
}
