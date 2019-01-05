package com.dokidevs.pholder

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.LogProfile
import com.dokidevs.pholder.data.PholderDatabase
import com.dokidevs.pholder.service.FileIntentService
import com.dokidevs.pholder.utils.ColorUtils
import com.dokidevs.pholder.utils.PrefManager
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ANIMATE_GIF
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_IS_DARK_THEME
import com.squareup.leakcanary.LeakCanary
import io.fabric.sdk.android.Fabric

/*--- PholderApplication ---*/
class PholderApplication : Application() {

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

    override fun onCreate() {
        super.onCreate()
        // Set default preferences and get prefManager
        prefManager = PrefManager(applicationContext)
        isDarkTheme = prefManager.get(PREF_IS_DARK_THEME, true)
        animateGif = prefManager.get(PREF_ANIMATE_GIF, true)
        pholderDatabase = PholderDatabase.getInstance(applicationContext)
        colorUtils = ColorUtils(applicationContext, isDarkTheme)
        // Initiate database
        FileIntentService.initialise(applicationContext)
        // For debug
        if (BuildConfig.DEBUG) {
            DokiLog.addProfile(LogProfile("Debug"))
        }
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
    }
}