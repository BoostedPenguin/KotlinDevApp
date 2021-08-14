package com.penguinstudio.safecrypt.ui.home.encrypted

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
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
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.EncryptedGridAdapter
import com.penguinstudio.safecrypt.databinding.FragmentEncryptedMediaBinding
import com.penguinstudio.safecrypt.models.AlbumModel
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.enPicturesRecyclerView.setOnTouchListener { v, event ->
            binding.enMediaSwipeToRefresh.isEnabled = event.pointerCount <= 1
            return@setOnTouchListener v.onTouchEvent(event)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentEncryptedMediaBinding.inflate(layoutInflater, container, false)
        (activity as AppCompatActivity).supportActionBar?.show()

        binding.enMediaSaveLocation.setOnClickListener {
            encryptionProcessIntentHandler.chooseDefaultSaveLocation().observe(viewLifecycleOwner, {
                when(it) {
                    Activity.RESULT_OK -> {
                        binding.enMediaSaveLocation.visibility = View.GONE
                        binding.enMediaHint.visibility = View.GONE
                        binding.enPicturesRecyclerView.visibility = View.VISIBLE
                    }
                    Activity.RESULT_CANCELED -> {
                        Snackbar.make(binding.root, "You must choose a default save location before you can encrypt media", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }
            })
        }

        checkForSaveLocation()

        initGrid()

        registerEvents()

        model.getEncryptedFiles()

        return binding.root
    }

    private fun checkForSaveLocation() {
        val sp: SharedPreferences =
            requireContext().getSharedPreferences("DirPermission", Context.MODE_PRIVATE)

        val uriTree = sp.getString("uriTree", "")
        if(TextUtils.isEmpty(uriTree)) {
            binding.enMediaSaveLocation.visibility = View.VISIBLE
            binding.enMediaHint.visibility = View.VISIBLE
            binding.enPicturesRecyclerView.visibility = View.INVISIBLE
        }
    }

    private fun initGrid() {
        encryptedMediaAdapter = EncryptedGridAdapter(object : EncryptedGridAdapter.AdapterListeners {
            override fun onImageClickListener(position: Int, album: AlbumModel) {
                TODO("Not yet implemented")
            }
        })


        val sharedPref = context?.getSharedPreferences(getString(R.string.main_shared_pref), Context.MODE_PRIVATE)
        val columns =
            sharedPref?.getInt(context?.getString(R.string.grid_columns), 3) ?: 3

        // Prevent redrawing recyclerview if current column is same size as requested
        binding.enPicturesRecyclerView.layoutManager = GridLayoutManager(requireContext(), columns)

        binding.enPicturesRecyclerView.adapter = encryptedMediaAdapter

        binding.enPicturesRecyclerView.setHasFixedSize(true)

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