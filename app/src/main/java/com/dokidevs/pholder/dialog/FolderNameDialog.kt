package com.dokidevs.pholder.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseDialogFragment
import com.dokidevs.pholder.service.FileIntentService
import com.dokidevs.pholder.utils.ColorUtils.Companion.alertDialogTheme
import com.dokidevs.pholder.utils.FILEPATH_RESERVED_CHARACTERS
import com.dokidevs.pholder.utils.shortToast
import java.io.File

/*--- FolderNameDialog ---*/
class FolderNameDialog : BaseDialogFragment(), BaseDialogFragment.DialogListener {

    /* companion object */
    companion object {

        /* dialog type */
        const val DIALOG_CREATE = 301
        const val DIALOG_RENAME = 302

        /* intents */
        private const val ROOT_PATH = "ROOT_PATH"

        // newCreateDialog
        fun newCreateDialog(rootPath: String): FolderNameDialog {
            val folderNameDialog = FolderNameDialog()
            val bundle = getBaseBundle(DIALOG_CREATE)
            bundle.putString(ROOT_PATH, rootPath)
            folderNameDialog.arguments = bundle
            return folderNameDialog
        }

        // newRenameDialog
        fun newRenameDialog(): FolderNameDialog {
            val folderNameDialog = FolderNameDialog()
            folderNameDialog.arguments = getBaseBundle(DIALOG_RENAME)
            return folderNameDialog
        }

    }

    /* variables */
    var folderName = ""
        private set

    // onCreateDialogAction
    @SuppressLint("InflateParams")
    override fun onCreateDialogAction(savedInstanceState: Bundle?, dialog: Dialog): Dialog? {
        // Use contextThemeWrapper because activity context is using colorAccent, not colorAccentDark
        val contextThemeWrapper = ContextThemeWrapper(context!!, alertDialogTheme)
        val alertDialogBuilder = AlertDialog.Builder(contextThemeWrapper)
        val view = LayoutInflater.from(contextThemeWrapper).inflate(R.layout.dialog_folder_name, null)
        alertDialogBuilder.setView(view)
        when (dialogType) {
            DIALOG_CREATE -> {
                alertDialogBuilder.setTitle(R.string.folderNameDialog_title_create)
            }
            DIALOG_RENAME -> {
                alertDialogBuilder.setTitle(R.string.folderNameDialog_title_rename)
            }
        }
        alertDialogBuilder.setPositiveButton(android.R.string.ok, null)
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

    // onResumeAction
    override fun onResumeAction() {
        val alertDialog = dialog as? AlertDialog
        if (alertDialog != null) {
            // Set editText
            val editText = dialog.findViewById<EditText>(R.id.folderNameDialog_name)
            editText.append("")
            // Override positive button
            val positiveButton = alertDialog.getButton(Dialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val folderName = editText.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    // Check for reserved characters
                    for (char in FILEPATH_RESERVED_CHARACTERS) {
                        if (folderName.contains(char)) {
                            context!!.shortToast(R.string.toast_createFolder_warn_char)
                            return@setOnClickListener
                        }
                    }
                    if (folderName[0] == '.') {
                        context!!.shortToast(R.string.toast_createFolder_warn_full_stop)
                        return@setOnClickListener
                    }
                    when (dialogType) {
                        DIALOG_CREATE -> {
                            val rootFile = File(arguments?.getString(ROOT_PATH))
                            val newFolder = File(rootFile, folderName)
                            if (newFolder.exists()) {
                                context!!.shortToast(
                                    preResId = R.string.toast_createFolder_warn_exist_pre,
                                    message = folderName,
                                    postResId = R.string.toast_createFolder_warn_exist_post
                                )
                            } else {
                                FileIntentService.createFolder(context!!.applicationContext, newFolder.absolutePath)
                                alertDialog.dismiss()
                            }
                        }
                        DIALOG_RENAME -> {
                            this.folderName = folderName
                            ConfirmationDialog.newInstance(ConfirmationDialog.DIALOG_RENAME).show(childFragmentManager)
                        }
                    }
                } else {
                    context!!.shortToast(R.string.toast_createFolder_warn_empty)
                }
            }
        }
    }

    // onDialogAction
    override fun onDialogAction(action: Int, dialogFragment: BaseDialogFragment, data: Bundle?) {
        if (dialogFragment.dialogType == ConfirmationDialog.DIALOG_RENAME) {
            if (action == CLICK_POSITIVE) {
                dialogListener?.onDialogAction(CLICK_POSITIVE, this)
                dialog.dismiss()
            }
        }
    }

    // onPauseAction
    override fun onPauseAction() {
        dialog as AlertDialog
        // Detach blinking cursor which leaks activity
        val editText = dialog.findViewById<EditText>(R.id.folderNameDialog_name)
        editText?.isCursorVisible = false
        // Destroy fragment directly so that it won't be available onResume
        dismiss()
    }

}