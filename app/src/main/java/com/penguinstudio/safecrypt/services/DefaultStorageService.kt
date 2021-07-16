package com.penguinstudio.safecrypt.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

class DefaultStorageService @Inject constructor (
    @ActivityContext context: Context,
    registry: ActivityResultRegistry) {

    private val result: MutableLiveData<Int> = MutableLiveData()

    fun chooseDefaultSaveLocation() : LiveData<Int> {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
        saveLocationCallback.launch(intent)

        return result
    }

    private val saveLocationCallback = registry.register("TESTING",ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Completed", Toast.LENGTH_SHORT).show()
            val contentResolver = context.applicationContext.contentResolver

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            contentResolver.takePersistableUriPermission(it.data?.data!!, takeFlags)

            val sp: SharedPreferences =
                context.getSharedPreferences("DirPermission", Context.MODE_PRIVATE)
            val editor = sp.edit()

            editor.putString("uriTree", it.data?.data!!.toString())
            editor.apply()
        }
        result.value = it.resultCode
    }
}