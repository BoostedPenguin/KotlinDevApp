package com.penguinstudio.safecrypt.ui.settings

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.penguinstudio.safecrypt.MainActivity
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.services.EncryptionProcessIntentHandler
import com.penguinstudio.safecrypt.services.GCMEncryptionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class SettingsFragment : Fragment(),
    SharedPreferences.OnSharedPreferenceChangeListener, Preference.SummaryProvider<ListPreference> {



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        (activity as AppCompatActivity).supportActionBar?.show()
        menu.clear()
        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings, ChildSettingsFragment())
                .commit()
        }

        PreferenceManager.getDefaultSharedPreferences(context)
            .registerOnSharedPreferenceChangeListener(this)

    }


    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(context)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun provideSummary(preference: ListPreference?): CharSequence =
        if (preference?.key == getString(R.string.dark_mode)) preference.entry
        else "Unknown Preference"

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val darkModeString = getString(R.string.dark_mode)
        MainActivity.pausePattern()
        key?.let {
            if (it == darkModeString) sharedPreferences?.let { pref ->
                val darkModeValues = resources.getStringArray(R.array.dark_mode_values)
                when (pref.getString(darkModeString, darkModeValues[0])) {
                    darkModeValues[0] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    darkModeValues[1] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    darkModeValues[2] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }
        }
    }
    @AndroidEntryPoint
    class ChildSettingsFragment : PreferenceFragmentCompat() {

        @Inject
        lateinit var encryptionProcessIntentHandler: EncryptionProcessIntentHandler

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {

            if(preference?.key == "importKey") {


                activity?.let {
                    val builder = AlertDialog.Builder(it)
                    // Get the layout inflater
                    val inflater = requireActivity().layoutInflater;

                    // Inflate and set the layout for the dialog
                    // Pass null as the parent view because its going in the dialog layout
                    val dialogView = inflater.inflate(R.layout.import_key_dialog, null)
                    val sharedPref = requireContext().getSharedPreferences(requireContext().getString(R.string.main_shared_pref), Context.MODE_PRIVATE)

                    dialogView.findViewById<ImageButton>(R.id.encryptionKeyCopy).setOnClickListener {
                        val clipboard = getSystemService(
                            requireContext(),
                            ClipboardManager::class.java
                        ) ?: return@setOnClickListener


                        val keyStr =
                            sharedPref.getString(requireContext().getString(R.string.ENCRYPT_KEY), "")

                        val clip = ClipData.newPlainText("encryption_key_content", keyStr)
                        clipboard.setPrimaryClip(clip)

                        Toast.makeText(context, "Copied key", Toast.LENGTH_SHORT).show()
                    }

                    dialogView.findViewById<TextView>(R.id.encryptionKeyText).text = sharedPref
                        .getString(requireContext().getString(R.string.ENCRYPT_KEY), "")

                    dialogView.findViewById<Button>(R.id.newEncryptionSave).setOnClickListener {

                        val keyText =
                            dialogView.findViewById<EditText>(R.id.newEncryptionKeyText).text.toString()

                        if(keyText.length != 32) {
                            Toast.makeText(context, "Key needs to be exactly 32 characters!", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        GCMEncryptionService.putUuidKey(keyText, requireContext())

                        dialogView.findViewById<TextView>(R.id.encryptionKeyText).text = sharedPref
                            .getString(requireContext().getString(R.string.ENCRYPT_KEY), "")

                        Toast.makeText(context, "Encryption key changed!", Toast.LENGTH_SHORT).show()

                    }
                    builder.setView(dialogView)

                    builder.show()

                } ?: throw IllegalStateException("Activity cannot be null")

                return true
            }

            if(preference?.key == "privacy") {
                return try {
                    val url = "https://atodorov.netlify.app/#/safecrypt"
                    val uri: Uri = Uri.parse(url)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent);
                    true
                } catch (e: Exception) {
                    Toast.makeText(context, "Error opening link!", Toast.LENGTH_SHORT).show()
                    false
                }
            }

            if(preference?.key == "encryptFileDirectory") {
                encryptionProcessIntentHandler.chooseDefaultSaveLocation().observe(viewLifecycleOwner) {
                    when (it) {
                        Activity.RESULT_OK -> {
                            Snackbar.make(
                                requireView(),
                                "Successfully changed encryption file directory.",
                                Snackbar.LENGTH_SHORT
                            )
                                .show()
                        }
                        Activity.RESULT_CANCELED -> {
                            Snackbar.make(
                                requireView(),
                                "You did not change your existing file encryption directory.",
                                Snackbar.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                }
            }

            return true
        }
    }
}
