package com.penguinstudio.safecrypt.adapters

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.models.MediaType


class ImagePagerAdapter(private var listener: ImagePagerListeners, context: Context)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val IMAGE_TYPE = 1
        const val VIDEO_TYPE = 2
    }

    interface ImagePagerListeners {
        fun onImageClickListener(position: Int, album: MediaModel)
    }
    private var media: ArrayList<MediaModel> = ArrayList()
    private var player: SimpleExoPlayer = SimpleExoPlayer.Builder(context).build()
    private var currentVideoPlayingPosition: Int = -1

    fun setMedia(media: ArrayList<MediaModel>) {
        this.media = media
        notifyDataSetChanged()
    }

    /**
     * On viewpager2 page switch
     * Internal video playing state
     * Switches dynamically the selected video
     */
    fun onPageSwitch(position: Int) {
        if(player.mediaItemCount > 0) {
            player.clearMediaItems()
            player.release()
        }

        if(media[position].mediaType == MediaType.VIDEO) {
            media[position].isSelected = true

            // Don't change any data, just tell it to re-render itself
            if(currentVideoPlayingPosition == position) {
                notifyItemChanged(position)
                return
            }

            if(currentVideoPlayingPosition != -1) {
                media[currentVideoPlayingPosition].isSelected = false
                notifyItemChanged(currentVideoPlayingPosition)
            }

            notifyItemChanged(position)
            currentVideoPlayingPosition = position
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (media[position].mediaType) {
            MediaType.IMAGE -> IMAGE_TYPE
            MediaType.VIDEO -> VIDEO_TYPE
            null -> throw IllegalArgumentException("Supplied media item isn't neither Video nor Image")
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)

        if(holder is InnerGalleryVideoViewHolder) {
            holder.releasePlayer()
        }
    }

    fun getItemPosition(item: MediaModel) : Int {
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
        return viewHolder;
    }



    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val vh = holder as CommonViewHolderItems
        vh.setMediaItem(media[position])
    }

    override fun getItemCount(): Int {
        return media.size
    }

    private interface CommonViewHolderItems {
        fun setMediaItem(media: MediaModel)
    }


    inner class InnerGalleryImageViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), CommonViewHolderItems {

        lateinit var media: MediaModel
        private var imageView: ImageView = itemView.findViewById(R.id.selected_picture)

        init {
            // On this click and on video click change boolean
            imageView.setOnClickListener {
                val position = bindingAdapterPosition

                if (position != RecyclerView.NO_POSITION) {
                    listener.onImageClickListener(position, media)
                }
            }
        }

        override fun setMediaItem(media: MediaModel) {
            this.media = media

            Glide.with(itemView)
                .load(media.mediaUri)
                .placeholder(R.drawable.ic_baseline_image_24)
                .thumbnail(0.1f)
                .into(imageView)

        }
    }

    inner class InnerGalleryVideoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), CommonViewHolderItems {

        lateinit var media: MediaModel
        private var selectedVideo: StyledPlayerView = itemView.findViewById(R.id.selected_video)
        private var imageThumbnail: ImageView = itemView.findViewById(R.id.selected_video_thumbnail)
        private var progressBar: ProgressBar = itemView.findViewById(R.id.selected_video_progressbar)

        override fun setMediaItem(media: MediaModel) {
            this.media = media
            imageThumbnail.visibility = View.VISIBLE
            selectedVideo.visibility = View.GONE

            Glide.with(itemView)
                .load(media.mediaUri)
                .placeholder(R.drawable.ic_baseline_image_24)
                .fitCenter()
                .thumbnail(0.1f)
                .into(imageThumbnail)

            if(media.isSelected) {
                createVideoPlayback()
            }
        }

        fun releasePlayer() {
            if(player.mediaItemCount > 0) {
                player.stop()
                player.clearMediaItems()
                player.release()
            }
        }


        private fun createVideoPlayback() {

            player = SimpleExoPlayer.Builder(itemView.context)
                .build()

            selectedVideo.player = player
            progressBar.visibility = View.VISIBLE

            selectedVideo.setOnClickListener {

                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    listener.onImageClickListener(bindingAdapterPosition, media)
                }
            }

            val mediaItem: MediaItem = MediaItem.fromUri(media.mediaUri)

            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady

            player.addListener(object: Player.Listener {
                override fun onIsLoadingChanged(isLoading: Boolean) {
                    super.onIsLoadingChanged(isLoading)
                    if(!isLoading) {
                        imageThumbnail.visibility = View.GONE
                        selectedVideo.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                    }
                }
            })

            selectedVideo.findViewById<ImageButton>(R.id.video_back).setOnClickListener {
                player.stop()
                player.clearMediaItems()
                player.release()

                findNavController(itemView).popBackStack()
            }
        }
    }
}