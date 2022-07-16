package com.penguinstudio.safecrypt.adapters

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.MediaType
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaExtractor
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.penguinstudio.safecrypt.services.EncryptedDataSource
import com.penguinstudio.safecrypt.services.glide_service.IPicture
import com.penguinstudio.safecrypt.utilities.MediaMode
import com.penguinstudio.safecrypt.utilities.loadImage
import java.security.KeyStore


class SelectedMediaAdapter(private var listener: ImagePagerListeners,
                           var fullRequest: RequestBuilder<Drawable>,
                           val glide: RequestManager,
                           val mediaMode: MediaMode = MediaMode.NORMAL_MEDIA)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    ListPreloader.PreloadModelProvider<IPicture> {

    companion object {
        const val IMAGE_TYPE = 1
        const val VIDEO_TYPE = 2
    }

    interface ImagePagerListeners {
        fun onViewClickListener(position: Int, media: IPicture)
    }
    private var media: List<IPicture> = listOf()
    private var player: SimpleExoPlayer? = null
    private var currentSelectedItem = -1
    var isHandleVisible = true

    val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MOVIE)
        .build()

    fun setMedia(media: List<IPicture>) {
        calculateDiff(this.media, media)
        this.media = media
    }


    private fun calculateDiff(old: List<IPicture>, new: List<IPicture>) {
        val diffCallback = ItemDiffUtilCallback(old, new)
        val diffResults = DiffUtil.calculateDiff(diffCallback)

        diffResults.dispatchUpdatesTo(this)
    }

    fun pausePlayer() {
        player?.pause()
    }

    fun getItem(position: Int) : IPicture {
        return media[position]
    }

    fun getCurrentItem() : IPicture {
        return media[currentSelectedItem]
    }

    fun setCurrentPosition(position: Int) {

        //Notify current selected item that it ain't selected anymore
        notifyItemChanged(currentSelectedItem)

        // Notify the new current selected item to come in place
        currentSelectedItem = position
        notifyItemChanged(currentSelectedItem)
    }


    override fun getItemViewType(position: Int): Int {
        return when (media[position].mediaType) {
            MediaType.IMAGE -> IMAGE_TYPE
            MediaType.VIDEO -> VIDEO_TYPE
        }
    }


    fun getItemPosition(item: IPicture) : Int {
        return media.indexOf(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        lateinit var viewHolder: RecyclerView.ViewHolder
        val inflater = LayoutInflater.from(parent.context)

        when (viewType) {
            IMAGE_TYPE -> {
                val v1: View =
                    inflater.inflate(R.layout.selected_image_item, parent, false)
                viewHolder = InnerGalleryImageViewHolder(v1)
            }
            VIDEO_TYPE -> {
                val v1: View =
                    inflater.inflate(R.layout.selected_video_item, parent, false)
                viewHolder = InnerGalleryVideoViewHolder(v1)
            }
        }
        return viewHolder
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when(media[position].mediaType) {
            MediaType.IMAGE -> {
                val vh = holder as InnerGalleryImageViewHolder
                vh.setMediaItem(media[position])

                if(currentSelectedItem != -1
                    && currentSelectedItem < media.size
                    && currentSelectedItem == position) {

                    player?.stop()
                    player?.clearMediaItems()
                    player?.release()
                    player = null
                }
            }
            MediaType.VIDEO -> {
                val vh = holder as InnerGalleryVideoViewHolder
                vh.setMediaItem(media[position])

                if(currentSelectedItem != -1
                    && currentSelectedItem < media.size
                    && currentSelectedItem == position) {

                    vh.createVideoPlayback()
                }
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)

        if(holder is CommonViewHolderItems) {
            glide.clear(holder.imageView)
        }
    }

    override fun getItemCount(): Int {
        return media.size
    }


    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        player?.stop()
        player?.clearMediaItems()
        player?.release()
        player = null

        super.onDetachedFromRecyclerView(recyclerView)
    }

    private interface CommonViewHolderItems {
        fun setMediaItem(media: IPicture)
        var imageView: ImageView
    }


    inner class InnerGalleryImageViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), CommonViewHolderItems {

        lateinit var media: IPicture
        override var imageView: ImageView = itemView.findViewById(R.id.selected_picture)

        init {
            // On this click and on video click change boolean
            imageView.setOnClickListener {
                val position = layoutPosition

                if (position != RecyclerView.NO_POSITION) {
                    listener.onViewClickListener(position, media)
                }
            }
        }

        override fun setMediaItem(media: IPicture) {
            this.media = media

            when(mediaMode) {
                MediaMode.NORMAL_MEDIA -> {
                    fullRequest
                        .load(media.uri)
                        .placeholder(R.drawable.ic_baseline_image_24)
                        .override(1200)
                        .fitCenter()
                        .into(imageView)
                }
                MediaMode.ENCRYPTED_MEDIA -> {
                    fullRequest
                        .load(media)
                        .placeholder(R.drawable.ic_baseline_image_24)
                        .override(1200)
                        .fitCenter()
                        .into(imageView)
                }
            }
        }
    }

    inner class InnerGalleryVideoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), CommonViewHolderItems {

        lateinit var media: IPicture
        private var selectedVideo: StyledPlayerView = itemView.findViewById(R.id.selected_video)
        override var imageView: ImageView = itemView.findViewById(R.id.selected_video_thumbnail)
        private var progressBar: ProgressBar = itemView.findViewById(R.id.selected_video_progressbar)

        override fun setMediaItem(media: IPicture) {
            this.media = media
            selectedVideo.controllerAutoShow = false
            selectedVideo.controllerShowTimeoutMs = -1


            fullRequest.loadImage(media.uri, imageView)
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

        fun createVideoPlayback() {
            player?.stop()
            player?.clearMediaItems()
            player?.release()
            player = null

            val mi = MediaItem.fromUri(media.uri)

            player = when(mediaMode) {
                MediaMode.NORMAL_MEDIA ->
                    SimpleExoPlayer.Builder(itemView.context)
                        .build()
                MediaMode.ENCRYPTED_MEDIA ->
                    SimpleExoPlayer.Builder(itemView.context)
                        .setMediaSourceFactory(createMediaSourceFactory())
                        .build()
            }

            player?.setMediaItem(mi)

            player?.setAudioAttributes(audioAttributes, true)

            selectedVideo.player = player

            selectedVideo.setOnClickListener {

                if (layoutPosition != RecyclerView.NO_POSITION) {
                    listener.onViewClickListener(layoutPosition, media)
                }
            }

            if(isHandleVisible) {
                selectedVideo.showController()
            }
            else {
                selectedVideo.hideController()
            }


            player?.prepare()

            player?.addListener(object: Player.Listener {
                override fun onIsLoadingChanged(isLoading: Boolean) {
                    if(!isLoading) {


                        imageView.visibility = View.GONE
                        selectedVideo.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                        //Glide.with(itemView.context).clear(imageView)

                    }
                }
            })
        }
    }


    override fun getPreloadRequestBuilder(item: IPicture): RequestBuilder<*> {
        return fullRequest.clone()
    }

    override fun getPreloadItems(position: Int): List<IPicture> {
        return media.subList(position, position + 1)
    }
}