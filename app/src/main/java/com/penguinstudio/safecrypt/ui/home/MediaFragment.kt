package com.penguinstudio.safecrypt.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.PhotoGridAdapter
import com.penguinstudio.safecrypt.databinding.FragmentPicturesBinding
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.services.DefaultStorageService
import com.penguinstudio.safecrypt.utilities.EncryptionStatus
import com.penguinstudio.safecrypt.utilities.Status
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MediaFragment : Fragment(), LifecycleObserver {
    private lateinit var binding: FragmentPicturesBinding
    private lateinit var photoAdapter: PhotoGridAdapter
    private val _model: GalleryViewModel by activityViewModels()

    @Inject
    lateinit var defaultStorageService: DefaultStorageService
    private val model: IPicturesViewModel
        get() {
            return _model
        }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreated(){
        (activity as AppCompatActivity?)?.supportActionBar?.title = model.selectedAlbum.value?.data?.name
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
        if(model.itemSelectionMode) {
            exitSelectMode()
            return
        }
        findNavController().popBackStack()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentPicturesBinding.inflate(layoutInflater, container, false)
        setHasOptionsMenu(true)

        (activity as AppCompatActivity).supportActionBar?.show()

        initGrid()

        registerEvents()

        return binding.root
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_select_all -> {
                model.addAllMediaToSelection(photoAdapter.getImages())
                photoAdapter.notifyItemRangeChanged(0, photoAdapter.itemCount)
                true
            }
            R.id.action_encrypt -> {
                model.encryptSelectedMedia()
                true
            }
            android.R.id.home -> {
                onBackPress()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun initGrid() {
        photoAdapter = PhotoGridAdapter(object: PhotoGridAdapter.AdapterListeners {
            override fun onClickListener(position: Int, media: MediaModel) {

                // If in selection mode add / remove, else trigger normal action
                if(model.itemSelectionMode) {

                    if(model.selectedItems.value?.contains(media) == true) {
                        model.removeMediaFromSelection(position, media)
                        media.isSelected = false
                    }
                    else {
                        model.addMediaToSelection(position, media)
                        media.isSelected = true
                    }

                    // Notify adapter that this item has changed
                    photoAdapter.notifyItemChanged(position)
                }
                else {
                    model.setSelectedMedia(media)
                    findNavController().navigate(R.id.action_picturesFragment_to_selectedPicture)
                }
            }
            override fun onLongClickListener(position: Int, media: MediaModel) {
                if(model.itemSelectionMode) return
                photoAdapter.toggleSelectionMode(true)

                model.itemSelectionMode = true

                model.addMediaToSelection(position, media)
                (activity as AppCompatActivity?)?.supportActionBar?.title = "Select Media"
                // Notify adapter that this item has changed
                activity?.invalidateOptionsMenu()
                media.isSelected = true
                photoAdapter.notifyItemChanged(position)
            }
        })

        when(resources.configuration.orientation) {

            Configuration.ORIENTATION_LANDSCAPE -> {
                binding.picturesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 5)
            }
            else -> {
                binding.picturesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

            }
        }
        binding.picturesRecyclerView.adapter = photoAdapter
    }

    private fun registerEvents() {
        /**
         * If the selected items become 0,
         * Disable selection mode
         */
        binding.picturesSwipeToRefresh.setOnRefreshListener {
            model.getMedia()
        }

        model.albums.observe(viewLifecycleOwner, {
            when(it.status) {
                Status.LOADING -> {
                    // Do nothing
                }
                else -> {
                    binding.picturesSwipeToRefresh.isRefreshing = false
                    binding.picturesSwipeToRefresh.isEnabled = true
                }
            }
        })

        model.selectedAlbum.observe(viewLifecycleOwner, {
            if(it == null) return@observe

            when(it.status) {
                Status.SUCCESS -> {
                    binding.picturesProgressBar.visibility = View.GONE

                    // If this album doesn't exist anymore // No media inside it
                    if(it.data == null || it.data.albumMedia.size == 0) {
                        findNavController().popBackStack()
                        return@observe
                    }

                    photoAdapter.setImages(it.data.albumMedia)
                    (activity as AppCompatActivity?)?.supportActionBar?.title =
                        it.data.name
                }
                Status.ERROR -> {
                    binding.picturesProgressBar.visibility = View.GONE
                    Toast.makeText(context, "${it.message}", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                Status.LOADING -> {
                    binding.picturesProgressBar.visibility = View.VISIBLE
                }
            }
        })


        model.selectedItems.observe(viewLifecycleOwner, {
            if(it != null && it.size == 0) {
                photoAdapter.toggleSelectionMode(false)

                exitSelectMode()
            }
        })


        model.encryptionStatus.observe(viewLifecycleOwner, {
            if(it == null) return@observe

            when(it.status) {
                EncryptionStatus.LOADING -> {
                    binding.picturesProgressBar.visibility = View.VISIBLE
                }
                EncryptionStatus.REQUEST_STORAGE -> {
                    defaultStorageService.chooseDefaultSaveLocation().observe(viewLifecycleOwner, { result ->
                        //TODO On chosen location result
                        when(result) {
                            Activity.RESULT_OK -> {

                            }
                            Activity.RESULT_CANCELED -> {

                            }
                        }
                        binding.picturesProgressBar.visibility = View.GONE
                    })
                }
                EncryptionStatus.DELETE_RECOVERABLE -> {
                    deleteOriginalFileLauncher.launch(it.intentSender)
                    binding.picturesProgressBar.visibility = View.GONE

                }
                EncryptionStatus.OPERATION_COMPLETE -> {

                    exitSelectMode()

                    // Fetch media, it will update adapter automatically
                    model.getMedia()

                    binding.picturesProgressBar.visibility = View.GONE

                }
                EncryptionStatus.ERROR -> {
                    binding.picturesProgressBar.visibility = View.GONE
                }
            }
            model.clearEncryptionStatus()
        })
    }

    private var deleteOriginalFileLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Call to function to continue encryption process
            Log.d("callStack", "Currently, accepted delete request")
        }
    }

    private fun exitSelectMode() {
        (activity as AppCompatActivity?)?.supportActionBar?.title = model.selectedAlbum.value?.data?.name

        activity?.invalidateOptionsMenu()
        model.itemSelectionMode = false
        model.clearSelections()
        photoAdapter.toggleSelectionMode(false)
    }

    /**
     * Triggers only once when fragment loads
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }

    /**
     * Triggers programmatically and on every menu item click
     */
    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
        super.onPrepareOptionsMenu(menu)

        if(model.itemSelectionMode) {
            activity?.menuInflater?.inflate(R.menu.item_selected_menu , menu)
        }
        else {
            activity?.menuInflater?.inflate(R.menu.main_menu , menu)
        }
    }
}