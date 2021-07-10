package com.penguinstudio.safecrypt.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
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


    override fun onStart() {
        super.onStart()
        checkPermissionsOnStart()
    }

    private fun navigateToPasswordUnlock() {

        val sp = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val storedPattern = sp.getString(getString(R.string.pattern), null)

        // Checks if user is a first timer
        val action: NavDirections = if(storedPattern == null) {
            SplashFragmentDirections.actionSplashFragmentToPatternUnlockFragment(true)
        } else {
            SplashFragmentDirections.actionSplashFragmentToPatternUnlockFragment(false)
        }

        findNavController().navigate(action)
    }

    /**
     * Validates if storage permissions were given
     * Requests them at runtime if they are required
     */
    private fun checkPermissionsOnStart() {
        Dexter.withActivity(activity)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if(report.areAllPermissionsGranted()) {
                        navigateToPasswordUnlock()
                    }
                    else {
                        showSettingsDialog();
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    // this method is called when user grants some
                    // permission and denies some of them.
                    token?.continuePermissionRequest();
                }

            }).check()
    }

    /**
     * Opens app settings to let user change permissions
     * Closes app if user refuses
     */
    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(requireContext())

        builder.setTitle("Require permissions")
        builder.setMessage("This app needs storage permissions to operate. You can grant them in app settings.");
        builder.setPositiveButton(
            "Go to settings"
        ) { dialog, _ ->

            dialog.cancel()

            // below is the intent from which we
            // are redirecting our user.
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", context?.packageName, null)
            intent.data = uri
            context?.startActivity(intent)
        }
        builder.setNegativeButton(
            "Exit"
        ) { dialog, _ -> // this method is called when
            // user click on negative button.
            activity?.finishAndRemoveTask();

            dialog.cancel()
        }

        builder.setCancelable(false)
        builder.show()
    }
}