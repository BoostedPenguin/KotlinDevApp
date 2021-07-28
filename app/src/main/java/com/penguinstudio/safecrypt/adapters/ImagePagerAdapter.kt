package com.penguinstudio.safecrypt.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.models.MediaType
import kotlin.collections.ArrayList


class ImagePagerAdapter(private var listener: ImagePagerListeners,
                        var fullRequest: RequestBuilder<Drawable>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    ListPreloader.PreloadModelProvider<MediaModel> {

    companion object {
        const val IMAGE_TYPE = 1
        const val VIDEO_TYPE = 2
    }

    interface ImagePagerListeners {
        fun onImageClickListener(position: Int, album: MediaModel)
    }
    private var media: ArrayList<MediaModel> = ArrayList()
    private var player: SimpleExoPlayer? = null
    private var currentSelectedItem = -1

    fun setMedia(media: ArrayList<MediaModel>) {
        this.media = media
        notifyDataSetChanged()
    }

    fun setCurrentPosition(position: Int) {

        //Notify current selected item to fuck off
        notifyItemChanged(currentSelectedItem)

        // Notify the new current selected item to come in place
        currentSelectedItem = position
        notifyItemChanged(currentSelectedItem)
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
            null -> throw IllegalArgumentException("No such type")
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)

        if(holder is CommonViewHolderItems) {
            Glide.with(holder.itemView.context).clear(holder.imageView);
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
        fun setMediaItem(media: MediaModel)
        var imageView: ImageView
    }


    inner class InnerGalleryImageViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), CommonViewHolderItems {

        lateinit var media: MediaModel
        override var imageView: ImageView = itemView.findViewById(R.id.selected_picture)

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

            fullRequest
                .load(media.mediaUri)
                .placeholder(R.drawable.ic_baseline_image_24)
                .fitCenter()
                .into(imageView)

        }
    }

    inner class InnerGalleryVideoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), CommonViewHolderItems {

        lateinit var media: MediaModel
        private var selectedVideo: StyledPlayerView = itemView.findViewById(R.id.selected_video)
        override var imageView: ImageView = itemView.findViewById(R.id.selected_video_thumbnail)
        private var progressBar: ProgressBar = itemView.findViewById(R.id.selected_video_progressbar)

        override fun setMediaItem(media: MediaModel) {
            this.media = media


            fullRequest
                .load(media.mediaUri)
                .placeholder(R.drawable.ic_baseline_image_24)
                .fitCenter()
                .into(imageView)
        }

        fun createVideoPlayback() {
            imageView.visibility = View.INVISIBLE
            progressBar.visibility = View.INVISIBLE
            selectedVideo.visibility = View.VISIBLE

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
                        Glide.with(itemView.context).clear(imageView)

                        imageView.visibility = View.GONE
                        selectedVideo.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
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

    override fun getPreloadItems(position: Int): MutableList<MediaModel> {
        return media.subList(position, position + 1);
    }

    override fun getPreloadRequestBuilder(item: MediaModel): RequestBuilder<*>? {
        return fullRequest.clone()
    }
}