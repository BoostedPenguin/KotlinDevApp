package com.penguinstudio.safecrypt.ui.home

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.penguinstudio.safecrypt.NavGraphDirections
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.AlbumGridAdapter
import com.penguinstudio.safecrypt.adapters.PhotoGridAdapter
import com.penguinstudio.safecrypt.databinding.FragmentPicturesBinding
import com.penguinstudio.safecrypt.utilities.GalleryType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PicturesFragment : Fragment() {
    private lateinit var binding: FragmentPicturesBinding
    private lateinit var photoAdapter: PhotoGridAdapter
    private val model: GalleryViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentPicturesBinding.inflate(layoutInflater, container, false)
        setHasOptionsMenu(true)

        initGrid()

        registerEvents()

        photoAdapter.setImages(model.selectedAlbum?.albumPhotos)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_settings -> {
                findNavController().navigate(NavGraphDirections.actionToSettingsFragment())
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun initGrid() {
        photoAdapter = PhotoGridAdapter()
        binding.picturesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.picturesRecyclerView.adapter = photoAdapter
    }

    private fun registerEvents() {

    }
}