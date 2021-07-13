package com.penguinstudio.safecrypt.ui.home

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.AlbumGridAdapter
import com.penguinstudio.safecrypt.databinding.FragmentGalleryBinding
import com.penguinstudio.safecrypt.models.AlbumModel
import com.penguinstudio.safecrypt.utilities.Status
import dagger.hilt.android.AndroidEntryPoint


//TODO("scrolling, clicking, interaction with recyclerview needs to be DISABLED IF MEDIA FILES ARE LOADING")
@AndroidEntryPoint
class GalleryFragment : Fragment() {
    private val model: GalleryViewModel by activityViewModels()
    private lateinit var binding: FragmentGalleryBinding
    private lateinit var galleryAdapter: AlbumGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    // Handle the back button event
                    activity?.finishAndRemoveTask()
                }
            }

        requireActivity().onBackPressedDispatcher.addCallback(this, callback)

        // The callback can be enabled or disabled here or in handleOnBackPressed()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentGalleryBinding.inflate(layoutInflater)

        initGrid()

        registerEvents()

        model.getMedia()

        return binding.root
    }

    private fun registerEvents() {

        // On user manual refresh
        binding.gallerySwipeToRefresh.setOnRefreshListener {
            model.getMedia()
        }


        model.albums.observe(viewLifecycleOwner, {
            when (it.status) {
                Status.SUCCESS -> {
                    binding.gallerySwipeToRefresh.isRefreshing = false
                    binding.gallerySwipeToRefresh.isEnabled = true

                    binding.galleryProgressBar.visibility = View.GONE
                    binding.galleryRecyclerView.visibility = View.VISIBLE

                    it.data?.let { it1 -> galleryAdapter.setAlbums(it1.media) }
                }
                Status.ERROR -> {
                    binding.galleryProgressBar.visibility = View.GONE
                    binding.galleryRecyclerView.visibility = View.VISIBLE

                    binding.gallerySwipeToRefresh.isRefreshing = false
                    binding.gallerySwipeToRefresh.isEnabled = true

                    Snackbar.make(binding.root, "Something went wrong", Snackbar.LENGTH_SHORT)
                        .show()
                }
                Status.LOADING -> {

                    // Trigger spinner only if manual refresh wasn't activated
                    if (binding.gallerySwipeToRefresh.isRefreshing) {
                        binding.galleryProgressBar.visibility = View.INVISIBLE
                        binding.galleryRecyclerView.visibility = View.VISIBLE
                    } else {
                        binding.gallerySwipeToRefresh.isEnabled = false

                        binding.galleryProgressBar.visibility = View.VISIBLE
                        binding.galleryRecyclerView.visibility = View.VISIBLE
                    }
                }
            }
        })
    }


    private fun initGrid() {
        galleryAdapter = AlbumGridAdapter(object : AlbumGridAdapter.AdapterListeners {
            override fun onImageClickListener(position: Int, album: AlbumModel) {
                model.setSelectedAlbum(album)
                findNavController().navigate(R.id.action_homeFragment_to_picturesFragment)
            }
        })

        when(resources.configuration.orientation) {

            Configuration.ORIENTATION_LANDSCAPE -> {
                binding.galleryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 5)
            }
            else -> {
                binding.galleryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

            }
        }
        binding.galleryRecyclerView.adapter = galleryAdapter
    }
}