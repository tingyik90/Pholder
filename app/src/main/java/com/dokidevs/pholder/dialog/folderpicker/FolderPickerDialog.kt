package com.dokidevs.pholder.dialog.folderpicker

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import com.dokidevs.pholder.PholderApplication.Companion.prefManager
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseDialogFragment
import com.dokidevs.pholder.data.FolderTag
import com.dokidevs.pholder.data.PholderDatabase
import com.dokidevs.pholder.data.PholderDatabase.Companion.PUBLIC_ROOT
import com.dokidevs.pholder.data.PholderDatabase.Companion.SORT_ORDER_NAME_ASC
import com.dokidevs.pholder.data.PholderTagUtil
import com.dokidevs.pholder.dialog.ConfirmationDialog
import com.dokidevs.pholder.dialog.FolderNameDialog
import com.dokidevs.pholder.service.FileIntentService
import com.dokidevs.pholder.utils.*
import com.dokidevs.pholder.utils.ColorUtils.Companion.colorDialogAccent
import com.dokidevs.pholder.utils.ColorUtils.Companion.textColorHint
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SHOW_EMPTY_FOLDERS
import kotlinx.android.synthetic.main.dialog_folder_picker.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File

/*--- FolderPickerDialog ---*/
class FolderPickerDialog : BaseDialogFragment(), FolderPickerAdapter.FolderPickerAdapterListener,
    BaseDialogFragment.DialogListener {

    /* companion object */
    companion object {

        /* dialog type */
        const val DIALOG_SIMPLE = 201
        const val DIALOG_THUMBNAIL = 202

        /* intents */
        private const val ROOT_PATH = "ROOT_PATH"
        private const val REQUEST_CODE = "REQUEST CODE"
        private const val TOP_DIRECTORY_PATH = "TOP_DIRECTORY_PATH"
        private const val ALLOW_CREATE_FOLDER = "ALLOW_CREATE_FOLDER"
        private const val CONFIRMATION_DIALOG_TYPE = "CONFIRMATION_DIALOG_TYPE"

        // newInstance
        fun newInstance(
            dialogType: Int,
            requestCode: Int,
            rootPath: String = PUBLIC_ROOT.absolutePath,
            topDirectoryPath: String = PUBLIC_ROOT.absolutePath,
            allowCreateFolder: Boolean = true,
            confirmationDialogType: Int = -1
        ): FolderPickerDialog {
            val folderPickerDialog = FolderPickerDialog()
            val bundle = getBaseBundle(dialogType)
            bundle.putString(ROOT_PATH, rootPath)
            bundle.putInt(REQUEST_CODE, requestCode)
            bundle.putString(TOP_DIRECTORY_PATH, topDirectoryPath)
            bundle.putBoolean(ALLOW_CREATE_FOLDER, allowCreateFolder)
            bundle.putInt(CONFIRMATION_DIALOG_TYPE, confirmationDialogType)
            folderPickerDialog.arguments = bundle
            return folderPickerDialog
        }

    }

    /* views */
    private val recyclerView by lazy { folderPickerDialog_recyclerView }
    private lateinit var folderPickerAdapter: FolderPickerAdapter
    private val addButton by lazy { folderPickerDialog_addButton }
    private val backButton by lazy { folderPickerDialog_backButton }
    private val title by lazy { folderPickerDialog_title }
    private val emptyView by lazy { folderPickerDialog_list_emptyView }
    private val actionButton by lazy { folderPickerDialog_button_action }
    private val cancelButton by lazy { folderPickerDialog_button_cancel }

    /* parameters */
    var requestCode = -1
        private set
    private var allowCreateFolder = true
    private var confirmationDialogType = -1
    private lateinit var rootFile: File
    private lateinit var topDirectoryPath: String
    private lateinit var broadcastReceiver: BroadcastReceiver

    // onCreateAction
    override fun onCreateAction(savedInstanceState: Bundle?) {
        val rootPath = arguments!!.getString(ROOT_PATH)
        rootFile = File(rootPath)
        if (!rootFile.exists()) {
            rootFile = PUBLIC_ROOT
        }
        requestCode = arguments!!.getInt(REQUEST_CODE, -1)
        topDirectoryPath = arguments!!.getString(TOP_DIRECTORY_PATH, "")
        allowCreateFolder = arguments!!.getBoolean(ALLOW_CREATE_FOLDER, true)
        confirmationDialogType = arguments!!.getInt(CONFIRMATION_DIALOG_TYPE, -1)
    }

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
        return View.inflate(
            ContextThemeWrapper(context, ColorUtils.alertDialogTheme),
            R.layout.dialog_folder_picker,
            null
        )
    }

    // onViewCreatedAction
    override fun onViewCreatedAction(view: View, savedInstanceState: Bundle?) {
        setBroadcastReceiver()
        setAddButton()
        setBackButton()
        setTitle()
        setActionButton()
        setCancelButton()
        setAdapter()
        updateRoot(rootFile.absolutePath)
    }

    // setBroadcastReceiver
    private fun setBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                when (action) {
                    FileIntentService.ACTION_FOLDER_CREATED -> {
                        val scrollToPath = intent.getStringExtra(FileIntentService.FOLDER_CREATED_PATH)
                        updateRoot(rootFile.absolutePath, scrollToPath)
                    }
                }
            }
        }
    }

    // setAddButton
    private fun setAddButton() {
        addButton.setOnClickListener {
            val folderNameDialog = FolderNameDialog.newCreateDialog(rootFile.absolutePath)
            folderNameDialog.show(childFragmentManager)
        }
    }

    // toggleAddButton
    private fun toggleAddButton() {
        // Don't allow create folder at top directory
        addButton.isVisible = allowCreateFolder && rootFile != PUBLIC_ROOT
    }

    // setBackButton
    private fun setBackButton() {
        // backButton.setColorFilter(iconColorActive, PorterDuff.Mode.SRC_IN)
        backButton.setOnClickListener {
            rootFile = rootFile.parentFile
            updateRoot(rootFile.absolutePath)
        }
    }

    // toggleBackButton
    private fun toggleBackButton() {
        backButton.isVisible = rootFile.absolutePath != topDirectoryPath
    }

    // setTitle
    private fun setTitle() {
        // Must be selected in order for marquee ellipsize to work. See https://stackoverflow.com/a/3333855/3584439
        title.isSelected = true
        val name = if (rootFile == PUBLIC_ROOT) {
            "Root"
        } else {
            rootFile.name
        }
        title.text = name
    }

    // setActionButton
    private fun setActionButton() {
        actionButton.setOnClickListener {
            val destinationRootPath = getDestinationRootPath()
            // Don't allow anything to be moved / saved at top directory
            if (destinationRootPath == PUBLIC_ROOT.absolutePath) {
                getApplicationContext().shortToast(R.string.toast_folderPicker_warn_root_not_allowed)
            } else {
                if (confirmationDialogType != -1) {
                    // Ask for confirmation
                    ConfirmationDialog.newInstance(confirmationDialogType).show(childFragmentManager)
                } else {
                    sendDestination()
                }
            }
        }
    }

    // onDialogAction
    override fun onDialogAction(action: Int, dialogFragment: BaseDialogFragment, data: Bundle?) {
        if (dialogFragment.dialogType == ConfirmationDialog.DIALOG_MOVE) {
            if (action == CLICK_POSITIVE) {
                sendDestination()
            }
        }
    }

    // toggleActionButton
    private fun toggleActionButton() {
        if (folderPickerAdapter.getSelectedTag() == null) {
            // Don't allow top directory selection
            if (getDestinationRootPath() == PUBLIC_ROOT.absolutePath) {
                actionButton.setTextWithColor(getString(R.string.folderPickerDialog_action_here), textColorHint)
            } else {
                actionButton.setTextWithColor(getString(R.string.folderPickerDialog_action_here), colorDialogAccent)
            }
        } else {
            actionButton.setTextWithColor(getString(R.string.folderPickerDialog_action_select), colorDialogAccent)
        }
    }

    // setCancelButton
    private fun setCancelButton() {
        cancelButton.setOnClickListener {
            dialog.cancel()
        }
    }

    // setAdapter
    private fun setAdapter() {
        folderPickerAdapter = FolderPickerAdapter(this)
        recyclerView.adapter = folderPickerAdapter
    }

    // updateRoot
    private fun updateRoot(rootPath: String, scrollToPath: String = "") {
        when (dialogType) {
            DIALOG_SIMPLE -> {
                // These folderTags do not have thumbnailPath and will only show folder icon.
                rootFile = File(rootPath)
                val folderTags = mutableListOf<FolderTag>()
                val files = rootFile.listFiles()
                files.forEach { file ->
                    if (file.isDirectory && PholderTagUtil.isValidFile(file)) {
                        val folderTag = FolderTag(file)
                        val subFiles = file.listFiles()
                        subFiles.forEach { subFile ->
                            if (PholderTagUtil.isValidFile(subFile)) {
                                if (subFile.isDirectory) {
                                    folderTag.folderCount++
                                } else {
                                    folderTag.fileCount++
                                }
                            }
                        }
                        folderTags.add(folderTag)
                    }
                }
                folderTags.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.fileName })
                folderPickerAdapter.updateFolderTags(folderTags)
                postUpdateRoot(scrollToPath)
            }
            DIALOG_THUMBNAIL -> {
                rootFile = File(rootPath)
                doAsync {
                    val folderTags =
                        PholderDatabase.buildFolderItems(
                            rootFile,
                            prefManager.get(PREF_SHOW_EMPTY_FOLDERS, false)
                        )
                    PholderDatabase.sortFolderItems(folderTags, SORT_ORDER_NAME_ASC, false)
                    uiThread {
                        folderPickerAdapter.updateFolderTags(folderTags)
                        postUpdateRoot(scrollToPath)
                    }
                }
            }
        }
    }

    // postUpdateRoot
    private fun postUpdateRoot(scrollToPath: String = "") {
        setEmptyView()
        setTitle()
        toggleAddButton()
        toggleBackButton()
        toggleActionButton()
        scrollTo(scrollToPath)
    }

    // setEmptyView
    private fun setEmptyView() {
        emptyView.isVisible = folderPickerAdapter.getAllItems().isEmpty()
    }

    // scrollTo
    private fun scrollTo(scrollToPath: String) {
        if (scrollToPath.isNotEmpty()) {
            val position = PholderTagUtil.getPholderTagPosition(folderPickerAdapter.getAllItems(), scrollToPath)
            if (position >= 0) {
                recyclerView.smoothScrollToPosition(position)
            }
        }
    }

    // onResumeAction
    override fun onResumeAction() {
        getActivityContext().registerLocalBroadcastReceiver(broadcastReceiver, FileIntentService.ACTION_FOLDER_CREATED)
    }

    // onItemClick
    override fun onItemClick(folderTag: FolderTag) {
        val isSelected = !folderTag.isSelected
        if (isSelected) {
            // Deselect original tag
            val oldSelectedTag = folderPickerAdapter.getSelectedTag()
            if (oldSelectedTag != null) {
                oldSelectedTag.isSelected = false
                folderPickerAdapter.updateSelection(oldSelectedTag)
            }
            folderPickerAdapter.setSelectedTag(folderTag)
        } else {
            folderPickerAdapter.setSelectedTag(null)
        }
        // Update tag status
        folderTag.isSelected = isSelected
        folderPickerAdapter.updateSelection(folderTag)
        toggleActionButton()
    }

    // onNextClick
    override fun onNextClick(folderTag: FolderTag) {
        updateRoot(folderTag.getFilePath())
    }

    // getDestinationRootPath
    fun getDestinationRootPath(): String {
        val selectedTag = folderPickerAdapter.getSelectedTag()
        return selectedTag?.getFilePath() ?: rootFile.absolutePath
    }

    // sendDestination
    private fun sendDestination() {
        dialogListener?.onDialogAction(CLICK_POSITIVE, this)
        dismiss()
    }

    // onPauseAction
    override fun onPauseAction() {
        getActivityContext().unregisterLocalBroadcastReceiver(broadcastReceiver)
        recyclerView.adapter = null
        // Destroy fragment directly so that it won't be available onResume
        dismiss()
    }

}