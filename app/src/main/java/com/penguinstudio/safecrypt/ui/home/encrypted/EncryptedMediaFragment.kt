package com.penguinstudio.safecrypt.ui.home.encrypted

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.PhotoGridAdapter
import com.penguinstudio.safecrypt.databinding.FragmentEncryptedMediaBinding
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.ui.home.EncryptedMediaViewModel
import com.penguinstudio.safecrypt.ui.home.GalleryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptedMediaFragment : Fragment(), LifecycleObserver {
    private val model: EncryptedMediaViewModel by activityViewModels()
    private lateinit var binding: FragmentEncryptedMediaBinding
    private lateinit var encryptedMediaAdapter: PhotoGridAdapter

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

        //chooseDefaultSaveLocation()
        binding.enPicturesSaveLocation.setOnClickListener {
            chooseDefaultSaveLocation()
        }

        val sp: SharedPreferences =
            requireContext().getSharedPreferences("DirPermission", Context.MODE_PRIVATE)

        val uriTree = sp.getString("uriTree", "")
        if(TextUtils.isEmpty(uriTree)) {
            binding.enPicturesSaveLocation.visibility = View.VISIBLE
            binding.enPicturesHint.visibility = View.VISIBLE
            binding.enPicturesRecyclerView.visibility = View.INVISIBLE
        }

        initGrid()

        return binding.root
    }

    private fun initGrid() {
        // Non encrypted file stored images (MediaStore controlled)
        encryptedMediaAdapter = PhotoGridAdapter(object : PhotoGridAdapter.AdapterListeners {
            override fun onClickListener(position: Int, media: MediaModel) {
                TODO("Not yet implemented")
            }

            override fun onLongClickListener(position: Int, media: MediaModel) {
                TODO("Not yet implemented")
            }
        })
        binding.enPicturesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.enPicturesRecyclerView.adapter = encryptedMediaAdapter
    }

    private fun chooseDefaultSaveLocation() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
        saveLocationCallback.launch(intent)
    }

    private val saveLocationCallback = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Completed", Toast.LENGTH_SHORT).show()
            val contentResolver = requireContext().applicationContext.contentResolver

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            contentResolver.takePersistableUriPermission(it.data?.data!!, takeFlags)

            val sp: SharedPreferences =
                requireContext().getSharedPreferences(getString(R.string.ENC_STORAGE_DIR_PERMISSIONS), Context.MODE_PRIVATE)
            val editor = sp.edit()

            editor.putString(getString(R.string.ENC_DIR_ROOT_TREE), it.data?.data!!.toString())
            editor.apply()

            binding.enPicturesSaveLocation.visibility = View.VISIBLE
            binding.enPicturesHint.visibility = View.VISIBLE
            binding.enPicturesRecyclerView.visibility = View.INVISIBLE
        }
    }
}