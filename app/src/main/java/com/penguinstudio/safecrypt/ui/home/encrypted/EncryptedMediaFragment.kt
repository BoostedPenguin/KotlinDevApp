package com.penguinstudio.safecrypt.ui.home.encrypted

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.navigation.fragment.findNavController
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.databinding.FragmentEncryptedMediaBinding
import com.penguinstudio.safecrypt.ui.home.EncryptedMediaViewModel
import com.penguinstudio.safecrypt.ui.home.GalleryViewModel

class EncryptedMediaFragment : Fragment(), LifecycleObserver {
    private val model: EncryptedMediaViewModel by activityViewModels()
    private lateinit var binding: FragmentEncryptedMediaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    onBackPress()
                }
            }

        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun onBackPress() {
        // Handle the back button event
//        if(model.itemSelectionMode) {
//            exitSelectMode()
//            return
//        }
        findNavController().popBackStack()
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreated(){
        (activity as AppCompatActivity?)?.supportActionBar?.title = "Encrypted Media"
        (activity as AppCompatActivity).supportActionBar?.show()
        activity?.lifecycle?.removeObserver(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.lifecycle?.addObserver(this)
    }

    override fun onDetach() {
        super.onDetach()
        activity?.lifecycle?.removeObserver(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentEncryptedMediaBinding.inflate(layoutInflater, container, false)
        (activity as AppCompatActivity?)?.supportActionBar?.title = "Encrypted Media"
        (activity as AppCompatActivity).supportActionBar?.show()

        initGrid()

        return binding.root
    }

    private fun initGrid() {

    }

    private fun registerEvents() {

    }
}