package com.dokidevs.pholder.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import java.io.File

// setTextWithColor
fun TextView.setTextWithColor(text: String, color: Int) {
    this.text = text
    this.setTextColor(color)
}

// colorFromId
fun Context.colorFromId(id: Int): Int {
    return ContextCompat.getColor(this.applicationContext, id)
}

// prepareMessage
fun prepareMessage(context: Context, preResId: Int = -1, message: String = "", postResId: Int = -1): String {
    val applicationContext = context.applicationContext
    val pre = if (preResId != -1) {
        applicationContext.getString(preResId)
    } else {
        ""
    }
    val post = if (postResId != -1) {
        applicationContext.getString(postResId)
    } else {
        ""
    }
    return prepareMessage(pre, message, post)
}

// prepareMessage
fun prepareMessage(pre: String = "", message: String = "", post: String = ""): String {
    return pre + message + post
}

// shortToast
fun Context.shortToast(preResId: Int = -1, message: String = "", postResId: Int = -1) {
    Toast.makeText(this.applicationContext, prepareMessage(this, preResId, message, postResId), 3 * TIME_SECOND.toInt())
        .show()
}

// shortSnackBar
fun SnackProgressBarManager.shortSnackBar(
    context: Context,
    preResId: Int = -1,
    message: String = "",
    postResId: Int = -1
) {
    messageSnackBar(
        context, preResId, message, postResId, SnackProgressBarManager.LENGTH_SHORT
    )
}

// longSnackBar
fun SnackProgressBarManager.longSnackBar(
    context: Context,
    preResId: Int = -1,
    message: String = "",
    postResId: Int = -1
) {
    messageSnackBar(
        context, preResId, message, postResId, SnackProgressBarManager.LENGTH_LONG
    )
}

// messageSnackBar
fun SnackProgressBarManager.messageSnackBar(
    context: Context,
    preResId: Int = -1,
    message: String = "",
    postResId: Int = -1,
    duration: Int = SnackProgressBarManager.LENGTH_SHORT
) {
    show(
        SnackProgressBar(SnackProgressBar.TYPE_NORMAL, prepareMessage(context, preResId, message, postResId))
            .setAllowUserInput(true)
            .setSwipeToDismiss(true),
        duration
    )
}

// localBroadcast
fun Context.localBroadcast(intent: Intent) {
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}

// registerLocalBroadcastReceiver
fun Context.registerLocalBroadcastReceiver(broadcastReceiver: BroadcastReceiver, action: String) {
    LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, IntentFilter(action))
}

// unregisterLocalBroadcastReceiver
fun Context.unregisterLocalBroadcastReceiver(broadcastReceiver: BroadcastReceiver) {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
}

// copyTo
fun File.copyTo(outputFile: File) {
    inputStream().use { input ->
        outputFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

// logActivityResult
fun logActivityResult(resultCode: Int): String {
    return when (resultCode) {
        Activity.RESULT_OK -> "Activity.RESULT_OK"
        else -> "Activity.RESULT_CANCELED"
    }
}

// setMargins
fun View.setMargins(left: Int = -2000, top: Int = -2000, right: Int = -2000, bottom: Int = -2000) {
    val newLayoutParams = layoutParams as ViewGroup.MarginLayoutParams
    if (left != -2000) newLayoutParams.leftMargin = left
    if (top != -2000) newLayoutParams.topMargin = top
    if (right != -2000) newLayoutParams.rightMargin = right
    if (bottom != -2000) newLayoutParams.bottomMargin = bottom
    layoutParams = newLayoutParams
}

// setDimensions
fun View.setDimensions(width: Int = -100, height: Int = -100) {
    val newLayoutParams = layoutParams
    if (width != -100) newLayoutParams.width = width
    if (height != -100) newLayoutParams.height = height
    layoutParams = newLayoutParams
}