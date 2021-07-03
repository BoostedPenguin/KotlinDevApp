package com.penguinstudio.safecrypt.ui.verify

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.databinding.FragmentPasswordUnlockBinding

/**
 * Verifies password / PIN given by user
 * Navigates to main dashboard if pattern is correct
 */
class PasswordUnlockFragment : Fragment() {
    lateinit var binding: FragmentPasswordUnlockBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentPasswordUnlockBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.passwordButtonEnter.setOnClickListener {
            verifyPassword(binding.passwordInput.text.toString())
        }

        // Testing
        binding.fromPasswordToFingerprint.setOnClickListener {
            registerPassword(binding.passwordInput.text.toString())
        }
    }

    private fun registerPassword(inputPassword: String) {
        if(inputPassword.isEmpty()) {
            Toast.makeText(context, "Password field is empty.", Toast.LENGTH_SHORT).show()
            return
        }

        val sp = activity?.getPreferences(Context.MODE_PRIVATE) ?: return

        with (sp.edit()) {
            putString(getString(R.string.password), inputPassword)
            apply()
        }
    }


    private fun verifyPassword(inputPassword: String) {
        if(inputPassword.isEmpty()) {
            Toast.makeText(context, "Password field is empty.", Toast.LENGTH_SHORT).show()
            return
        }

        val sp = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val password = sp.getString(getString(R.string.password), null)

        if(password == null) {
            Log.d("safeCrypt", "Password wasn't configured properly.")
            return
        }

        if(inputPassword == password) {
            // Correct password
            Toast.makeText(context, "Password is correct.", Toast.LENGTH_SHORT).show()
            return
        }
        else {
            // Wrong password
            Toast.makeText(context, "Password isn't correct.", Toast.LENGTH_SHORT).show()
            return
        }
    }
}