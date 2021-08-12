package com.penguinstudio.safecrypt.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
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
import com.bumptech.glide.RequestBuilder
import com.penguinstudio.safecrypt.adapters.SelectedMediaAdapter
import com.penguinstudio.safecrypt.databinding.FragmentSelectedPictureBinding
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.models.MediaType
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.*
import android.widget.*
import com.penguinstudio.safecrypt.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class SelectedMediaFragment : Fragment(), LifecycleObserver {
    private lateinit var binding: FragmentSelectedPictureBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var imagePagerAdapter: SelectedMediaAdapter
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
        binding.selectedFragmentToolbar.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.action_media_details -> {
                    showPopupWindow(imagePagerAdapter.getCurrentItem())
                    true
                }
                else -> true
            }
        }
        binding.selectedFragmentToolbar.visibility = View.VISIBLE

        imagePagerAdapter = SelectedMediaAdapter(object: SelectedMediaAdapter.ImagePagerListeners {
            override fun onViewClickListener(position: Int, media: MediaModel) {
                handleToolbarOnImageClick().observe(viewLifecycleOwner, {
                    (viewPager.adapter as SelectedMediaAdapter).isHandleVisible = it
                })
            }
        }, fullRequest, Glide.with(this))

        viewPager.adapter = imagePagerAdapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewPager.post {
                    imagePagerAdapter.setCurrentPosition(position)

                    when(imagePagerAdapter.getItem(position).mediaType) {
                        MediaType.IMAGE -> {
                            binding.selectedFragmentToolbar.
                            setBackgroundColor(resources.getColor(R.color.black_overlay))
                        }
                        MediaType.VIDEO -> {
                            binding.selectedFragmentToolbar.
                            setBackgroundColor(Color.TRANSPARENT)
                        }
                    }
                }
            }
        })

        content = model.selectedAlbum.value?.data?.albumMedia
            ?: return

        imagePagerAdapter.setMedia(content)

        viewPager.setCurrentItem(imagePagerAdapter.getItemPosition(model.selectedMedia!!), false)
    }

    override fun onPause() {
        super.onPause()

        imagePagerAdapter.pausePlayer()
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

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    private fun sizeConvertedToString(value: Double) : String {
        value.let {
            when {
                value >= 1024 * 1024 * 1024 -> {
                    //gb
                    return "${(value / 1024 / 1024 / 1024).format(2)} GBs"
                }
                value >= 1024 * 1024 -> {
                    //mb
                    return "${(value / 1024 / 1024).format(2)} MBs"
                }
                value >= 1024 -> {
                    //kb
                    return "${(value / 1024).format(2)} KBs"
                }
                else -> {
                    //byte
                    return "${value.format(2)} Bytes"
                }
            }
        }
    }

    private fun showPopupWindow(mediaModel: MediaModel) {

        //Create a View object yourself through inflater
        val popupView: View = View.inflate(context, R.layout.details_popup_view, null)


        binding.selectedPictureOverlay.visibility = View.VISIBLE

        //Specify the length and width through constants
        val width = LinearLayout.LayoutParams.MATCH_PARENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT


        //Make Inactive Items Outside Of PopupWindow
        val focusable = true

        //Create a window with our parameters
        val popupWindow = PopupWindow(popupView, width, height, focusable)
        popupWindow.setOnDismissListener {
            binding.selectedPictureOverlay.visibility = View.GONE
        }


        //Set the location of the window on the screen
        popupWindow.showAtLocation(view, Gravity.BOTTOM, 0, 0)

        val size = mediaModel.details.size?.toDouble()?.let { sizeConvertedToString(it) }

        var dimensions = ""
        when(mediaModel.mediaType) {
            MediaType.IMAGE -> {
                popupView.findViewById<TextView>(R.id.detailsDimensionsValue).text =
                    "${mediaModel.details.height}x${mediaModel.details.width}"
            }
            MediaType.VIDEO -> {
                mediaModel.videoDuration?.let {
                    val formattedDuration = String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(it),
                        TimeUnit.MILLISECONDS.toSeconds(it) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(it))
                    )

                    popupView.findViewById<TextView>(R.id.detailsDimensions).text =
                        "Duration:"

                    popupView.findViewById<TextView>(R.id.detailsDimensionsValue).text =
                        formattedDuration
                }
            }
        }
        popupView.findViewById<TextView>(R.id.detailsSizeValue).text = size ?: "No Value"
        popupView.findViewById<TextView>(R.id.detailsDateValue).text = "${SimpleDateFormat("dd MMMM yyyy hh:mm", Locale.US)
            .format(mediaModel.details.dateAdded)}"
        popupView.findViewById<TextView>(R.id.detailsPathValue).text = "${mediaModel.details.relativePath}"
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}