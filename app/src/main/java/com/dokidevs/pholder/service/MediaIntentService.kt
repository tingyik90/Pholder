package com.dokidevs.pholder.service

import android.app.IntentService
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.provider.MediaStore
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.d
import com.dokidevs.pholder.data.PholderTagUtil

import java.io.File

/*--- MediaIntentService ---*/
// Closely related to https://android.googlesource.com/platform/packages/providers/MediaProvider/+/jb-dev/src/com/android/providers/media/MediaProvider.java
// Note that contentResolver only notify MTP via sendObjectAdded() and sendObjectRemoved() in contentResolver.insert() and contentResolver.delete().
// Hence, when moving files, updating file path via contentResolver.update() will cause the file still showing in the old location.
// Deleting the files show in MTP at old location will also delete the files at new location, since MTP and MediaStore db are tied via uri.
class MediaIntentService : IntentService("MediaIntentService"), DokiLog {

    /* companion object */
    companion object {

        /* scan actions */
        const val SCAN_ACTION_FILE_DELETE = 1
        const val SCAN_ACTION_FILE_ADD = 2
        const val SCAN_ACTION_FOLDER_DELETE = 3
        const val SCAN_ACTION_FOLDER_ADD_THIS = 4
        const val SCAN_ACTION_FOLDER_ADD_ALL = 5

        /* intents */
        private const val ACTION_MEDIA_SCAN_PATHS = "ACTION_MEDIA_SCAN_PATHS"
        private const val ACTION_MEDIA_UPDATE_LAT_LNG = "ACTION_MEDIA_UPDATE_LAT_LNG"
        private const val MAP_KEY = "MAP_KEY"
        private const val FILE_PATH = "FILE_PATH"
        private const val LATITUDE = "LATITUDE"
        private const val LONGITUDE = "LONGITUDE"

        /* transactions */
        private const val MAX_TRANSACTION_SIZE = 100

        /* dummy file */
        private const val dummyFileName = ".pholder.txt"

        /* map */
        private val scanPairMap = HashMap<String, List<Pair<String, Int>>>()

        // scanMediaPaths
        fun scanMediaPaths(context: Context, scanPairs: List<Pair<String, Int>>) {
            val intent = Intent(context, MediaIntentService::class.java)
            intent.action = ACTION_MEDIA_SCAN_PATHS
            val mapKey = PholderTagUtil.insertMapUnique(scanPairMap, scanPairs)
            intent.putExtra(MAP_KEY, mapKey)
            context.startService(intent)
        }

        // updateLatLng
        fun updateLatLng(context: Context, filePath: String, lat: Double, lng: Double) {
            val intent = Intent(context, MediaIntentService::class.java)
            intent.action = ACTION_MEDIA_UPDATE_LAT_LNG
            intent.putExtra(FILE_PATH, filePath)
            intent.putExtra(LATITUDE, lat)
            intent.putExtra(LONGITUDE, lng)
            context.startService(intent)
        }

    }

    // onHandleIntent
    override fun onHandleIntent(intent: Intent) {
        val action = intent.action
        val mapKey = intent.getStringExtra(MAP_KEY)
        d("action = $action")
        when (action) {
            ACTION_MEDIA_SCAN_PATHS -> {
                val scanPairs = scanPairMap[mapKey]
                scanPairMap.remove(mapKey)
                if (scanPairs != null) {
                    scanMediaPaths(scanPairs)
                }
            }
            ACTION_MEDIA_UPDATE_LAT_LNG -> {
                val filePath = intent.getStringExtra(FILE_PATH) ?: ""
                val lat = intent.getDoubleExtra(LATITUDE, 0.0)
                val lng = intent.getDoubleExtra(LONGITUDE, 0.0)
                updateLatLng(filePath, lat, lng)
            }
        }
    }

    // scanMediaPaths
    private fun scanMediaPaths(scanPairs: List<Pair<String, Int>>) {
        val start = System.currentTimeMillis()
        val bufferFilePaths = ArrayList<String>(MAX_TRANSACTION_SIZE)
        for (scanPair in scanPairs) {
            when (scanPair.second) {
                SCAN_ACTION_FILE_DELETE, SCAN_ACTION_FILE_ADD -> {
                    bufferFilePaths.add(scanPair.first)
                }
                SCAN_ACTION_FOLDER_DELETE -> {
                    deleteFolder(scanPair.first)
                    bufferFilePaths.add(scanPair.first)
                }
                SCAN_ACTION_FOLDER_ADD_THIS -> {
                    // Do not scan folders, they will turn into file icon in MTP.
                    // Create a dummy file and scan it for empty folder.
                    val dummyFile = File(scanPair.first, dummyFileName)
                    dummyFile.createNewFile()
                    bufferFilePaths.add(dummyFile.absolutePath)
                }
                SCAN_ACTION_FOLDER_ADD_ALL -> {
                    val newScanPairs = mutableListOf<Pair<String, Int>>()
                    walkFolders(scanPair.first, newScanPairs)
                    scanMediaPaths(newScanPairs)
                }
            }
            // To avoid transactionTooLargeException or scanFile to hang
            if (bufferFilePaths.size == MAX_TRANSACTION_SIZE) {
                scanMedia(bufferFilePaths)
                bufferFilePaths.clear()
            }
        }
        // Finish remaining update
        if (bufferFilePaths.isNotEmpty()) {
            scanMedia(bufferFilePaths)
        }
        val duration = System.currentTimeMillis() - start
        d("duration = $duration")
    }

    // deleteFolder
    private fun deleteFolder(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            // 'Files' table is the main table for image, video, audio and non-media. If folder is deleted, all content should be removed as well.
            // This operation is only for folders which are able to be deleted completely i.e. does not contains file of other format.
            // Do not applyBatch due to "sqlConnectionPool unable to grant a connection" during large transaction.
            // This method is slower, but guaranteed to work.
            val folderPath = filePath + File.separator + "%"
            val deletedFileRows = contentResolver.delete(
                MediaStore.Files.getContentUri("external"),
                MediaStore.MediaColumns.DATA + " like ?",
                arrayOf(folderPath)
            )
            d("folderPath = $filePath, deletedFileRows = $deletedFileRows")
        }
    }

    // walkFolders
    private fun walkFolders(rootPath: String, scanPairs: MutableList<Pair<String, Int>>) {
        val file = File(rootPath)
        if (file.isDirectory) {
            val subFiles = file.listFiles()
            if (subFiles.isEmpty()) {
                // This is an empty folder. Just scan this folder to show in MTP.
                scanPairs.add(Pair(file.absolutePath, SCAN_ACTION_FOLDER_ADD_THIS))
            } else {
                // Scan for all files. scanFile() will take care of handling ".nomedia" itself.
                // Those files and folders will not be added to MediaStore, but must still can to show up in MTP.
                subFiles.forEach { subFile ->
                    if (subFile.isFile) {
                        scanPairs.add(Pair(subFile.absolutePath, SCAN_ACTION_FILE_ADD))
                    } else {
                        walkFolders(subFile.absolutePath, scanPairs)
                    }
                }
            }
        } else {
            // Wrong assignment, change to scan file
            scanPairs.add(Pair(file.absolutePath, SCAN_ACTION_FILE_ADD))
        }
    }


    // updateLatLng
    private fun updateLatLng(filePath: String, lat: Double, lng: Double) {
        if (lat != 0.0 && lng != 0.0) {
            // Column name is same for image and video
            val values = ContentValues()
            values.put(MediaStore.Images.ImageColumns.LATITUDE, lat)
            values.put(MediaStore.Images.ImageColumns.LONGITUDE, lng)
            contentResolver.update(
                PholderTagUtil.getExternalContentUri(filePath),
                values,
                MediaStore.MediaColumns.DATA + " = ?",
                arrayOf(filePath)
            )
        }
    }

    // scanMedia
    private fun scanMedia(filePaths: List<String>) {
        MediaScannerConnection.scanFile(applicationContext, filePaths.toTypedArray(), null) { filePath, uri ->
            d("filePath = $filePath, uri = ${uri ?: "deleted"}")
            if (uri != null && filePath.endsWith(dummyFileName)) {
                // Dummy file is created. Delete it and scan again.
                File(filePath).delete()
                scanMedia(listOf(filePath))
            }
        }
    }

    // onDestroy
    override fun onDestroy() {
        d()
        super.onDestroy()
    }

}