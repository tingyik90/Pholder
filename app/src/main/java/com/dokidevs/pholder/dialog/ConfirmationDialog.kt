package com.dokidevs.pholder.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseDialogFragment
import com.dokidevs.pholder.utils.ColorUtils.Companion.alertDialogTheme

/*--- ConfirmationDialog ---*/
class ConfirmationDialog : BaseDialogFragment() {

    /* companion object */
    companion object {

        /* dialog type */
        const val DIALOG_DELETE = 101
        const val DIALOG_MOVE = 102
        const val DIALOG_RENAME = 103

        // newInstance
        fun newInstance(dialogType: Int): ConfirmationDialog {
            val confirmationDialog = ConfirmationDialog()
            confirmationDialog.arguments = getBaseBundle(dialogType)
            return confirmationDialog
        }

    }

    // onCreateDialogAction
    override fun onCreateDialogAction(savedInstanceState: Bundle?, dialog: Dialog): Dialog? {
        // Use alertDialogTheme because activity context is using colorAccent, not colorAccentDark
        val alertDialogBuilder = AlertDialog.Builder(ContextThemeWrapper(context!!, alertDialogTheme))
        // Set message
        val message = when (dialogType) {
            DIALOG_DELETE -> R.string.confirmationDialog_message_delete_media
            DIALOG_RENAME -> R.string.confirmationDialog_message_rename_folder
            DIALOG_MOVE -> R.string.confirmationDialog_message_move_media
            else -> -1
        }
        if (message != -1) {
            alertDialogBuilder.setMessage(message)
        }
        // Set listeners
        alertDialogBuilder.setPositiveButton(android.R.string.ok) { _, _ ->
            dialogListener?.onDialogAction(CLICK_POSITIVE, this@ConfirmationDialog)
        }
        alertDialogBuilder.setNegativeButton(android.R.string.cancel, null)
        return alertDialogBuilder.create()
    }

    // onCreateViewAction
    override fun onCreateViewAction(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return null
    }

    // onPauseAction
    override fun onPauseAction() {
        // Destroy fragment directly so that it won't be available onResume
        dismiss()
    }

}