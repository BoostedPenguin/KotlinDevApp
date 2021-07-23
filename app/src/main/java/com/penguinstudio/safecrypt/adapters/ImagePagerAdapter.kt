package com.penguinstudio.safecrypt.adapters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.models.MediaType


class ImagePagerAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val IMAGE_TYPE = 1
        const val VIDEO_TYPE = 2
    }
    private var media: ArrayList<MediaModel> = ArrayList()

    fun setMedia(media: ArrayList<MediaModel>) {
        this.media = media
        notifyDataSetChanged()
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

    private class InnerGalleryImageViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), CommonViewHolderItems {

        lateinit var media: MediaModel
        private var imageView: ImageView = itemView.findViewById(R.id.selected_picture)
        private var selectedFragmentToolbar: Toolbar = itemView.findViewById(R.id.selected_fragment_toolbar)
        override fun setMediaItem(media: MediaModel) {
            this.media = media

            selectedFragmentToolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
            selectedFragmentToolbar.setNavigationOnClickListener {
                findNavController(itemView).popBackStack()
            }
            selectedFragmentToolbar.visibility = View.VISIBLE

//            val photoViewAttacher = PhotoViewAttacher(imageView)
//            if (photoViewAttacher.isZoomEnabled) {
//                // Prevent pageviewer from sliding
//            }

            Glide.with(itemView)
                .load(media.mediaUri)
                .placeholder(R.drawable.ic_baseline_image_24)
                .thumbnail(0.1f)
                .into(imageView)

            handleToolbarOnImageClick()
            imageView.setOnClickListener {
                handleToolbarOnImageClick()
            }
        }

        private fun handleToolbarOnImageClick() {
            selectedFragmentToolbar.apply {
                if (visibility == View.VISIBLE) {
                    animate()
                        .translationY(-height.toFloat())
                        .alpha(0f)
                        .setListener(object: AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                super.onAnimationEnd(animation)
                                visibility = View.INVISIBLE
                            }
                        })
                } else {
                    visibility = View.VISIBLE
                    alpha = 0f

                    animate().translationY(0f)
                        .alpha(1f)
                        .setListener(null)
                }
            }
        }
    }

    private class InnerGalleryVideoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), CommonViewHolderItems {

        lateinit var media: MediaModel
        private lateinit var player: SimpleExoPlayer
        private var selectedVideo: StyledPlayerView = itemView.findViewById(R.id.selected_video)

        override fun setMediaItem(media: MediaModel) {
            this.media = media

            createVideoPlayback()
        }

        fun releasePlayer() {
            player.stop()
            player.clearMediaItems()
            player.release()
        }


        private fun createVideoPlayback() {


            player = SimpleExoPlayer.Builder(itemView.context).build()

            // Bind the player to the view.
            selectedVideo.player = player;

            val mediaItem: MediaItem = MediaItem.fromUri(media.mediaUri)

            player.setMediaItem(mediaItem)
            player.prepare()
            //player.play()

            selectedVideo.findViewById<ImageButton>(R.id.video_back).setOnClickListener {
                player.stop()
                player.clearMediaItems()
                player.release()

                findNavController(itemView).popBackStack()
            }
        }
    }
}