package com.penguinstudio.safecrypt.ui.registerFolder

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.databinding.FragmentPatternUnlockBinding
import com.penguinstudio.safecrypt.databinding.FragmentSelectEncryptionFolderBinding
import com.penguinstudio.safecrypt.services.EncryptionProcessIntentHandler
import com.penguinstudio.safecrypt.ui.verify.PatternUnlockViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SelectEncryptionFolderFragment : Fragment() {
    private lateinit var binding: FragmentSelectEncryptionFolderBinding

    @Inject
    lateinit var encryptionProcessIntentHandler: EncryptionProcessIntentHandler
    private lateinit var defaultStorageLocationSnackbar: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectEncryptionFolderBinding.inflate(layoutInflater)

        binding.encryptionFolderSelect.setOnClickListener {
            encryptionProcessIntentHandler.chooseDefaultSaveLocation().observe(viewLifecycleOwner) {
                when (it) {
                    Activity.RESULT_OK -> {
                        findNavController().navigate(R.id.action_selectEncryptionFolderFragment_to_homeFragment)
                    }
                    Activity.RESULT_CANCELED -> {
                        Snackbar.make(
                            binding.root,
                            "You must choose a default save location before you can encrypt media",
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        defaultStorageLocationSnackbar = Snackbar
            .make(requireActivity().findViewById(android.R.id.content), "Choose default storage location before encryption proceeds.", Snackbar.LENGTH_INDEFINITE)
            .setAction("Choose") {
                encryptionProcessIntentHandler.chooseDefaultSaveLocation()
            }

    }
}