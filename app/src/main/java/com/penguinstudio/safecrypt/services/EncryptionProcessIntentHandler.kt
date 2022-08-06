package com.penguinstudio.safecrypt.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.penguinstudio.safecrypt.MainActivity
import com.penguinstudio.safecrypt.utilities.Event
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

@FragmentScoped
class EncryptionProcessIntentHandler @Inject constructor (
    @ActivityContext context: Context,
    registry: ActivityResultRegistry) {

    private val _saveLocationResult = MutableLiveData<Int>()

    private val deleteFileResult: MutableLiveData<Int> = MutableLiveData()

    /**
     * @param lifecycleOwner if supplied, will remove all observers attached to it
     */
    fun chooseDefaultSaveLocation(lifecycleOwner: LifecycleOwner? = null) : LiveData<Int> {
        if(lifecycleOwner != null)
            _saveLocationResult.removeObservers(lifecycleOwner)

        MainActivity.pausePattern()
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
        saveLocationCallback.launch(intent)

        return _saveLocationResult
    }

    fun deleteOriginalFile(intentSenderRequest: IntentSenderRequest) : LiveData<Int> {
        MainActivity.pausePattern()

        deleteOriginalFileLauncher.launch(intentSenderRequest)

        return deleteFileResult
    }

    private var deleteOriginalFileLauncher = registry.register("Test", ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Call to function to continue encryption process
            Log.d("callStack", "Currently, accepted delete request")
        }
        deleteFileResult.value = result.resultCode
    }

    private val saveLocationCallback = registry.register("TESTING",ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Completed", Toast.LENGTH_SHORT).show()
            val contentResolver = context.applicationContext.contentResolver

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            contentResolver.takePersistableUriPermission(it.data?.data!!, takeFlags)

            val selectedFolderPathUri = it.data!!.data!!

            val sp: SharedPreferences =
                context.getSharedPreferences("DirPermission", Context.MODE_PRIVATE)
            val editor = sp.edit()

            val root = DocumentFile.fromTreeUri(context, selectedFolderPathUri)

            val path = if(root?.findFile("Encrypted") == null) {
                root?.createDirectory("Encrypted")
            }
            else {
                root.findFile("Encrypted")
            }

            editor.putString("uriTree", path?.uri.toString())
            editor.apply()
        }
        _saveLocationResult.value = it.resultCode
    }
}