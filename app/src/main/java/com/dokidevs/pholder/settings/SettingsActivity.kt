package com.dokidevs.pholder.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.dokidevs.pholder.PholderApplication.Companion.colorUtils
import com.dokidevs.pholder.PholderApplication.Companion.isDarkTheme
import com.dokidevs.pholder.PholderApplication.Companion.prefManager
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseActivity
import com.dokidevs.pholder.gallery.GalleryActivity
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_IS_DARK_THEME

/*--- SettingsActivity ---*/
class SettingsActivity : BaseActivity() {

    /* companion object */
    companion object {

        /* intent */
        const val TO_UPDATE_DATA = "TO_UPDATE_DATA"
        const val TO_SHOW_CAMERA_BUTTONS = "TO_SHOW_CAMERA_BUTTONS"

    }

    /* fragments */
    private var settingsFragment: SettingsFragment? = null

    // onCreatePreAction
    override fun onCreatePreAction(savedInstanceState: Bundle?) {
        // Set theme
        if (isDarkTheme) {
            setTheme(R.style.AppPreferenceTheme)
        } else {
            setTheme(R.style.AppPreferenceTheme_Light)
        }
    }

    // onCreateAction
    override fun onCreateAction(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        var fragment = supportFragmentManager.findFragmentByTag(SettingsFragment.FRAGMENT_CLASS) as? SettingsFragment
        if (fragment == null) {
            fragment = SettingsFragment()
        }
        settingsFragment = fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settingsActivity_container, fragment, "SettingsFragment").commit()
        }
    }

    // onOptionsItemSelected
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // To avoid destroying source activity
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // onBackPressed
    override fun onBackPressed() {
        // Start new activity if restart is required
        val fragment = settingsFragment
        if (fragment != null && fragment.isRestartRequired()) {
            // set theme accordingly first because application does not reinitialise
            isDarkTheme = prefManager.get(PREF_IS_DARK_THEME, true)
            colorUtils.initialiseColor(applicationContext, isDarkTheme)
            val intent = Intent(this, GalleryActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            // Else, just inform the result
            val resultIntent = settingsFragment?.setResultIntent()
            setResult(Activity.RESULT_OK, resultIntent)
            super.onBackPressed()
        }
    }

}