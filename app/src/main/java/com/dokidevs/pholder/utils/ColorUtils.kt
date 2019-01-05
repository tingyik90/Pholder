package com.dokidevs.pholder.utils

import android.content.Context
import com.dokidevs.pholder.R

/*--- ColorUtils ---*/
class ColorUtils(context: Context, isDarkTheme: Boolean) {

    /* companion object */
    companion object {

        /* colors */
        var colorPrimary = 0
        var colorPrimaryDark = 0
        var colorPrimaryLight = 0
        var colorAccent = 0
        var colorAccentDark = 0
        var colorBlack = 0
        var colorWhite = 0
        var colorTransparent = 0
        var textBlackPrimary = 0
        var textBlackSecondary = 0
        var textBlackHint = 0
        var textWhitePrimary = 0
        var textWhiteSecondary = 0
        var textWhiteHint = 0
        var textColorPrimary = 0
        var textColorSecondary = 0
        var textColorHint = 0
        var iconBlackActive = 0
        var iconBlackActiveUnfocused = 0
        var iconBlackInactive = 0
        var iconWhiteActive = 0
        var iconWhiteActiveUnfocused = 0
        var iconWhiteInactive = 0
        var iconColorActive = 0
        var iconColorActiveUnfocused = 0
        var iconColorInactive = 0
        var colorBackgroundBlack = 0
        var colorBackgroundWhite = 0
        var colorBackground = 0
        var colorDialogBackgroundBlack = 0
        var colorDialogBackgroundWhite = 0
        var colorDialogBackground = 0
        var colorDialogAccent = 0
        var dividerBlack = 0
        var dividerWhite = 0
        var colorDivider = 0
        var alertDialogThemeDark = 0
        var alertDialogThemeLight = 0
        var alertDialogTheme = 0
        var statusBarDefault = 0
        var statusBarTranslucent = 0
        var navigationBarDefault = 0
        var navigationBarTranslucent = 0

    }

    /* init */
    init {
        initialiseColor(context, isDarkTheme)
    }

    // initialiseColor
    fun initialiseColor(context: Context, isDarkTheme: Boolean) {
        // primary
        colorPrimary = context.colorFromId(R.color.colorPrimary)
        colorPrimaryDark = context.colorFromId(R.color.colorPrimaryDark)
        colorPrimaryLight = context.colorFromId(R.color.colorPrimaryLight)
        colorAccent = context.colorFromId(R.color.colorAccent)
        colorAccentDark = context.colorFromId(R.color.colorAccentDark)
        // color
        colorBlack = context.colorFromId(R.color.black)
        colorWhite = context.colorFromId(R.color.white)
        colorTransparent = context.colorFromId(android.R.color.transparent)
        // text
        textBlackPrimary = context.colorFromId(R.color.textBlackPrimary)
        textBlackSecondary = context.colorFromId(R.color.textBlackSecondary)
        textBlackHint = context.colorFromId(R.color.textBlackHint)
        textWhitePrimary = context.colorFromId(R.color.textWhitePrimary)
        textWhiteSecondary = context.colorFromId(R.color.textWhiteSecondary)
        textWhiteHint = context.colorFromId(R.color.textWhiteHint)
        textColorPrimary = if (isDarkTheme) textWhitePrimary else textBlackPrimary
        textColorSecondary = if (isDarkTheme) textWhiteSecondary else textBlackSecondary
        textColorHint = if (isDarkTheme) textWhiteHint else textBlackHint
        // icon
        iconBlackActive = context.colorFromId(R.color.iconBlackActive)
        iconBlackActiveUnfocused = context.colorFromId(R.color.iconBlackActiveUnfocused)
        iconBlackInactive = context.colorFromId(R.color.iconBlackInactive)
        iconWhiteActive = context.colorFromId(R.color.iconWhiteActive)
        iconWhiteActiveUnfocused = context.colorFromId(R.color.iconWhiteActiveUnfocused)
        iconWhiteInactive = context.colorFromId(R.color.iconWhiteInactive)
        iconColorActive = if (isDarkTheme) iconWhiteActive else iconBlackActive
        iconColorActiveUnfocused = if (isDarkTheme) iconWhiteActiveUnfocused else iconBlackActiveUnfocused
        iconColorInactive = if (isDarkTheme) iconWhiteInactive else iconBlackInactive
        // background
        colorBackgroundBlack = context.colorFromId(R.color.backgroundBlack)
        colorBackgroundWhite = context.colorFromId(R.color.backgroundWhite)
        colorBackground = if (isDarkTheme) colorBackgroundBlack else colorBackgroundWhite
        // dialog
        colorDialogBackgroundBlack = context.colorFromId(R.color.dialogBackgroundBlack)
        colorDialogBackgroundWhite = context.colorFromId(R.color.dialogBackgroundWhite)
        colorDialogBackground = if (isDarkTheme) colorDialogBackgroundBlack else colorDialogBackgroundWhite
        colorDialogAccent = if (isDarkTheme) colorAccent else colorAccentDark
        // divider
        dividerBlack = context.colorFromId(R.color.dividerBlack)
        dividerWhite = context.colorFromId(R.color.dividerWhite)
        colorDivider = if (isDarkTheme) dividerWhite else dividerBlack
        // alertDialog
        alertDialogThemeDark = R.style.AppTheme_AlertDialog
        alertDialogThemeLight = R.style.AppTheme_Light_AlertDialog
        alertDialogTheme = if (isDarkTheme) alertDialogThemeDark else alertDialogThemeLight
        // system
        statusBarDefault = context.colorFromId(R.color.statusBarDefault)
        statusBarTranslucent = context.colorFromId(R.color.statusBarTranslucent)
        navigationBarDefault = context.colorFromId(R.color.navigationBarDefault)
        navigationBarTranslucent = context.colorFromId(R.color.navigationBarTranslucent)
    }

}