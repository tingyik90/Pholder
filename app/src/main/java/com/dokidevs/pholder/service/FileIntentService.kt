package com.dokidevs.pholder.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.JobIntentService
import androidx.core.content.FileProvider
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.d
import com.dokidevs.dokilog.e
import com.dokidevs.pholder.PholderApplication.Companion.prefManager
import com.dokidevs.pholder.R
import com.dokidevs.pholder.data.FileTag
import com.dokidevs.pholder.data.PholderDatabase
import com.dokidevs.pholder.data.PholderTag
import com.dokidevs.pholder.data.PholderTagUtil
import com.dokidevs.pholder.utils.*
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_AUTOMATICALLY_STAR_CREATED_FOLDER
import java.io.File

/*--- FileIntentService ---*/
class FileIntentService : JobIntentService(), DokiLog {

    /* companion object */
    companion object {

        /* intents */
        const val ACTION_INITIALISE = "ACTION_INITIALISE"
        const val ACTION_FILES_UPDATE = "ACTION_FILES_UPDATE"
        const val ACTION_FILES_DELETE = "ACTION_FILES_DELETE"
        const val ACTION_FILES_DELETED = "ACTION_FILES_DELETED"
        const val ACTION_FILES_MOVE = "ACTION_FILES_MOVE"
        const val ACTION_FILES_MOVED = "ACTION_FILES_MOVED"
        const val ACTION_FILES_RENAME = "DIALOG_RENAME"
        const val ACTION_FILES_RENAMED = "ACTION_FILES_RENAMED"
        const val ACTION_FILES_UPDATED = "ACTION_FILES_UPDATED"
        const val ACTION_FOLDER_CREATE = "DIALOG_CREATE"
        const val ACTION_FOLDER_CREATED = "ACTION_FOLDER_CREATED"
        const val ACTION_FOLDER_STAR = "ACTION_FOLDER_STAR"
        const val ACTION_FOLDER_STARRED = "ACTION_FOLDER_STARRED"
        const val FILES_DESTINATION_ROOT_PATH = "FILES_DESTINATION_ROOT_PATH"
        const val FILES_RENAME_FILE_NAME = "FILES_RENAME_FILE_NAME"
        const val FOLDER_CREATE_PATH = "FOLDER_CREATE_PATH"
        const val FOLDER_STAR_BOOLEAN = "FOLDER_STAR_BOOLEAN"
        private const val MAP_KEY = "MAP_KEY"

        /* results */
        const val ACTION_STATUS_OK = PholderDatabase.ACTION_STATUS_OK
        const val ACTION_STATUS_FAILED = PholderDatabase.ACTION_STATUS_FAILED
        const val ACTION_STATUS_FILE_COLLISION = PholderDatabase.ACTION_STATUS_FILE_COLLISION
        const val ACTION_STATUS_MOVE_INTO_SELF = PholderDatabase.ACTION_STATUS_MOVE_INTO_SELF
        const val ACTION_STATUS_SYSTEM_FOLDER_NOT_ALLOWED = PholderDatabase.ACTION_STATUS_SYSTEM_FOLDER_NOT_ALLOWED
        const val FILES_ACTION_RESULT_PAIR_ARRAY = "FILES_ACTION_RESULT_PAIR_ARRAY"
        const val FOLDER_CREATED_BOOLEAN = "FOLDER_CREATED_BOOLEAN"
        const val FOLDER_CREATED_PATH = "FOLDER_CREATED_PATH"

        /* map */
        private val pholderTagMap = HashMap<String, List<PholderTag>>()

        // initialise
        fun initialise(context: Context) {
            enqueueWork(context, Intent(ACTION_INITIALISE))
        }

        // updateFiles
        fun updateFiles(context: Context) {
            enqueueWork(context, Intent(ACTION_FILES_UPDATE))
        }

        // delete files
        fun deleteFiles(context: Context, pholderTags: List<PholderTag>) {
            val intent = Intent(ACTION_FILES_DELETE)
            putPholderTags(intent, pholderTags)
            enqueueWork(context, intent)
        }

        // moveFiles
        fun moveFiles(context: Context, destinationRootPath: String, pholderTags: List<PholderTag>) {
            val intent = Intent(ACTION_FILES_MOVE)
            intent.putExtra(FILES_DESTINATION_ROOT_PATH, destinationRootPath)
            putPholderTags(intent, pholderTags)
            enqueueWork(context, intent)
        }

        // renameFiles
        fun renameFiles(context: Context, fileName: String, pholderTags: List<PholderTag>) {
            val intent = Intent(ACTION_FILES_RENAME)
            intent.putExtra(FILES_RENAME_FILE_NAME, fileName)
            putPholderTags(intent, pholderTags)
            enqueueWork(context, intent)
        }

        // createFolder
        fun createFolder(context: Context, folderPath: String) {
            val intent = Intent(ACTION_FOLDER_CREATE)
            intent.putExtra(FOLDER_CREATE_PATH, folderPath)
            enqueueWork(context, intent)
        }

        // starFolders
        fun starFolders(context: Context, toStar: Boolean, pholderTags: List<PholderTag>) {
            val intent = Intent(ACTION_FOLDER_STAR)
            intent.putExtra(FOLDER_STAR_BOOLEAN, toStar)
            putPholderTags(intent, pholderTags)
            enqueueWork(context, intent)
        }

        // shareFiles
        fun shareFiles(context: Context, fileTags: List<FileTag>) {
            val fileUris = ArrayList<Uri>(fileTags.size)
            var type = MIME_IMAGE_MIX
            fileTags.forEach { fileTag ->
                fileUris.add(FileProvider.getUriForFile(context, FILE_PROVIDER, fileTag.toFile()))
                if (fileTag.isVideo()) {
                    type = MIME_VIDEO_MIX
                }
            }
            if (fileUris.size > 1) {
                val intent = Intent()
                intent.action = Intent.ACTION_SEND_MULTIPLE
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
                intent.type = type
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.shareIntent_title)))
            } else if (fileUris.size == 1) {
                val intent = Intent()
                intent.action = Intent.ACTION_SEND
                intent.putExtra(Intent.EXTRA_STREAM, fileUris[0])
                intent.type = type
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.shareIntent_title)))
            }
        }

        // enqueueWork
        private fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, FileIntentService::class.java, FILE_INTENT_SERVICE_JOB_ID, intent)
        }

        // putPholderTags
        private fun putPholderTags(intent: Intent, pholderTags: List<PholderTag>) {
            val mapKey = PholderTagUtil.insertMapUnique(pholderTagMap, pholderTags)
            intent.putExtra(MAP_KEY, mapKey)
        }

        // getPholderTags
        private fun getPholderTags(mapKey: String): List<PholderTag>? {
            val pholderTags = pholderTagMap[mapKey]
            pholderTagMap.remove(mapKey)
            return pholderTags
        }

    }

    // onHandleWork
    override fun onHandleWork(intent: Intent) {
        var resultIntent: Intent? = null
        try {
            val action = intent.action
            d("action = $action")
            when (action) {
                ACTION_INITIALISE -> {
                    PholderDatabase.initialise(applicationContext)
                }
                ACTION_FILES_UPDATE -> {
                    PholderDatabase.updateData(applicationContext, contentResolver)
                    resultIntent = Intent(ACTION_FILES_UPDATED)
                }
                ACTION_FILES_DELETE -> {
                    val mapKey = intent.getStringExtra(MAP_KEY)
                    val pholderTags = getPholderTags(mapKey)
                    val resultPairs = PholderDatabase.deleteFiles(applicationContext, pholderTags)
                    resultIntent = Intent(ACTION_FILES_DELETED)
                    resultIntent.putExtra(FILES_ACTION_RESULT_PAIR_ARRAY, resultPairs.toTypedArray())
                }
                ACTION_FILES_RENAME -> {
                    val mapKey = intent.getStringExtra(MAP_KEY)
                    val pholderTags = getPholderTags(mapKey)
                    val fileName = intent.getStringExtra(FILES_RENAME_FILE_NAME)
                    val resultPairs = PholderDatabase.moveFiles(applicationContext, pholderTags, "", fileName)
                    resultIntent = Intent(ACTION_FILES_RENAMED)
                    resultIntent.putExtra(FILES_ACTION_RESULT_PAIR_ARRAY, resultPairs.toTypedArray())
                }
                ACTION_FILES_MOVE -> {
                    val mapKey = intent.getStringExtra(MAP_KEY)
                    val pholderTags = getPholderTags(mapKey)
                    val destinationRootPath = intent.getStringExtra(FILES_DESTINATION_ROOT_PATH)
                    val resultPairs =
                        PholderDatabase.moveFiles(applicationContext, pholderTags, destinationRootPath, "")
                    resultIntent = Intent(ACTION_FILES_MOVED)
                    resultIntent.putExtra(FILES_ACTION_RESULT_PAIR_ARRAY, resultPairs.toTypedArray())
                }
                ACTION_FOLDER_CREATE -> {
                    val folderPath = intent.getStringExtra(FOLDER_CREATE_PATH) ?: ""
                    var isCreated = false
                    if (folderPath.isNotEmpty()) {
                        val newFolder = File(folderPath)
                        isCreated = PholderDatabase.createFolder(
                            applicationContext,
                            newFolder,
                            prefManager.get(PREF_AUTOMATICALLY_STAR_CREATED_FOLDER, true)
                        )
                    }
                    resultIntent = Intent(ACTION_FOLDER_CREATED)
                    resultIntent.putExtra(FOLDER_CREATED_BOOLEAN, isCreated)
                    resultIntent.putExtra(FOLDER_CREATED_PATH, folderPath)
                }
                ACTION_FOLDER_STAR -> {
                    val mapKey = intent.getStringExtra(MAP_KEY)
                    val pholderTags = getPholderTags(mapKey)
                    val toStar = intent.getBooleanExtra(FOLDER_STAR_BOOLEAN, true)
                    PholderDatabase.starFolders(toStar, pholderTags)
                    resultIntent = Intent(ACTION_FOLDER_STARRED)
                }
            }
        } catch (ex: Exception) {
            e(ex)
        } finally {
            if (resultIntent != null) {
                localBroadcast(resultIntent)
            }
        }
    }

    // onDestroy
    override fun onDestroy() {
        d()
        super.onDestroy()
    }

}