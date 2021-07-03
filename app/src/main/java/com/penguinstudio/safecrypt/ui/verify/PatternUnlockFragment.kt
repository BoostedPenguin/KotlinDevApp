package com.penguinstudio.safecrypt.ui.verify

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPatternUnlockBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment

        binding.patternLockView.addPatternLockListener(mPatternLockViewListener)
        return binding.root
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

            val sp = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
            val pattern = sp.getString(getString(R.string.pattern), null)


            if(pattern == null) {
                Log.d("safeCrypt", "Pattern wasn't configured properly.")
                binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG)


                Handler(Looper.getMainLooper()).postDelayed(
                    { binding.patternLockView.clearPattern() },
                    500
                )
                return
            }


            if(pattern == result) {
                // Correct
                binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT)
            }
            else {
                // Wrong
                binding.patternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG)
            }

            binding.patternLockView.clearPattern()
        }

        override fun onCleared() {
            // Nothing
        }
    }
}