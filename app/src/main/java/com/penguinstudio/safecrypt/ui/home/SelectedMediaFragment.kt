package com.penguinstudio.safecrypt.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.databinding.FragmentSelectedPictureBinding
import com.penguinstudio.safecrypt.models.MediaType


class SelectedMediaFragment : Fragment(), LifecycleObserver {
    private lateinit var binding: FragmentSelectedPictureBinding

    private val _model: GalleryViewModel by activityViewModels()
    private val model: ISelectedMediaViewModel
        get() {
            return _model
        }

    private var player: SimpleExoPlayer? = null


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.hide()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectedPictureBinding.inflate(layoutInflater, container, false)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        when(model.selectedMedia?.mediaType) {
            MediaType.IMAGE -> {
                binding.selectedPicture.visibility = View.VISIBLE

                createPhotoSetup()
            }
            MediaType.VIDEO -> {
                binding.selectedVideo.visibility = View.VISIBLE

                createVideoPlayback()
            }
            else -> {
                Log.d("error", "Image Media type not provided")
            }
        }
        return binding.root
    }

    override fun onPause() {
        super.onPause()

        if(player != null) {
            player?.pause()
        }
    }

    private fun createPhotoSetup() {

        binding.selectedFragmentToolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        binding.selectedFragmentToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.selectedFragmentToolbar.visibility = View.VISIBLE


        Glide.with(this)
            .load(model.selectedMedia?.mediaUri)
            .thumbnail(0.1f)
            .into(binding.selectedPicture)

        handleToolbarOnImageClick()
        binding.selectedPicture.setOnClickListener {
            handleToolbarOnImageClick()
        }
    }

    private fun handleToolbarOnImageClick() {
        binding.selectedFragmentToolbar.apply {
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

    private fun createVideoPlayback() {

        player = SimpleExoPlayer.Builder(requireContext()).build()

        // Bind the player to the view.
        binding.selectedVideo.player = player;

        val mediaItem: MediaItem = MediaItem.fromUri(model.selectedMedia?.mediaUri!!)

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()

        binding.selectedVideo.findViewById<ImageButton>(R.id.video_back).setOnClickListener {
            player?.stop()
            player?.clearMediaItems()
            player?.release()
            findNavController().popBackStack()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}