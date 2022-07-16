package com.penguinstudio.safecrypt.ui.home.encrypted

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.EncryptedGridAdapter
import com.penguinstudio.safecrypt.databinding.FragmentEncryptedMediaBinding
import com.penguinstudio.safecrypt.models.AlbumModel
import com.penguinstudio.safecrypt.models.EncryptedModel
import com.penguinstudio.safecrypt.models.MediaType
import com.penguinstudio.safecrypt.services.EncryptionProcessIntentHandler
import com.penguinstudio.safecrypt.services.glide_service.GlideApp
import com.penguinstudio.safecrypt.services.glide_service.GlideRequest
import com.penguinstudio.safecrypt.ui.home.HomeFragmentDirections
import com.penguinstudio.safecrypt.ui.home.SelectedMediaFragmentDirections
import com.penguinstudio.safecrypt.utilities.EncryptionStatus
import com.penguinstudio.safecrypt.utilities.MediaMode
import com.penguinstudio.safecrypt.utilities.Status
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EncryptedMediaFragment : Fragment(), LifecycleObserver {
    private val model: EncryptedMediaViewModel by activityViewModels()
    private lateinit var binding: FragmentEncryptedMediaBinding
    private lateinit var encryptedMediaAdapter: EncryptedGridAdapter
    private lateinit var fullRequest: GlideRequest<Drawable>

    @Inject
    lateinit var encryptionProcessIntentHandler: EncryptionProcessIntentHandler
    private lateinit var defaultStorageLocationSnackbar: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    if(model.itemSelectionMode.value == true) {
                        model.itemSelectionMode.postValue(false)
                    } else {
                        isEnabled = false
                        activity?.onBackPressed()
                    }
                }
            }

        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
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

        // Instantiate snack-bars
        defaultStorageLocationSnackbar = Snackbar
            .make(requireActivity().findViewById(android.R.id.content), "Choose default storage location before encryption proceeds.", Snackbar.LENGTH_INDEFINITE)
            .setAction("Choose") {
                encryptionProcessIntentHandler.chooseDefaultSaveLocation()
            }

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
        setHasOptionsMenu(true)

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

        fullRequest = GlideApp.with(this)
            .asDrawable()
            .placeholder(R.drawable.ic_baseline_image_24)
            .fitCenter()
            .diskCacheStrategy(DiskCacheStrategy.ALL)

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
            override fun onClickListener(position: Int, media: EncryptedModel) {

                // If in selection mode add / remove, else trigger normal action
                if(model.itemSelectionMode.value == true) {

                    if(model.selectedItems.contains(media)) {
                        model.removeMediaFromSelection(position, media)

                        if(model.selectedItems.size == 0) {

                            encryptedMediaAdapter.toggleSelectionMode(false)

                            (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.app_name)

                            model.itemSelectionMode.postValue(false)

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
                    encryptedMediaAdapter.notifyItemChanged(position)
                }
                else {
                    //TODO Enlarge image
                    if(media.mediaType == MediaType.VIDEO) {
                        Snackbar.make(requireActivity().findViewById(android.R.id.content), "(BETA) Currently videos take a lot of time to preview", Snackbar.LENGTH_LONG)
                            .show()
                    }
                    model.setSelectedMedia(media)
                    val action = HomeFragmentDirections.actionHomeFragmentToSelectedPicture(MediaMode.ENCRYPTED_MEDIA)
                    findNavController().navigate(action)
                }
            }

            override fun onLongClickListener(position: Int, media: EncryptedModel) {
                if(model.itemSelectionMode.value == true) return
                encryptedMediaAdapter.toggleSelectionMode(true, position)

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

        binding.enPicturesRecyclerView.adapter = encryptedMediaAdapter
        binding.enPicturesRecyclerView.layoutManager = GridLayoutManager(requireContext(), columns)

    }

    private fun registerEvents() {

        binding.enMediaSwipeToRefresh.setOnRefreshListener {
            model.itemSelectionMode.postValue(false)

            model.getEncryptedFiles()
        }

        model.itemSelectionMode.observe(viewLifecycleOwner, {
            if(!it) exitSelectMode()
        })

        model.encryptedFiles.observe(viewLifecycleOwner, {
            when(it.status) {
                Status.SUCCESS -> {
                    binding.enMediaSwipeToRefresh.isRefreshing = false
                    binding.enMediaSwipeToRefresh.isEnabled = true

                    binding.enMediaProgressBar.visibility = View.GONE

                    it.data?.let { collectionResponse ->
                        if(collectionResponse.collection.size == 0) {
                            binding.enMediaHint.visibility = View.VISIBLE
                            binding.enMediaHint.text = "No encrypted files found."
                        } else {
                            binding.enMediaHint.visibility = View.GONE
                        }
                        encryptedMediaAdapter.setImages(collectionResponse.collection)
                    }
                }
                Status.ERROR -> {
                    binding.enMediaProgressBar.visibility = View.GONE
                    binding.enPicturesRecyclerView.visibility = View.VISIBLE

                    binding.enMediaSwipeToRefresh.isRefreshing = false
                    binding.enMediaSwipeToRefresh.isEnabled = true

                    Snackbar.make(requireActivity().findViewById(android.R.id.content), "Something went wrong", Snackbar.LENGTH_SHORT)
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

        model.encryptionStatus.observe(viewLifecycleOwner, {
            if(it == null) return@observe

            when(it.status) {
                EncryptionStatus.LOADING -> {
                    binding.enMediaProgressBar.visibility = View.VISIBLE
                }
                EncryptionStatus.REQUEST_STORAGE -> {
                    binding.enMediaProgressBar.visibility = View.GONE

                    encryptionProcessIntentHandler.chooseDefaultSaveLocation(viewLifecycleOwner)
                        .observe(viewLifecycleOwner, { result ->
                            when(result) {
                                Activity.RESULT_OK -> {
                                    model.decryptSelectedMedia()
                                }
                                Activity.RESULT_CANCELED -> {
                                    defaultStorageLocationSnackbar.show()
                                }
                            }
                        })
                }
                EncryptionStatus.DELETE_RECOVERABLE -> {
                    binding.enMediaProgressBar.visibility = View.GONE
                    if(it.intentSender == null) return@observe

                    encryptionProcessIntentHandler.deleteOriginalFile(it.intentSender)
                        .observe(viewLifecycleOwner, { result ->
                            when(result) {
                                Activity.RESULT_OK -> {
                                    //Successfully deleted
                                }
                                Activity.RESULT_CANCELED -> {
                                    Snackbar.make(binding.root,
                                        "Original media item wasn't deleted. Please delete it manually.",
                                        Snackbar.LENGTH_INDEFINITE).show()
                                }
                            }
                        })
                }
                EncryptionStatus.OPERATION_COMPLETE -> {
                    model.itemSelectionMode.postValue(false)

                    // Fetch media, it will update adapter automatically
                    model.getEncryptedFiles()

                    binding.enMediaProgressBar.visibility = View.GONE
                }
                EncryptionStatus.ERROR -> {
                    binding.enMediaProgressBar.visibility = View.GONE
                }
            }
            model.clearEncryptionStatus()
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
        super.onPrepareOptionsMenu(menu)

        if(model.itemSelectionMode.value == true) {
            activity?.menuInflater?.inflate(R.menu.encrypted_item_selected_menu , menu)
            (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        else {
            activity?.menuInflater?.inflate(R.menu.main_menu , menu)
            (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when(item.itemId) {
            android.R.id.home -> {
                onBackPress()
                true
            }
            R.id.action_decrypt -> {
                model.decryptSelectedMedia()
                true
            }
            R.id.action_encrypt_select_all -> {
                model.addAllMediaToSelection(encryptedMediaAdapter.getImages())
                (activity as AppCompatActivity).supportActionBar?.title = "${model.selectedItems.size} selected"
                encryptedMediaAdapter.notifyDataSetChanged()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onBackPress() {
        // Handle the back button event
        if(model.itemSelectionMode.value == true) {
            model.itemSelectionMode.postValue(false)
        }
    }

    private fun exitSelectMode() {
        // Prevent invalidating if not in selection mode already
        activity?.invalidateOptionsMenu()
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.app_name)
        model.clearSelections()
        encryptedMediaAdapter.toggleSelectionMode(false)
    }
}