package com.penguinstudio.safecrypt.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleObserver
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.ImagePagerAdapter
import com.penguinstudio.safecrypt.databinding.FragmentSelectedPictureBinding
import com.penguinstudio.safecrypt.models.MediaType


class SelectedMediaFragment : Fragment(), LifecycleObserver {
    private lateinit var binding: FragmentSelectedPictureBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var imagePagerAdapter: ImagePagerAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    // Handle the back button event
                    viewPager.adapter = null
                    findNavController().popBackStack()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = binding.pager
        viewPager.clipToPadding = false
        viewPager.offscreenPageLimit = 1
        viewPager.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER;
        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(40));
        viewPager.setPageTransformer(compositePageTransformer);


        imagePagerAdapter = ImagePagerAdapter()
        viewPager.adapter = imagePagerAdapter

        val content = model.selectedAlbum.value?.data?.albumMedia
            ?: return

        imagePagerAdapter.setMedia(content)

        val g = imagePagerAdapter.getItemPosition(model.selectedMedia!!)
        viewPager.setCurrentItem(g, false)
    }


    private val model: GalleryViewModel by activityViewModels()
//    private val model: IPicturesViewModel
//        get() {
//            return _model
//        }

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

        return binding.root
    }

    override fun onPause() {
        super.onPause()

        if(player != null) {
            player?.pause()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}