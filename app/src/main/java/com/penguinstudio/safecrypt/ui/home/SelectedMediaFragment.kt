package com.penguinstudio.safecrypt.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.ListPreloader.PreloadSizeProvider
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.ImagePagerAdapter
import com.penguinstudio.safecrypt.databinding.FragmentSelectedPictureBinding
import com.penguinstudio.safecrypt.models.MediaModel


class SelectedMediaFragment : Fragment(), LifecycleObserver {
    private lateinit var binding: FragmentSelectedPictureBinding
    private lateinit var viewPager: ViewPager2
    private var content: ArrayList<MediaModel> = ArrayList()
    private lateinit var fullRequest: RequestBuilder<Drawable>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    model.clearSelectedMedia()
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
        compositePageTransformer.addTransformer(MarginPageTransformer(40))
        viewPager.setPageTransformer(compositePageTransformer)

        // Toolbar control (should be images only)
        binding.selectedFragmentToolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        binding.selectedFragmentToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.selectedFragmentToolbar.visibility = View.VISIBLE


//        val sizeProvider: PreloadSizeProvider<MediaModel>  = ViewPreloadSizeProvider()
//        val modelProvider: PreloadModelProvider<MediaModel> = MyPreloadModelProvider()
//        val preloader: RecyclerViewPreloader<MediaModel> = RecyclerViewPreloader(
//            Glide.with(this), modelProvider, sizeProvider, 10
//        )


        val imagePagerAdapter = ImagePagerAdapter(object: ImagePagerAdapter.ImagePagerListeners {
            override fun onViewClickListener(position: Int, album: MediaModel) {
                handleToolbarOnImageClick().observe(viewLifecycleOwner, {
                    (viewPager.adapter as ImagePagerAdapter).isHandleVisible = it
                })
            }
        }, fullRequest, Glide.with(this))

        viewPager.adapter = imagePagerAdapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewPager.post {
                    (viewPager.adapter as ImagePagerAdapter).setCurrentPosition(position)
                }
            }
        })

        content = model.selectedAlbum.value?.data?.albumMedia
            ?: return

        imagePagerAdapter.setMedia(content)

        val g = imagePagerAdapter.getItemPosition(model.selectedMedia!!)
        viewPager.setCurrentItem(g, false)
    }

    override fun onPause() {
        super.onPause()

        (viewPager.adapter as ImagePagerAdapter).pausePlayer()
    }

    override fun onDestroyView() {
        viewPager.adapter = null
        super.onDestroyView()
    }


    private val model: GalleryViewModel by activityViewModels()
//    private val model: IPicturesViewModel
//        get() {
//            return _model
//        }

    private var _handle = MutableLiveData<Boolean>()
    private fun handleToolbarOnImageClick() : LiveData<Boolean> {
        binding.selectedFragmentToolbar.apply {
            if (visibility == View.VISIBLE) {
                animate()
                    .translationY(-height.toFloat())
                    .alpha(0f)
                    .setListener(object: AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                            visibility = View.INVISIBLE
                            _handle.value = binding.selectedFragmentToolbar.isVisible
                        }
                    })
                return _handle
            } else {
                visibility = View.VISIBLE
                alpha = 0f

                animate().translationY(0f)
                    .alpha(1f)
                    .setListener(null)

                _handle.value = binding.selectedFragmentToolbar.isVisible
                return _handle
            }
        }
    }

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
        fullRequest = Glide.with(this)
            .asDrawable()
            .placeholder(R.drawable.ic_baseline_image_24)
            .fitCenter()

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}