package com.penguinstudio.safecrypt.ui.verify

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.PatternLockView.Dot
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.andrognito.patternlockview.utils.PatternLockUtils
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.databinding.FragmentPatternUnlockBinding


/**
 * Verifies pattern given by user
 * Navigates to main dashboard if pattern is correct
 */
class PatternUnlockFragment : Fragment() {
    private lateinit var binding: FragmentPatternUnlockBinding
    val model: PatternUnlockViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPatternUnlockBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment

        binding.patternLockView.addPatternLockListener(mPatternLockViewListener)

        arguments?.let {
            val safeArgs = PatternUnlockFragmentArgs.fromBundle(it)
            model.isRegistering = safeArgs.isRegistering

            val displayText = if(model.isRegistering) "Create a pattern" else "Enter pattern to unlock"
            binding.patternHint.text = displayText
        }
        return binding.root
    }

    /**
     * Triggered on app first time pattern creation
     * Triggered on app change pattern request
     */
    private fun isRegistering(pattern: String) {
        fun registerPattern() {
            val sp = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
            with (sp.edit()) {
                putString(getString(R.string.pattern), pattern)
                apply()
            }

            Toast.makeText(context, "Patterns match!!!", Toast.LENGTH_SHORT).show()
            // Continue to next component
        }

        if(model.isRegisteringCounter == 1) {
            binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT)

            Handler(Looper.getMainLooper()).postDelayed(
                { binding.patternLockView.clearPattern() },
                500
            )

            binding.patternHint.text = "Confirm your pattern"
            model.pattern = pattern
            model.isRegisteringCounter++
        }
        else {
            if(model.pattern == pattern) {
                registerPattern()
            }
            else {
                Toast.makeText(context, "Patterns don't match", Toast.LENGTH_SHORT).show()
                binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG)

                Handler(Looper.getMainLooper()).postDelayed(
                    { binding.patternLockView.clearPattern() },
                    500
                )

                binding.patternHint.text = "Create a pattern"
                model.pattern = ""
                model.isRegisteringCounter = 1
            }
        }
    }

    /**
     * Triggered on every app start
     * Validates user before going to dashboard
     */
    private fun isValidating(inputPattern: String) {
        val sp = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val storedPattern = sp.getString(getString(R.string.pattern), null)

        if(inputPattern == storedPattern) {
            // Correct
            binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT)
            Toast.makeText(context, "Patterns match", Toast.LENGTH_SHORT).show()

            // Navigate
        }
        else {
            // Wrong
            binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG)

            Handler(Looper.getMainLooper()).postDelayed(
                { binding.patternLockView.clearPattern() },
                500
            )
        }
    }

    private val mPatternLockViewListener: PatternLockViewListener = object : PatternLockViewListener {
        override fun onStarted() {
            // Nothing
        }

        override fun onProgress(progressPattern: List<Dot>) {
            //Nothing
        }

        override fun onComplete(pattern: List<Dot>) {
            val result = PatternLockUtils.patternToString(binding.patternLockView, pattern)

            if(model.isRegistering) {
                isRegistering(result)
            }
            else {
                isValidating(result)
            }
        }

        override fun onCleared() {
            // Nothing
        }
    }
}