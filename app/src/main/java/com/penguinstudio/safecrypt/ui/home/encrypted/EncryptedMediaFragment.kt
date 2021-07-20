package com.penguinstudio.safecrypt.ui.home.encrypted

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.penguinstudio.safecrypt.adapters.EncryptedGridAdapter
import com.penguinstudio.safecrypt.adapters.PhotoGridAdapter
import com.penguinstudio.safecrypt.databinding.FragmentEncryptedMediaBinding
import com.penguinstudio.safecrypt.models.AlbumModel
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.services.EncryptionProcessIntentHandler
import com.penguinstudio.safecrypt.utilities.Status
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EncryptedMediaFragment : Fragment(), LifecycleObserver {
    private val model: EncryptedMediaViewModel by activityViewModels()
    private lateinit var binding: FragmentEncryptedMediaBinding
    private lateinit var encryptedMediaAdapter: EncryptedGridAdapter

    @Inject
    lateinit var encryptionProcessIntentHandler: EncryptionProcessIntentHandler

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
        (activity as AppCompatActivity).supportActionBar?.show()

        //chooseDefaultSaveLocation()
        binding.enMediaSaveLocation.setOnClickListener {
            encryptionProcessIntentHandler.chooseDefaultSaveLocation().observe(viewLifecycleOwner, {
                when(it) {
                    Activity.RESULT_OK -> {
                        binding.enMediaSaveLocation.visibility = View.VISIBLE
                        binding.enMediaHint.visibility = View.VISIBLE
                        binding.enPicturesRecyclerView.visibility = View.INVISIBLE
                    }
                    Activity.RESULT_CANCELED -> {
                        Toast.makeText(context, "User canceled request", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        val sp: SharedPreferences =
            requireContext().getSharedPreferences("DirPermission", Context.MODE_PRIVATE)

        val uriTree = sp.getString("uriTree", "")
        if(TextUtils.isEmpty(uriTree)) {
            binding.enMediaSaveLocation.visibility = View.VISIBLE
            binding.enMediaHint.visibility = View.VISIBLE
            binding.enPicturesRecyclerView.visibility = View.INVISIBLE
        }

        initGrid()

        registerEvents()

        model.getEncryptedFiles()

        return binding.root
    }

    private fun initGrid() {
        encryptedMediaAdapter = EncryptedGridAdapter(object : EncryptedGridAdapter.AdapterListeners {
            override fun onImageClickListener(position: Int, album: AlbumModel) {
                TODO("Not yet implemented")
            }
        })
        binding.enPicturesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.enPicturesRecyclerView.adapter = encryptedMediaAdapter
    }

    private fun registerEvents() {

        binding.enMediaSwipeToRefresh.setOnRefreshListener {
            model.getEncryptedFiles()
        }

        model.encryptedFiles.observe(viewLifecycleOwner, {
            when(it.status) {
                Status.SUCCESS -> {
                    binding.enMediaSwipeToRefresh.isRefreshing = false
                    binding.enMediaSwipeToRefresh.isEnabled = true

                    binding.enMediaProgressBar.visibility = View.GONE

                    it.data?.let { it1 -> encryptedMediaAdapter.setImages(it1.collection) }
                }
                Status.ERROR -> {
                    binding.enMediaProgressBar.visibility = View.GONE
                    binding.enPicturesRecyclerView.visibility = View.VISIBLE

                    binding.enMediaSwipeToRefresh.isRefreshing = false
                    binding.enMediaSwipeToRefresh.isEnabled = true

                    Snackbar.make(binding.root, "Something went wrong", Snackbar.LENGTH_SHORT)
                        .show()
                }
                Status.LOADING -> {
                    // Trigger spinner only if manual refresh wasn't activated
                    if (binding.enMediaSwipeToRefresh.isRefreshing) {
                        binding.enMediaProgressBar.visibility = View.INVISIBLE
                        binding.enPicturesRecyclerView.visibility = View.VISIBLE
                    } else {
                        binding.enMediaSwipeToRefresh.isEnabled = false

                        binding.enMediaProgressBar.visibility = View.VISIBLE
                        binding.enPicturesRecyclerView.visibility = View.VISIBLE
                    }
                }
            }
        })
    }
}