package com.penguinstudio.safecrypt

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setCurrentTheme()
    }

    private fun setCurrentTheme() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        sharedPref.let { pref ->
            val darkModeString = getString(R.string.dark_mode)
            val darkModeValues = resources.getStringArray(R.array.dark_mode_values)
            when (pref.getString(darkModeString, darkModeValues[0])) {
                darkModeValues[0] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                darkModeValues[1] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                darkModeValues[2] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }
}