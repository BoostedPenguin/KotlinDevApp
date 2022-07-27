package com.penguinstudio.safecrypt.ui.verify

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.PatternLockView.Dot
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.andrognito.patternlockview.utils.PatternLockUtils
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.databinding.FragmentPatternUnlockBinding
import com.penguinstudio.safecrypt.services.CBCEncryptionService
import com.penguinstudio.safecrypt.services.GCMEncryptionService
import com.penguinstudio.safecrypt.ui.main.SplashFragmentDirections


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

        val sp = activity?.getPreferences(Context.MODE_PRIVATE)
        val storedPattern = sp?.getString(getString(R.string.pattern), null)

        model.isRegistering = storedPattern == null

        if(model.isRegistering) {
            binding.patternHint.text = getString(R.string.pattern_create)
        }
        else {
            binding.patternHint.text = getString(R.string.pattern_enter)
            binding.patternWarning.visibility = View.INVISIBLE
        }

        val s = (activity as AppCompatActivity).supportActionBar
        s?.hide()
        return binding.root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    // Handle the back button event
                    activity?.finishAndRemoveTask()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)

        // The callback can be enabled or disabled here or in handleOnBackPressed()
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

            // Generate key for encrypting media
            // Stores securely in KeyStore
            GCMEncryptionService.generateUuidKey(requireContext())

            binding.patternLockView.clearPattern()
            model.pattern = ""
            findNavController().navigate(R.id.action_patternUnlockFragment_to_homeFragment)
        }

        if(pattern.length < 4) {
            binding.patternHint.text = getString(R.string.pattern_4dots_required)
            binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG)
            return
        }

        if(model.isRegisteringCounter == 1) {
            binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT)
            binding.patternLockView.clearPattern()

            binding.patternHint.text = getString(R.string.pattern_confirm)
            model.pattern = pattern
            model.isRegisteringCounter++
        }
        else {
            if(model.pattern == pattern) {
                registerPattern()
            }
            else {
                binding.patternHint.text = getString(R.string.pattern_not_match)
                binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG)

                Handler(Looper.getMainLooper()).postDelayed(
                    { binding.patternLockView.clearPattern() },
                    500
                )

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
            //binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT)
            binding.patternLockView.clearPattern()
            model.pattern = ""

            findNavController().navigate(R.id.action_patternUnlockFragment_to_homeFragment)
        }
        else {
            // Wrong
            binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG)
            binding.patternHint.text = getString(R.string.pattern_incorrect)

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