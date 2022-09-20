package com.ogeorges.mobileproximitycommunication.views

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.ogeorges.mobileproximitycommunication.R

class FunctionalitySettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.functionality_preferences, rootKey)
    }

}