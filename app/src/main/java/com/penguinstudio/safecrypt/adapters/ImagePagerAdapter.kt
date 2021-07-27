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
    private var player: SimpleExoPlayer? = null
    private var currentPosition: Int = -1

    fun setMedia(media: ArrayList<MediaModel>) {
        this.media = media
        notifyDataSetChanged()
    }

    fun setCurrentPosition(position: Int) {
        media[position].isSelected = true
        notifyItemChanged(position)
    }


    override fun getItemViewType(position: Int): Int {
        return when (media[position].mediaType) {
            MediaType.IMAGE -> IMAGE_TYPE
            MediaType.VIDEO -> VIDEO_TYPE
            null -> throw IllegalArgumentException("Supplied media item isn't neither Video nor Image")
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

        when(media[position].mediaType) {
            MediaType.IMAGE -> {
                val vh = holder as InnerGalleryImageViewHolder
                vh.setMediaItem(media[position])
            }
            MediaType.VIDEO -> {
                val vh = holder as InnerGalleryVideoViewHolder
                vh.setMediaItem(media[position])

                if(media[position].isSelected) {
                    vh.createVideoPlayback()
                }
            }
            null -> throw IllegalArgumentException("No such type")
        }
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

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
//
//        player?.stop()
//        player?.clearMediaItems()
//        player?.release()
//
//        player = null
    }

    inner class InnerGalleryVideoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), CommonViewHolderItems {

        lateinit var media: MediaModel
        private var selectedVideo: StyledPlayerView = itemView.findViewById(R.id.selected_video)
        private var imageThumbnail: ImageView = itemView.findViewById(R.id.selected_video_thumbnail)
        private var progressBar: ProgressBar = itemView.findViewById(R.id.selected_video_progressbar)

        override fun setMediaItem(media: MediaModel) {
            this.media = media

            imageThumbnail.visibility = View.INVISIBLE
            progressBar.visibility = View.INVISIBLE
            selectedVideo.visibility = View.VISIBLE

            Glide.with(itemView)
                .load(media.mediaUri)
                .placeholder(R.drawable.ic_baseline_image_24)
                .fitCenter()
                .thumbnail(0.1f)
                .into(imageThumbnail)
        }

        fun createVideoPlayback() {

            player?.stop()
            player?.clearMediaItems()
            player?.release()
            player = null

            player = SimpleExoPlayer.Builder(itemView.context)
                .build()

            selectedVideo.player = player

            selectedVideo.setOnClickListener {

                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    listener.onImageClickListener(bindingAdapterPosition, media)
                }
            }

            val mediaItem: MediaItem = MediaItem.fromUri(media.mediaUri)

            player?.setMediaItem(mediaItem)
            player?.prepare()

            player?.addListener(object: Player.Listener {
                override fun onIsLoadingChanged(isLoading: Boolean) {
                    if(!isLoading) {

                    }
                }
            })


//            selectedVideo.findViewById<ImageButton>(R.id.video_back).setOnClickListener {
//                player?.stop()
//                player?.clearMediaItems()
//                player?.release()
//
//                findNavController(itemView).popBackStack()
//            }
        }
    }
}