package com.dokidevs.pholder.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import com.dokidevs.pholder.PholderApplication.Companion.isDarkTheme
import com.dokidevs.pholder.PholderApplication.Companion.prefManager
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseActivity
import com.dokidevs.pholder.base.BaseDialogFragment
import com.dokidevs.pholder.data.PholderDatabase
import com.dokidevs.pholder.dialog.folderpicker.FolderPickerDialog
import com.dokidevs.pholder.utils.ColorUtils
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ARRAY_EXCLUDED_FOLDER_PATHS
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ARRAY_INCLUDED_FOLDER_PATHS
import kotlinx.android.synthetic.main.activity_directory_list.*
import java.io.File

/*--- DirectoryListActivity ---*/
class DirectoryListActivity : BaseActivity(), DirectoryListAdapter.DirectoryListAdapterListener,
    BaseDialogFragment.DialogListener {

    /* companion object */
    companion object {

        /* intent */
        const val INCLUDED_DIRECTORY = "INCLUDED_DIRECTORY"
        const val EXCLUDED_DIRECTORY = "EXCLUDED_DIRECTORY"
        private const val DIRECTORY_TYPE = "DIRECTORY_TYPE"

        /* dialog */
        private const val REQUEST_CODE_FOLDER_PICKER_DIALOG = 1001

        // newIntent
        fun newIntent(context: Context, directoryType: String): Intent {
            val intent = Intent(context, DirectoryListActivity::class.java)
            intent.putExtra(DIRECTORY_TYPE, directoryType)
            return intent
        }

    }

    /* views */
    private val recyclerView by lazy { directoryListActivity_recyclerView }
    private val adapter = DirectoryListAdapter(this)

    // getDirectoryType
    private fun getDirectoryType(): String {
        return intent.getStringExtra(DIRECTORY_TYPE)
    }

    // getPrefKey
    private fun getPrefKey(directoryType: String): String {
        return if (directoryType == INCLUDED_DIRECTORY) {
            PREF_ARRAY_INCLUDED_FOLDER_PATHS
        } else {
            PREF_ARRAY_EXCLUDED_FOLDER_PATHS
        }
    }

    // getDirectories
    private fun getDirectories(prefKey: String): List<String> {
        val directories = prefManager.getStringArray(prefKey).toMutableList()
        directories.sort()
        return directories
    }

    // onCreatePreAction
    override fun onCreatePreAction(savedInstanceState: Bundle?) {
        // Set theme
        if (isDarkTheme) {
            setTheme(R.style.AppTheme)
        } else {
            setTheme(R.style.AppTheme_Light)
        }
    }

    // onCreateAction
    override fun onCreateAction(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_directory_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setAdapter()
    }

    // setAdapter
    private fun setAdapter() {
        // Get user directories
        adapter.setFilePaths(getDirectories(getPrefKey(getDirectoryType())))
        recyclerView.adapter = adapter
    }

    // onStartAction
    override fun onStartAction() {
        // Do nothing
    }

    // onCreateOptionsMenu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_directory_list, menu)
        return true
    }

    // onOptionsItemSelected
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // To avoid destroying source activity
                onBackPressed()
                return true
            }
            R.id.directoryListActivity_add_folder -> {
                FolderPickerDialog.newInstance(
                    dialogType = FolderPickerDialog.DIALOG_SIMPLE,
                    requestCode = REQUEST_CODE_FOLDER_PICKER_DIALOG,
                    allowCreateFolder = false
                ).show(supportFragmentManager)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // onDialogAction
    override fun onDialogAction(action: Int, dialogFragment: BaseDialogFragment, data: Bundle?) {
        if (dialogFragment is FolderPickerDialog) {
            if (action == BaseDialogFragment.CLICK_POSITIVE) {
                val filePath = dialogFragment.getDestinationRootPath()
                val collisionList = hasCollision(filePath)
                // No collision, save to preferences
                if (collisionList.isEmpty()) {
                    adapter.addFilePath(filePath)
                    saveFilePaths()
                } else {
                    // There is collision, prompt to remove from another side
                    val alertDialogBuilder = AlertDialog.Builder(
                        ContextThemeWrapper(
                            this@DirectoryListActivity,
                            ColorUtils.alertDialogTheme
                        )
                    )
                    var message = if (getDirectoryType() == INCLUDED_DIRECTORY) {
                        getString(R.string.directoryListActivity_dialog_collision_excluded)
                    } else {
                        getString(R.string.directoryListActivity_dialog_collision_included)
                    }
                    collisionList.forEach { collisionPath ->
                        message += "\n\n- $collisionPath"
                    }
                    alertDialogBuilder.setMessage(message)
                    alertDialogBuilder.setNegativeButton(android.R.string.cancel, null)
                    alertDialogBuilder.setPositiveButton(android.R.string.ok) { _, _ ->
                        adapter.addFilePath(filePath)
                        saveFilePaths()
                        val oppositePrefKey = if (getDirectoryType() == INCLUDED_DIRECTORY) {
                            getPrefKey(EXCLUDED_DIRECTORY)
                        } else {
                            getPrefKey(INCLUDED_DIRECTORY)
                        }
                        val oppositeDirectories = getDirectories(oppositePrefKey).toMutableList()
                        collisionList.forEach { collisionPath ->
                            oppositeDirectories.remove(collisionPath)
                        }
                        saveFilePaths(oppositePrefKey, oppositeDirectories)
                    }
                    alertDialogBuilder.show()
                }
            }
        }
    }

    // onDeleteClick
    override fun onDeleteClick(filePath: String) {
        adapter.deleteFilePath(filePath)
        saveFilePaths()
    }

    // hasCollision
    private fun hasCollision(filePath: String): List<String> {
        val collisionList = mutableListOf<String>()
        if (getDirectoryType() == INCLUDED_DIRECTORY) {
            // Check if parent is excluded
            val excludedDirectories = getDirectories(getPrefKey(EXCLUDED_DIRECTORY))
            for (excludedDirectory in excludedDirectories) {
                if (filePath == excludedDirectory || filePath.contains(excludedDirectory + File.separator))
                    collisionList.add(excludedDirectory)
            }
        } else {
            // Check if child is included
            val includedDirectories = getDirectories(getPrefKey(INCLUDED_DIRECTORY))
            for (includedDirectory in includedDirectories) {
                if (filePath == includedDirectory || includedDirectory.contains(filePath + File.separator)) {
                    collisionList.add(includedDirectory)
                }
            }
        }
        return collisionList
    }

    // saveFilePaths
    private fun saveFilePaths(
        prefKey: String = getPrefKey(getDirectoryType()),
        filePaths: List<String> = adapter.getAllItems().map { it.filePath }
    ) {
        prefManager.putStringArray(prefKey, filePaths.toTypedArray())
    }

    // onPauseAction
    override fun onPauseAction() {
        // Update changes
        PholderDatabase.updateExcludedFolderPaths()
    }

}