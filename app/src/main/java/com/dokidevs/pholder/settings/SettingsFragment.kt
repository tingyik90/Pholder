package com.dokidevs.pholder.settings

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.preference.*
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.d
import com.dokidevs.pholder.PholderApplication.Companion.animateGif
import com.dokidevs.pholder.PholderApplication.Companion.prefManager
import com.dokidevs.pholder.R
import com.dokidevs.pholder.utils.ColorUtils
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ALL_VIDEOS_FOLDER_STAR
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ANIMATE_GIF
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ARRAY_EXCLUDED_FOLDER_PATHS
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ARRAY_INCLUDED_FOLDER_PATHS
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ENABLE_CAMERA_LOCATION
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_IS_DARK_THEME
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_LIST_BROWSING_MODE
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SHOW_ALL_VIDEOS_FOLDER
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SHOW_CAMERA_BUTTONS
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SHOW_EMPTY_FOLDERS
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_USE_FULL_NATIVE_CAMERA
import com.takisoft.preferencex.PreferenceFragmentCompat

/*--- SettingsFragment ---*/
class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener, DokiLog {

    /* companion object */
    companion object {

        /* tag */
        const val FRAGMENT_CLASS = "SettingsFragment"

        /* saved instance state */
        private const val SAVED_PREF_IS_DARK_THEME = "SAVED_PREF_IS_DARK_THEME"
        private const val SAVED_PREF_SHOW_ALL_VIDEOS_FOLDER = "SAVED_PREF_SHOW_ALL_VIDEOS_FOLDER"
        private const val SAVED_PREF_SHOW_EMPTY_FOLDERS = "SAVED_PREF_SHOW_EMPTY_FOLDERS"
        private const val SAVED_PREF_LIST_BROWSING_MODE = "SAVED_PREF_LIST_BROWSING_MODE"
        private const val SAVED_PREF_ARRAY_INCLUDED_FOLDER_PATHS = "SAVED_PREF_ARRAY_INCLUDED_FOLDER_PATHS"
        private const val SAVED_PREF_ARRAY_EXCLUDED_FOLDER_PATHS = "SAVED_PREF_ARRAY_EXCLUDED_FOLDER_PATHS"

    }

    /* preferences */
    private var isDarkTheme = true
    private var showAllVideosFolder = true
    private var showEmptyFolders = false
    private var browsingMode = ""
    private var includedFolders = arrayOf("")
    private var excludedFolders = arrayOf("")

    // onCreatePreferences
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, rootKey)
        if (savedInstanceState == null) {
            getInitialValues()
        } else {
            getSavedValues(savedInstanceState)
        }
        setIconColor()
    }

    // getInitialValues
    private fun getInitialValues() {
        isDarkTheme = prefManager.get(PREF_IS_DARK_THEME, true)
        showAllVideosFolder = prefManager.get(PREF_SHOW_ALL_VIDEOS_FOLDER, true)
        showEmptyFolders = prefManager.get(PREF_SHOW_EMPTY_FOLDERS, false)
        browsingMode = prefManager.get(PREF_LIST_BROWSING_MODE, "")
        includedFolders = prefManager.getStringArray(PREF_ARRAY_INCLUDED_FOLDER_PATHS)
        excludedFolders = prefManager.getStringArray(PREF_ARRAY_EXCLUDED_FOLDER_PATHS)
    }

    // getSavedValues
    private fun getSavedValues(savedInstanceState: Bundle) {
        isDarkTheme = savedInstanceState.getBoolean(SAVED_PREF_IS_DARK_THEME, true)
        showAllVideosFolder = savedInstanceState.getBoolean(SAVED_PREF_SHOW_ALL_VIDEOS_FOLDER, true)
        showEmptyFolders = savedInstanceState.getBoolean(SAVED_PREF_SHOW_EMPTY_FOLDERS, false)
        browsingMode = savedInstanceState.getString(SAVED_PREF_LIST_BROWSING_MODE, "")
        includedFolders = savedInstanceState.getStringArray(SAVED_PREF_ARRAY_INCLUDED_FOLDER_PATHS) ?: arrayOf("")
        excludedFolders = savedInstanceState.getStringArray(SAVED_PREF_ARRAY_EXCLUDED_FOLDER_PATHS) ?: arrayOf("")
    }

    // setIconColor
    private fun setIconColor() {
        val preferences = mutableListOf<Preference>()
        getAllPreferences(preferenceScreen, preferences)
        preferences.forEach { preference ->
            preference.icon?.setColorFilter(ColorUtils.iconColorActiveUnfocused, PorterDuff.Mode.SRC_IN)
        }
    }

    // getAllPreferences
    private fun getAllPreferences(preference: Preference, preferences: MutableList<Preference>) {
        if (preference is PreferenceScreen || preference is PreferenceCategory) {
            preference as PreferenceGroup
            val count = preference.preferenceCount
            for (i in 0 until count) {
                getAllPreferences(preference.getPreference(i), preferences)
            }
        } else {
            preferences.add(preference)
        }
    }

    // onResume
    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    // onSharedPreferenceChanged
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        d("key = $key")
        when (key) {
            PREF_ANIMATE_GIF -> {
                // Immediately update global preference
                animateGif = prefManager.get(PREF_ANIMATE_GIF, true)
            }
            PREF_SHOW_CAMERA_BUTTONS -> {
                val showCamera = prefManager.get(PREF_SHOW_CAMERA_BUTTONS, true)
                val prefUseFullNativeCamera =
                    preferenceScreen.findPreference(PREF_USE_FULL_NATIVE_CAMERA) as SwitchPreference
                val prefEnableCameraLocation =
                    preferenceScreen.findPreference(PREF_ENABLE_CAMERA_LOCATION) as SwitchPreference
                if (showCamera) {
                    prefUseFullNativeCamera.isEnabled = true
                    prefEnableCameraLocation.isEnabled = true
                } else {
                    prefUseFullNativeCamera.isChecked = false
                    prefUseFullNativeCamera.isEnabled = false
                    prefEnableCameraLocation.isEnabled = false
                }
            }
            PREF_USE_FULL_NATIVE_CAMERA -> {
                val useNative = prefManager.get(PREF_USE_FULL_NATIVE_CAMERA, false)
                val prefEnableCameraLocation =
                    preferenceScreen.findPreference(PREF_ENABLE_CAMERA_LOCATION) as SwitchPreference
                if (useNative) {
                    prefEnableCameraLocation.isEnabled = false
                } else {
                    val showCamera = prefManager.get(PREF_SHOW_CAMERA_BUTTONS, true)
                    if (showCamera) {
                        prefEnableCameraLocation.isEnabled = true
                    }
                }
            }
        }
    }

    // onPreferenceTreeClick
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        // onPreferenceTreeClick is called after onSharedPreferenceChanged, which means the value is already updated.
        val key = preference.key
        when (key) {
            PREF_ARRAY_INCLUDED_FOLDER_PATHS -> {
                val activity = activity!!
                activity.startActivity(
                    DirectoryListActivity.newIntent(
                        activity,
                        DirectoryListActivity.INCLUDED_DIRECTORY
                    )
                )
            }
            PREF_ARRAY_EXCLUDED_FOLDER_PATHS -> {
                val activity = activity!!
                activity.startActivity(
                    DirectoryListActivity.newIntent(
                        activity,
                        DirectoryListActivity.EXCLUDED_DIRECTORY
                    )
                )
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    // onPause
    override fun onPause() {
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    // isRestartRequired
    fun isRestartRequired(): Boolean {
        if (isDarkTheme != prefManager.get(PREF_IS_DARK_THEME, true)) return true
        if (browsingMode != prefManager.get(PREF_LIST_BROWSING_MODE, "")) return true
        return false
    }

    // setResultIntent
    fun setResultIntent(): Intent {
        val intent = Intent()
        intent.putExtra(SettingsActivity.TO_UPDATE_DATA, toUpdateData())
        intent.putExtra(SettingsActivity.TO_SHOW_CAMERA_BUTTONS, prefManager.get(PREF_SHOW_CAMERA_BUTTONS, true))
        return intent
    }

    // toUpdateData
    private fun toUpdateData(): Boolean {
        if (showAllVideosFolder != prefManager.get(PREF_SHOW_ALL_VIDEOS_FOLDER, true)) {
            // User choose to show this folder again, mark it as starred so it will appear in top of list.
            if (!showAllVideosFolder) {
                prefManager.put(PREF_ALL_VIDEOS_FOLDER_STAR, true)
            }
            return true
        }
        if (showEmptyFolders != prefManager.get(PREF_SHOW_EMPTY_FOLDERS, false)) return true
        if (!includedFolders.contentEquals(prefManager.getStringArray(PREF_ARRAY_INCLUDED_FOLDER_PATHS))) return true
        if (!excludedFolders.contentEquals(prefManager.getStringArray(PREF_ARRAY_EXCLUDED_FOLDER_PATHS))) return true
        return false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVED_PREF_IS_DARK_THEME, isDarkTheme)
        outState.putBoolean(SAVED_PREF_SHOW_ALL_VIDEOS_FOLDER, showAllVideosFolder)
        outState.putBoolean(SAVED_PREF_SHOW_EMPTY_FOLDERS, showEmptyFolders)
        outState.putString(SAVED_PREF_LIST_BROWSING_MODE, browsingMode)
        outState.putStringArray(SAVED_PREF_ARRAY_INCLUDED_FOLDER_PATHS, includedFolders)
        outState.putStringArray(SAVED_PREF_ARRAY_EXCLUDED_FOLDER_PATHS, excludedFolders)
    }

}