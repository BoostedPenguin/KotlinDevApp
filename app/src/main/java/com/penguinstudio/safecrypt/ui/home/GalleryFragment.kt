package com.penguinstudio.safecrypt.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.GalleryType
import com.penguinstudio.safecrypt.databinding.FragmentGalleryBinding

class GalleryFragment : Fragment() {
    private val model: GalleryViewModel by viewModels()
    private lateinit var binding: FragmentGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Runtime gallery type store
        arguments?.takeIf { it.containsKey("obs") }?.apply {
            model.setGalleryType(getSerializable("GalleryType") as GalleryType)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentGalleryBinding.inflate(layoutInflater)

        return binding.root
    }

    private fun configureGalleryFragment() {
        when(model.galleryType) {
            GalleryType.NORMAL -> {

            }
            GalleryType.ENCRYPTED -> {

            }
        }
    }
}