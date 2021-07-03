package com.penguinstudio.safecrypt.ui.verify

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.penguinstudio.safecrypt.R

/**
 * Verifies password / PIN given by user
 * Navigates to main dashboard if pattern is correct
 */
class PasswordUnlockFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_password_unlock, container, false)
    }
}