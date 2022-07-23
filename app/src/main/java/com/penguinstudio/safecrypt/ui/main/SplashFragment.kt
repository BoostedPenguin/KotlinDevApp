package com.penguinstudio.safecrypt.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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



    private val PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    private fun hasPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    context!!,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("PERMISSIONS", "Permission is not granted: $permission")
                return false
            }
            Log.d("PERMISSIONS", "Permission already granted: $permission")
        }
        return true
    }

    private fun askPermissions(multiplePermissionLauncher: ActivityResultLauncher<Array<String>>) {
        if (!hasPermissions(PERMISSIONS)) {
            Log.d(
                "PERMISSIONS",
                "Launching multiple contract permission launcher for ALL required permissions"
            )
            multiplePermissionLauncher.launch(PERMISSIONS)
        } else {
            Log.d("PERMISSIONS", "All permissions are already granted")
        }
    }

    private var multiplePermissionsContract: RequestMultiplePermissions? = null
    private var multiplePermissionLauncher: ActivityResultLauncher<Array<String>>? = null


    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                Log.d("PERMISSIONS", "MF FINE")


            } else {
                Log.d("PERMISSIONS", "MF DID NOT WANT FULL ACCESS")
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        multiplePermissionsContract = RequestMultiplePermissions()
        multiplePermissionLauncher = registerForActivityResult(
            RequestMultiplePermissions()
        ) { isGranted ->
            Log.d("PERMISSIONS", "Launcher result: $isGranted")
            if (isGranted.containsValue(false)) {
                Log.d(
                    "PERMISSIONS",
                    "At least one of the permissions was not granted, launching again..."
                )
                multiplePermissionLauncher?.launch(PERMISSIONS)
            }
            else {
                if (Environment.isExternalStorageManager()) {
                    //todo when permission is granted
                    Log.d("PERMISSIONS", "MF FINE")

                } else {
                    //request for the permission
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    val uri: Uri = Uri.fromParts("package", activity!!.packageName, null)
                    intent.data = uri
                    startActivity(intent)
                    Log.d("PERMISSIONS", "MF DID NOT WANT FULL ACCESS")

                }
            }
        }

        askPermissions(multiplePermissionLauncher as ActivityResultLauncher<Array<String>>)
    }

    private fun navigateToPasswordUnlock() {
        findNavController().navigate(R.id.patternUnlockFragment)
    }

}