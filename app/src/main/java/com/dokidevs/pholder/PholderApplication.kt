package com.dokidevs.pholder

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.LogProfile
import com.dokidevs.dokilog.d
import com.dokidevs.pholder.data.PholderDatabase
import com.dokidevs.pholder.service.FileIntentService
import com.dokidevs.pholder.utils.ColorUtils
import com.dokidevs.pholder.utils.PrefManager
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ANIMATE_GIF
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_IS_DARK_THEME
import com.squareup.leakcanary.LeakCanary
import io.fabric.sdk.android.Fabric

/*--- PholderApplication ---*/
class PholderApplication : Application(), DokiLog {

    /* companion object */
    companion object {

        /* theme */
        var isDarkTheme = true

        /* gallery */
        var animateGif = true

        /* instances */
        lateinit var prefManager: PrefManager
        lateinit var pholderDatabase: PholderDatabase
        lateinit var colorUtils: ColorUtils

    }

    // onCreate
    override fun onCreate() {
        super.onCreate()
        // For debug
        if (BuildConfig.DEBUG) {
            DokiLog.addProfile(LogProfile("Debug"))
        }
        d()
        // Set up Crashlytics, disabled for debug builds.
        // This is not really required, proguard will remove some critical methods in firebase without this.
        // This will crash app at launch.
        val crashlyticsKit = Crashlytics.Builder()
            .core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
            .build()
        Fabric.with(this, crashlyticsKit)
        // For leak detection
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        LeakCanary.install(this)
        // Get prefManager and set preferences
        prefManager = PrefManager(applicationContext)
        isDarkTheme = prefManager.get(PREF_IS_DARK_THEME, true)
        animateGif = prefManager.get(PREF_ANIMATE_GIF, true)
        colorUtils = ColorUtils(applicationContext, isDarkTheme)
        // Initiate database
        pholderDatabase = PholderDatabase.getInstance(applicationContext)
        FileIntentService.initialise(applicationContext)
    }

}