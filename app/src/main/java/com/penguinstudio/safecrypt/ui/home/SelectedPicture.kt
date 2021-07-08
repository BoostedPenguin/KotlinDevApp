package com.penguinstudio.safecrypt.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.databinding.FragmentSelectedPictureBinding
import com.penguinstudio.safecrypt.models.MediaType

class SelectedPicture : Fragment() {
    private lateinit var binding: FragmentSelectedPictureBinding
    private var mediaController: MediaController? = null

    private val _model: GalleryViewModel by activityViewModels()
    private val model: ISelectedMediaViewModel
        get() {
            return _model
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSelectedPictureBinding.inflate(layoutInflater, container, false)

        when(model.selectedMedia?.mediaType) {
            MediaType.IMAGE -> {
                binding.selectedPicture.visibility = View.VISIBLE

                Glide.with(this)
                    .load(model.selectedMedia?.mediaUri)
                    .thumbnail(0.1f)
                    .into(binding.selectedPicture);
            }
            MediaType.VIDEO -> {
                binding.selectedVideo.visibility = View.VISIBLE

                binding.selectedVideo.setVideoURI(model.selectedMedia?.mediaUri)
                mediaController = MediaController(requireContext())
                mediaController?.setAnchorView(binding.selectedVideo)
                binding.selectedVideo.setMediaController(mediaController)
                binding.selectedVideo.start()
            }
            else -> {
                Log.d("error", "Image Media type not provided")
            }
        }

        //registerEvents()

        return binding.root
    }

    private fun registerEvents() {

        /*
        Toolbar control:
        If SHOWN -> click will HIDE IT
        If HIDDEN -> click will SHOW IT for 5s
         */

        val actionBar = (activity as AppCompatActivity).supportActionBar!!

        Handler(Looper.getMainLooper()).postDelayed({
            actionBar.hide()
        }, 2000)

        binding.selectedVideo.setOnClickListener {

            if(actionBar.isShowing) {
                actionBar.hide()
            }
            else {
                actionBar.show()
                Handler(Looper.getMainLooper()).postDelayed({
                    actionBar.hide()
                }, 5000)
            }
        }
    }
}