package com.dokidevs.pholder.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.*
import com.dokidevs.pholder.PholderApplication.Companion.prefManager
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseDialogFragment
import com.dokidevs.pholder.data.PholderDatabase.Companion.SORT_ORDER_DATE_ASC
import com.dokidevs.pholder.data.PholderDatabase.Companion.SORT_ORDER_DATE_DESC
import com.dokidevs.pholder.data.PholderDatabase.Companion.SORT_ORDER_NAME_ASC
import com.dokidevs.pholder.data.PholderDatabase.Companion.SORT_ORDER_NAME_DESC
import com.dokidevs.pholder.utils.ColorUtils.Companion.alertDialogTheme
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SORT_ORDER_FOLDER
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SORT_ORDER_MEDIA
import kotlinx.android.synthetic.main.dialog_sort.*

/*--- SortDialog ---*/
class SortDialog : BaseDialogFragment() {

    /* companion object */
    companion object {

        /* dialog type */
        const val DIALOG_SORT = 401

        // newInstance
        fun newInstance(): SortDialog {
            val sortDialog = SortDialog()
            sortDialog.arguments = getBaseBundle(DIALOG_SORT)
            return sortDialog
        }

    }

    /* views */
    private val folderRadioGroup by lazy { sortDialog_folder_radioGroup }
    private val mediaRadioGroup by lazy { sortDialog_media_radioGroup }
    private val ok by lazy { sortDialog_button_ok }
    private val cancel by lazy { sortDialog_button_cancel }

    /* parameters */
    var hasSortOrderChanged = false
        private set

    // onCreateDialogAction
    override fun onCreateDialogAction(savedInstanceState: Bundle?, dialog: Dialog): Dialog? {
        // Only called when is shown as a dialog, this will remove the title of dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    // onCreateViewAction
    override fun onCreateViewAction(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Use alertDialogTheme
        return View.inflate(ContextThemeWrapper(context, alertDialogTheme), R.layout.dialog_sort, null)
    }

    // onViewCreatedAction
    override fun onViewCreatedAction(view: View, savedInstanceState: Bundle?) {
        setRadioGroup()
        setOK()
        setCancel()
    }

    // setRadioGroup
    private fun setRadioGroup() {
        val folderSortOrder = prefManager.get(PREF_SORT_ORDER_FOLDER, SORT_ORDER_NAME_ASC)
        val checkFolderRadioId = when (folderSortOrder) {
            SORT_ORDER_DATE_ASC -> R.id.sortDialog_folder_date_asc
            SORT_ORDER_DATE_DESC -> R.id.sortDialog_folder_date_desc
            SORT_ORDER_NAME_ASC -> R.id.sortDialog_folder_name_asc
            SORT_ORDER_NAME_DESC -> R.id.sortDialog_folder_name_desc
            else -> R.id.sortDialog_folder_name_asc
        }
        folderRadioGroup.check(checkFolderRadioId)
        val mediaSortOrder = prefManager.get(PREF_SORT_ORDER_MEDIA, SORT_ORDER_DATE_DESC)
        val checkMediaRadioId = when (mediaSortOrder) {
            SORT_ORDER_DATE_ASC -> R.id.sortDialog_media_date_asc
            SORT_ORDER_DATE_DESC -> R.id.sortDialog_media_date_desc
            SORT_ORDER_NAME_ASC -> R.id.sortDialog_media_name_asc
            SORT_ORDER_NAME_DESC -> R.id.sortDialog_media_name_desc
            else -> R.id.sortDialog_media_date_desc
        }
        mediaRadioGroup.check(checkMediaRadioId)
    }

    // setOK
    private fun setOK() {
        ok.setOnClickListener {
            val folderSortOrder = prefManager.get(PREF_SORT_ORDER_FOLDER, SORT_ORDER_NAME_ASC)
            val newFolderSortOrder = when (folderRadioGroup.checkedRadioButtonId) {
                R.id.sortDialog_folder_date_asc -> SORT_ORDER_DATE_ASC
                R.id.sortDialog_folder_date_desc -> SORT_ORDER_DATE_DESC
                R.id.sortDialog_folder_name_asc -> SORT_ORDER_NAME_ASC
                R.id.sortDialog_folder_name_desc -> SORT_ORDER_NAME_DESC
                else -> SORT_ORDER_NAME_ASC
            }
            if (folderSortOrder != newFolderSortOrder) {
                hasSortOrderChanged = true
                prefManager.put(PREF_SORT_ORDER_FOLDER, newFolderSortOrder)
            }
            val mediaSortOrder = prefManager.get(PREF_SORT_ORDER_MEDIA, SORT_ORDER_DATE_DESC)
            val newMediaSortOrder = when (mediaRadioGroup.checkedRadioButtonId) {
                R.id.sortDialog_media_date_asc -> SORT_ORDER_DATE_ASC
                R.id.sortDialog_media_date_desc -> SORT_ORDER_DATE_DESC
                R.id.sortDialog_media_name_asc -> SORT_ORDER_NAME_ASC
                R.id.sortDialog_media_name_desc -> SORT_ORDER_NAME_DESC
                else -> SORT_ORDER_DATE_DESC
            }
            if (mediaSortOrder != newMediaSortOrder) {
                hasSortOrderChanged = true
                prefManager.put(PREF_SORT_ORDER_MEDIA, newMediaSortOrder)
            }
            dialogListener?.onDialogAction(CLICK_POSITIVE, this)
            dismiss()
        }
    }

    private fun setCancel() {
        cancel.setOnClickListener {
            dismiss()
        }
    }

    // onPauseAction
    override fun onPauseAction() {
        // Destroy fragment directly so that it won't be available onResume
        dismiss()
    }

}