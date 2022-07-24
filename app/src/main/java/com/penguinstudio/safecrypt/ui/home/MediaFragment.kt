package com.penguinstudio.safecrypt.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.RecoverableSecurityException
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.google.android.material.snackbar.Snackbar
import com.penguinstudio.safecrypt.MainActivity
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.AlbumMediaAdapter
import com.penguinstudio.safecrypt.databinding.FragmentPicturesBinding
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.services.EncryptionProcessIntentHandler
import com.penguinstudio.safecrypt.services.glide_service.GlideApp
import com.penguinstudio.safecrypt.services.glide_service.GlideRequest
import com.penguinstudio.safecrypt.utilities.EncryptionStatus
import com.penguinstudio.safecrypt.utilities.Status
import com.penguinstudio.safecrypt.utilities.getGlideRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class MediaFragment : Fragment(), LifecycleObserver {
    private lateinit var binding: FragmentPicturesBinding
    private lateinit var photoAdapter: AlbumMediaAdapter
    private val _model: GalleryViewModel by activityViewModels()

    private lateinit var fullRequest: GlideRequest<Drawable>

    private lateinit var defaultStorageLocationSnackbar: Snackbar

    @Inject
    lateinit var encryptionProcessIntentHandler: EncryptionProcessIntentHandler
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

        intentSenderLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if(it.resultCode == RESULT_OK) {
                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    lifecycleScope.launch {
                        deletePhotoFromExternalStorage(deletedImageUri ?: return@launch)
                    }
                }
                Toast.makeText(context, "Photo deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Photo couldn't be deleted", Toast.LENGTH_SHORT).show()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun onBackPress() {
        // Handle the back button event
        if(model.itemSelectionMode.value == true) {
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

        fullRequest = this.getGlideRequest()

        (activity as AppCompatActivity).supportActionBar?.show()

        initGrid()

        registerEvents()

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Instantiate snack-bars
        defaultStorageLocationSnackbar = Snackbar
            .make(binding.root, "Choose default storage location before encryption proceeds.", Snackbar.LENGTH_INDEFINITE)
            .setAction("Choose") {
                encryptionProcessIntentHandler.chooseDefaultSaveLocation()
            }

        binding.picturesRecyclerView.setOnTouchListener { v, event ->
            binding.picturesSwipeToRefresh.isEnabled = event.pointerCount <= 1
            return@setOnTouchListener v.onTouchEvent(event)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_select_all -> {
                model.addAllMediaToSelection(photoAdapter.getImages())
                (activity as AppCompatActivity).supportActionBar?.title = "${model.selectedItems.size} selected"
                photoAdapter.notifyDataSetChanged()
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
        photoAdapter = AlbumMediaAdapter(object: AlbumMediaAdapter.AdapterListeners {
            override fun onClickListener(position: Int, media: MediaModel) {


                // If in selection mode add / remove, else trigger normal action
                if(model.itemSelectionMode.value == true) {

                    if(model.selectedItems.contains(media)) {
                        model.removeMediaFromSelection(position, media)

                        if(model.selectedItems.size == 0) {

                            exitSelectMode()

                            return
                        }
                    }
                    else {
                        model.addMediaToSelection(media)
                    }

                    if (model.itemSelectionMode.value == true) {
                        (activity as AppCompatActivity).supportActionBar?.title = "${model.selectedItems.size} selected"
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
                if(model.itemSelectionMode.value == true) return
                photoAdapter.toggleSelectionMode(true, position)

                model.itemSelectionMode.value = true

                model.addMediaToSelection(media)
                (activity as AppCompatActivity).supportActionBar?.title = "${model.selectedItems.size} selected"

                // Notify adapter that this item has changed
                activity?.invalidateOptionsMenu()
            }
        }, fullRequest)

        val sharedPref = context?.getSharedPreferences(getString(R.string.main_shared_pref), Context.MODE_PRIVATE)
        val columns =
            sharedPref?.getInt(context?.getString(R.string.grid_columns), 3) ?: 3



        // Prevent redrawing recyclerview if current column is same size as requested
        binding.picturesRecyclerView.layoutManager = GridLayoutManager(requireContext(), columns)

        binding.picturesRecyclerView.adapter = photoAdapter
        
        binding.picturesRecyclerView.setHasFixedSize(true)
    }

    private fun registerEvents() {
        /**
         * If the selected items become 0,
         * Disable selection mode
         */

        binding.picturesSwipeToRefresh.setOnRefreshListener {
            exitSelectMode()
            model.getMedia()
        }
        model.selectedAlbum.observe(viewLifecycleOwner) {
            if (it == null) return@observe

            when (it.status) {
                Status.SUCCESS -> {
                    binding.picturesProgressBar.visibility = View.GONE
                    binding.picturesSwipeToRefresh.isRefreshing = false

                    // If this album doesn't exist anymore // No media inside it
                    if (it.data == null || it.data.albumMedia.size == 0) {
                        findNavController().popBackStack()
                        return@observe
                    }

                    photoAdapter.setImages(
                        it.data.albumMedia,
                        model.itemSelectionMode.value ?: false
                    )

                    if (model.itemSelectionMode.value == true) {
                        (activity as AppCompatActivity).supportActionBar?.title =
                            "${model.selectedItems.size} selected"
                    } else {
                        (activity as AppCompatActivity?)?.supportActionBar?.title =
                            it.data.name
                    }
                }
                Status.ERROR -> {
                    binding.picturesProgressBar.visibility = View.GONE
                    Toast.makeText(context, "${it.message}", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                Status.LOADING -> {
                    if (!binding.picturesSwipeToRefresh.isRefreshing) {
                        binding.picturesProgressBar.visibility = View.VISIBLE
                    }
                }
            }
        }

        model.encryptionStatus.observe(viewLifecycleOwner) { it ->
            if (it == null) return@observe

            when (it.status) {
                EncryptionStatus.LOADING -> {
                    binding.picturesProgressBar.visibility = View.VISIBLE
                }
                EncryptionStatus.REQUEST_STORAGE -> {
                    binding.picturesProgressBar.visibility = View.GONE

                    encryptionProcessIntentHandler.chooseDefaultSaveLocation(viewLifecycleOwner)
                        .observe(viewLifecycleOwner) { result ->
                            when (result) {
                                Activity.RESULT_OK -> {
                                    model.encryptSelectedMedia()
                                }
                                Activity.RESULT_CANCELED -> {
                                    defaultStorageLocationSnackbar.show()
                                }
                            }
                        }
                }
                EncryptionStatus.DELETE_RECOVERABLE -> {
                    binding.picturesProgressBar.visibility = View.GONE
                    if (it.intentSender == null) return@observe

                    encryptionProcessIntentHandler.deleteOriginalFile(it.intentSender)
                        .observe(viewLifecycleOwner) { result ->
                            when (result) {
                                Activity.RESULT_OK -> {
                                    //Successfully deleted
                                }
                                Activity.RESULT_CANCELED -> {
                                    Snackbar.make(
                                        binding.root,
                                        "Original media item wasn't deleted. Please delete it manually.",
                                        Snackbar.LENGTH_INDEFINITE
                                    ).show()
                                }
                            }
                        }
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
        }
    }


    private fun exitSelectMode() {
        // Prevent invalidating if not in selection mode already
        if(model.itemSelectionMode.value == true) {

            (activity as AppCompatActivity?)?.supportActionBar?.title = model.selectedAlbum.value?.data?.name

            if(defaultStorageLocationSnackbar.isShown) defaultStorageLocationSnackbar.dismiss()
            activity?.invalidateOptionsMenu()
            model.itemSelectionMode.value = false
            model.clearSelections()
            photoAdapter.toggleSelectionMode(false)
        }
    }
    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var deletedImageUri: Uri? = null


    private suspend fun deletePhotoFromExternalStorage(photoUri: Uri) {
        try {
            val g = context!!.contentResolver.delete(photoUri, null, null)
            val s = g
            Log.e("FUCK", g.toString())
        } catch (e: SecurityException) {
            val intentSender = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    MediaStore.createDeleteRequest(context!!.contentResolver, listOf(photoUri)).intentSender
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    val recoverableSecurityException = e as? RecoverableSecurityException
                    recoverableSecurityException?.userAction?.actionIntent?.intentSender
                }
                else -> null
            }
            intentSender?.let { sender ->
                intentSenderLauncher.launch(
                    IntentSenderRequest.Builder(sender).build()
                )
            }
        }
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

        if(model.itemSelectionMode.value == true) {
            activity?.menuInflater?.inflate(R.menu.item_selected_menu , menu)
        }
        else {
            activity?.menuInflater?.inflate(R.menu.main_menu , menu)
        }
    }
}