package com.penguinstudio.safecrypt.ui.main

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.common.reflect.Reflection.getPackageName
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.databinding.FragmentSplashBinding


/**
 * Initial loaded fragment
 * Checks for required permissions on every app load.
 * Checks if it's the first time use of the app.
 */
class SplashFragment : Fragment() {
    private lateinit var binding: FragmentSplashBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSplashBinding.inflate(inflater, container, false)

        return binding.root
    }





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionsLauncher = registerForActivityResult(RequestMultiplePermissions()) { permissions ->
            readPermissionGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted
            writePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: writePermissionGranted

            if(!readPermissionGranted || !writePermissionGranted) {
                Toast.makeText(requireContext(), "This app requires read and write permissions to work", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            navigateToPasswordUnlock()
        }

        if (updateOrRequestPermissions()) {
            navigateToPasswordUnlock()
        }
    }

    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
    private fun updateOrRequestPermissions() : Boolean {
        val hasReadPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        readPermissionGranted = hasReadPermission
        writePermissionGranted = hasWritePermission

        val permissionsToRequest = mutableListOf<String>()
        if(!writePermissionGranted) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if(!readPermissionGranted) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if(permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
            return false
        }

        return true
    }

    private fun navigateToPasswordUnlock() {
        findNavController().navigate(R.id.patternUnlockFragment)
    }

}