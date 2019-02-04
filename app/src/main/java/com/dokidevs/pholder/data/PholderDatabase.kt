package com.dokidevs.pholder.data

import android.content.ContentResolver
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.d
import com.dokidevs.pholder.PholderApplication
import com.dokidevs.pholder.PholderApplication.Companion.prefManager
import com.dokidevs.pholder.R
import com.dokidevs.pholder.data.FileAction.Companion.ACTION_ADD
import com.dokidevs.pholder.data.FileAction.Companion.ACTION_DELETE
import com.dokidevs.pholder.data.PholderTag.Companion.TYPE_FILE
import com.dokidevs.pholder.service.MediaIntentService
import com.dokidevs.pholder.service.MediaIntentService.Companion.SCAN_ACTION_FILE_ADD
import com.dokidevs.pholder.service.MediaIntentService.Companion.SCAN_ACTION_FILE_DELETE
import com.dokidevs.pholder.service.MediaIntentService.Companion.SCAN_ACTION_FOLDER_ADD_ALL
import com.dokidevs.pholder.service.MediaIntentService.Companion.SCAN_ACTION_FOLDER_ADD_THIS
import com.dokidevs.pholder.service.MediaIntentService.Companion.SCAN_ACTION_FOLDER_DELETE
import com.dokidevs.pholder.utils.MIME_IMAGE_ANY
import com.dokidevs.pholder.utils.MIME_VIDEO_MP4
import com.dokidevs.pholder.utils.MIME_VIDEO_TGP
import com.dokidevs.pholder.utils.MIME_VIDEO_WEBM
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ALL_VIDEOS_FOLDER_STAR
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ARRAY_CREATED_FOLDER_PATHS
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ARRAY_EXCLUDED_FOLDER_PATHS
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ARRAY_INCLUDED_FOLDER_PATHS
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_DATABASE_LAST_UPDATE_TIME
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_IS_PHOLDER_FOLDER_CREATED
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_IS_SUGGESTED_STAR_FOLDER_PATHS_ADDED
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SHOW_ALL_VIDEOS_FOLDER
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SHOW_EMPTY_FOLDERS
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SORT_ORDER_FOLDER
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SORT_ORDER_MEDIA
import org.jetbrains.anko.collections.forEachWithIndex
import java.io.File

/* dao */
val fileTagDao by lazy { PholderApplication.pholderDatabase.getFileTagDao() }
val folderTagDao by lazy { PholderApplication.pholderDatabase.getFolderTagDao() }
val fileActionDao by lazy { PholderApplication.pholderDatabase.getFileActionDao() }

/*--- PholderDatabase ---*/
@Database(entities = [FileTag::class, FolderTag::class, FileAction::class], version = 2)
abstract class PholderDatabase : RoomDatabase(), DokiLog {

    // getFileTagDao
    abstract fun getFileTagDao(): FileTagDao

    // getFolderTagDao
    abstract fun getFolderTagDao(): FolderTagDao

    // getFileActionDao
    abstract fun getFileActionDao(): FileActionDao

    /* companion object */
    companion object : DokiLog {

        /* directories */
        val PUBLIC_ROOT = Environment.getExternalStorageDirectory()!!
        val PHOLDER_FOLDER = File(PUBLIC_ROOT, "Pholder")
        val ALL_VIDEOS_FOLDER = File("/Pholder/All Videos")
        private val excludedFolderPaths = mutableListOf<String>()

        /* action status */
        const val ACTION_STATUS_OK = 1
        const val ACTION_STATUS_FAILED = 2
        const val ACTION_STATUS_FILE_COLLISION = 3
        const val ACTION_STATUS_MOVE_INTO_SELF = 4
        const val ACTION_STATUS_SYSTEM_FOLDER_NOT_ALLOWED = 11

        /* sort order */
        const val SORT_ORDER_DATE_ASC = 1
        const val SORT_ORDER_DATE_DESC = 2
        const val SORT_ORDER_NAME_ASC = 3
        const val SORT_ORDER_NAME_DESC = 4

        /* parameters */
        private var pholderDatabase: PholderDatabase? = null
        private var lastUpdateTime = 0L

        /* migrations */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            // NOT NULL is required as the rule of Room is that primitive fields will be NOT NULL.
            // It will throw 'attempt to re-open an already-closed object' error if the NOT NULL does not match.
            // See https://commonsware.com/AndroidArch/previews/the-dao-of-entities
            // See
            // 'action' is a keyword of SQLite. To use it as column name, need to convert it to string literal.
            // See https://www.sqlite.org/lang_keywords.html
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop old FileTag table
                database.execSQL("DROP TABLE FileTag")
                // Create FileTag table again
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS FileTag " +
                            "(parentPath TEXT NOT NULL, fileName TEXT NOT NULL, extension TEXT NOT NULL, " +
                            "mediaStoreId INTEGER NOT NULL, dateTaken INTEGER NOT NULL, duration INTEGER NOT NULL, " +
                            "lat REAL NOT NULL, lng REAL NOT NULL, exist INTEGER NOT NULL, " +
                            "PRIMARY KEY (parentPath, fileName, extension))"
                )
                database.execSQL("CREATE INDEX index_FileTag_dateTaken ON FileTag(dateTaken)")
                // Create fileAction table
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS FileAction " +
                            "(parentPath TEXT NOT NULL, fileName TEXT NOT NULL, extension TEXT NOT NULL, " +
                            "mediaStoreId INTEGER NOT NULL, dateTaken INTEGER NOT NULL, duration INTEGER NOT NULL, " +
                            "lat REAL NOT NULL, lng REAL NOT NULL, exist INTEGER NOT NULL, " +
                            "'action' INTEGER NOT NULL, time INTEGER NOT NULL, " +
                            "PRIMARY KEY (parentPath, fileName, extension))"
                )
                database.execSQL("CREATE INDEX index_FileAction_time ON FileAction(time)")
            }
        }

        // getInstance
        fun getInstance(context: Context): PholderDatabase {
            if (pholderDatabase == null) {
                synchronized(PholderDatabase::class.java) {
                    pholderDatabase = Room
                        .databaseBuilder(context.applicationContext, PholderDatabase::class.java, "pholder.db")
                        .addMigrations(MIGRATION_1_2)
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return pholderDatabase!!
        }

        // initialise
        fun initialise(context: Context) {
            val start = System.currentTimeMillis()
            val applicationContext = context.applicationContext
            lastUpdateTime = prefManager.get(PREF_DATABASE_LAST_UPDATE_TIME, 0L)
            // For first initiation, import included folder paths from xml
            if (!prefManager.contains(PREF_ARRAY_INCLUDED_FOLDER_PATHS)) {
                val suggestedFolderPaths =
                    applicationContext.resources.getStringArray(R.array.included_folder_paths_suggested)
                val validFolderPaths = mutableListOf<String>()
                suggestedFolderPaths.forEach { suggestedFolderPath ->
                    // The paths are relative paths, convert to absolute paths
                    val file = File(PUBLIC_ROOT, suggestedFolderPath)
                    validFolderPaths.add(file.absolutePath)
                }
                prefManager.putStringArray(PREF_ARRAY_INCLUDED_FOLDER_PATHS, validFolderPaths.toTypedArray())
            }
            // Update local list
            updateExcludedFolderPaths()
            // Import suggested starred folders
            if (!prefManager.get(PREF_IS_SUGGESTED_STAR_FOLDER_PATHS_ADDED, false)) {
                val starredFolderRelativePaths =
                    applicationContext.resources.getStringArray(R.array.starred_folder_paths_suggested).toList()
                val starredFolderTagSet = HashSet<FolderTag>(starredFolderRelativePaths.size)
                val starredFolderPaths = PholderTagUtil.getAbsolutePathFromPublicRoot(starredFolderRelativePaths)
                starredFolderPaths.forEach { starredFolderPath ->
                    val file = File(starredFolderPath)
                    val folderTag = FolderTag(file)
                    folderTag.isStarred = true
                    starredFolderTagSet.add(folderTag)
                }
                prefManager.put(PREF_IS_SUGGESTED_STAR_FOLDER_PATHS_ADDED, true)
                folderTagDao.update(starredFolderTagSet.toList())
            }
            d("totalDuration = ${System.currentTimeMillis() - start}")
        }

        // getLastUpdateTime
        fun getLastUpdateTime(): Long {
            return lastUpdateTime
        }

        // dataUpdated
        private fun dataUpdated() {
            lastUpdateTime = System.currentTimeMillis()
            prefManager.put(PREF_DATABASE_LAST_UPDATE_TIME, lastUpdateTime)
        }

        // updateData
        fun updateData(context: Context, contentResolver: ContentResolver) {
            val applicationContext = context.applicationContext
            val start = System.currentTimeMillis()
            // Create pholder folder here, after write permission is obtained
            if (!PHOLDER_FOLDER.exists()) {
                if (!prefManager.get(PREF_IS_PHOLDER_FOLDER_CREATED, false)) {
                    // For first creation, star the folder
                    createFolder(applicationContext, PHOLDER_FOLDER, true)
                    prefManager.put(PREF_IS_PHOLDER_FOLDER_CREATED, true)
                } else {
                    // Else, user might have deleted the folder from another source, create silently
                    createFolder(applicationContext, PHOLDER_FOLDER, false)
                }
            }
            updateFileTags(applicationContext, contentResolver)
            updateFolderTags()
            dataUpdated()
            d("totalDuration = ${System.currentTimeMillis() - start}")
        }

        // updateFileTags
        private fun updateFileTags(context: Context, contentResolver: ContentResolver) {
            val newFileTags = ArrayList<FileTag>()
            val excludedFolderPaths = getExcludedFolderPaths()
            // Get images
            val start = System.currentTimeMillis()
            // The constants are inherited in order of MediaColumns -> Images.ImageColumns / Video.VideoColumns -> Images.Media / Video.Media.
            // Calling Images.Media / Video.Media will get all available constants.
            val imageProjection = arrayOf(
                MediaStore.MediaColumns.DATA,
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.LATITUDE,
                MediaStore.Images.ImageColumns.LONGITUDE
                // MediaScannerConnection.scanFile only adds DATE_TAKEN. DATE_ADDED and DATE_MODIFIED are not included.
                // Below are for debugging purpose
                /*MediaStore.Images.ImageColumns.DATE_MODIFIED,
                MediaStore.Images.ImageColumns.DATE_ADDED,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.BUCKET_ID,
                MediaStore.Images.ImageColumns.DESCRIPTION,
                MediaStore.Images.ImageColumns.HEIGHT,
                MediaStore.Images.ImageColumns.WIDTH,
                MediaStore.Images.ImageColumns.DISPLAY_NAME,
                MediaStore.Images.ImageColumns.IS_PRIVATE,
                MediaStore.Images.ImageColumns.MINI_THUMB_MAGIC,
                MediaStore.Images.ImageColumns.ORIENTATION,
                MediaStore.Images.ImageColumns.PICASA_ID,
                MediaStore.Images.ImageColumns.SIZE,
                MediaStore.Images.ImageColumns.TITLE,
                MediaStore.Images.ImageColumns.MIME_TYPE*/
            )
            val imageSelection =
                MediaStore.MediaColumns.DATA + " like ? AND " +
                        MediaStore.MediaColumns.MIME_TYPE + " like ?"
            val imageSelectionArgs = arrayOf(
                PUBLIC_ROOT.absolutePath + File.separator + "%",
                MIME_IMAGE_ANY
            )
            val imageSortOrder = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"
            val imageCursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                imageSelection,
                imageSelectionArgs,
                imageSortOrder
            )
            if (imageCursor != null) {
                newFileTags.ensureCapacity(imageCursor.count + 10)
                while (imageCursor.moveToNext()) {
                    val filePath = imageCursor.getString(0)
                    val mediaStoreId = imageCursor.getLong(1)
                    val dateTaken = imageCursor.getLong(2)
                    val lat = imageCursor.getDouble(3)
                    val lng = imageCursor.getDouble(4)
                    newFileTags.add(FileTag(filePath, mediaStoreId, dateTaken, 0, lat, lng))
                    // Below are for debugging purpose
                    /*val dateModified = imageCursor.getLong(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_MODIFIED))
                    val dateAdded = imageCursor.getLong(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_ADDED))
                    val bucketDisplayName = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME))
                    val bucketID = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_ID))
                    val description = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.DESCRIPTION))
                    val width = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.WIDTH))
                    val height = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT))
                    val displayName = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME))
                    val isPrivate = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.IS_PRIVATE))
                    val lat = imageCursor.getFloat(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.LATITUDE))
                    val lng = imageCursor.getFloat(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.LONGITUDE))
                    val miniThumb = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.MINI_THUMB_MAGIC))
                    val orientation = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION))
                    val picasaID = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.PICASA_ID))
                    val size = imageCursor.getLong(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.SIZE))
                    val title = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.TITLE))
                    val mime = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.MIME_TYPE))
                    d("$filePath, " +
                            "dateTaken=${PholderTagUtil.millisToDateTime(dateTaken)}, " +
                            "dateModified=${PholderTagUtil.millisToDateTime(dateModified)}, " +
                            "dateAdded=${PholderTagUtil.millisToDateTime(dateAdded)}, " +
                            "bucketDisplayName=$bucketDisplayName, " +
                            "bucketID=$bucketID, " +
                            "description=$description, " +
                            "width=$width, height=$height, " +
                            "displayName=$displayName, " +
                            "isPrivate=$isPrivate, " +
                            "lat=$lat, " +
                            "lng=$lng, " +
                            "miniThumb=$miniThumb, " +
                            "orientation=$orientation, " +
                            "picasaID=$picasaID, " +
                            "size=$size, " +
                            "title=$title, " +
                            "mime=$mime")*/
                }
                imageCursor.close()
            }
            val getImagesTime = System.currentTimeMillis()
            d("getImagesDuration = ${getImagesTime - start}")
            // Get videos
            val videoProjection = arrayOf(
                MediaStore.MediaColumns.DATA,
                MediaStore.Video.VideoColumns._ID,
                MediaStore.Video.VideoColumns.DATE_TAKEN,
                MediaStore.Video.VideoColumns.DURATION,
                MediaStore.Video.VideoColumns.LATITUDE,
                MediaStore.Video.VideoColumns.LONGITUDE
            )
            // Note that 3gp is registered as mp4 by MediaScanner
            val videoSelection =
                MediaStore.MediaColumns.DATA + " like ? AND (" +
                        MediaStore.MediaColumns.MIME_TYPE + " = ? OR " +
                        MediaStore.MediaColumns.MIME_TYPE + " = ? OR " +
                        MediaStore.MediaColumns.MIME_TYPE + " = ?)"
            val videoSelectionArgs = arrayOf(
                PUBLIC_ROOT.absolutePath + File.separator + "%",
                MIME_VIDEO_MP4,
                MIME_VIDEO_WEBM,
                MIME_VIDEO_TGP
            )
            val videoSortOrder = MediaStore.Video.VideoColumns.DATE_TAKEN + " DESC"
            val videoCursor = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                videoSelection,
                videoSelectionArgs,
                videoSortOrder
            )
            if (videoCursor != null) {
                newFileTags.ensureCapacity(newFileTags.size + videoCursor.count + 10)
                while (videoCursor.moveToNext()) {
                    val filePath = videoCursor.getString(0)
                    val mediaStoreId = videoCursor.getLong(1)
                    val dateTaken = videoCursor.getLong(2)
                    val duration = videoCursor.getInt(3)
                    val lat = videoCursor.getDouble(4)
                    val lng = videoCursor.getDouble(5)
                    newFileTags.add(FileTag(filePath, mediaStoreId, dateTaken, duration, lat, lng))
                }
                videoCursor.close()
            }
            val getVideosTime = System.currentTimeMillis()
            d("getVideosDuration = ${getVideosTime - getImagesTime}")
            // Convert to hashMap for quick position lookup, also check for excluded files
            val positionMap = HashMap<String, Int>(newFileTags.size)
            newFileTags.forEachWithIndex { i, fileTag ->
                val filePath = fileTag.getFilePath()
                if (PholderTagUtil.isExcludedFile(filePath, excludedFolderPaths)) {
                    fileTag.exist = false
                }
                positionMap[filePath] = i
            }
            // Copy from MediaStore to local db
            fileTagDao.deleteAll()
            fileTagDao.update(newFileTags)
            val copyMediaStoreTime = System.currentTimeMillis()
            d("copyMediaStoreTime = ${copyMediaStoreTime - getVideosTime}, size = ${newFileTags.size}")
            // Check fileActions
            val fileActions = fileActionDao.getAll()
            if (fileActions.isNotEmpty()) {
                val addFileTags = mutableListOf<FileTag>()
                val deleteFileTags = mutableListOf<FileTag>()
                val completedFileActions = ArrayList<FileAction>(fileActions.size)
                fileActions.forEach { fileAction ->
                    val position = positionMap[fileAction.getFilePath()] ?: -1
                    when (fileAction.action) {
                        ACTION_DELETE -> {
                            if (position >= 0) {
                                // Delete from local db if still exist in MediaStore
                                val fileTag = newFileTags[position]
                                if (!fileTag.checkExist()) {
                                    deleteFileTags.add(fileTag)
                                    d("${fileAction.getFilePath()} - ACTION_DELETE")
                                }
                            } else {
                                // Mark as complete if not exists in MediaStore
                                d("${fileAction.getFilePath()} - ACTION_DELETE Completed")
                                completedFileActions.add(fileAction)
                            }
                        }
                        ACTION_ADD -> {
                            if (position >= 0) {
                                // Mark as complete if exists in MediaStore
                                completedFileActions.add(fileAction)
                                d("${fileAction.getFilePath()} - ACTION_ADD Completed")
                            } else {
                                // Add to local db if not exists in MediaStore
                                val fileTag = fileAction.toFileTag()
                                if (fileTag.checkExist()) {
                                    addFileTags.add(fileTag)
                                    d("${fileAction.getFilePath()} - ACTION_ADD")
                                }
                            }
                        }
                    }
                }
                // Mark as not exist for excluded addFileTags
                addFileTags.forEach { addFileTag ->
                    if (PholderTagUtil.isExcludedFile(addFileTag.getFilePath(), excludedFolderPaths)) {
                        addFileTag.exist = false
                        d("${addFileTag.getFilePath()} excluded")
                    }
                }
                // Sync fileActions to local db
                fileTagDao.update(addFileTags)
                fileTagDao.delete(deleteFileTags)
                fileActionDao.delete(completedFileActions)
                val checkFileActionTime = System.currentTimeMillis()
                d("checkFileActionTime = ${checkFileActionTime - copyMediaStoreTime}, size = ${fileActions.size}")
                // Rescan the filePaths which are not in sync
                if (addFileTags.isNotEmpty() || deleteFileTags.isNotEmpty()) {
                    val scanPairs = ArrayList<Pair<String, Int>>(addFileTags.size + deleteFileTags.size)
                    addFileTags.mapTo(scanPairs) { Pair(it.getFilePath(), SCAN_ACTION_FILE_ADD) }
                    deleteFileTags.mapTo(scanPairs) { Pair(it.getFilePath(), SCAN_ACTION_FILE_DELETE) }
                    MediaIntentService.scanMediaPaths(context, scanPairs)
                }
            }
            dataUpdated()
            d("totalDuration = ${System.currentTimeMillis() - start}")
        }

        // updateFolderTags
        private fun updateFolderTags() {
            val start = System.currentTimeMillis()
            val includedFolderPaths = getIncludedFolderPaths()
            val excludedFolderPaths = getExcludedFolderPaths()
            // Use hashSet to ensure uniqueness
            val newFolderTagSet = HashSet<FolderTag>()
            includedFolderPaths.forEach { includedFolderPath ->
                walkFoldersReversed(File(includedFolderPath), newFolderTagSet, excludedFolderPaths)
            }
            val walkFoldersTime = System.currentTimeMillis()
            d("walkFoldersDuration = ${walkFoldersTime - start}")
            // Add 'All Videos' folder
            val allVideos = FolderTag(ALL_VIDEOS_FOLDER)
            if (prefManager.get(PREF_SHOW_ALL_VIDEOS_FOLDER, true)) {
                // Mark as starred. This can happen when this folder is first created, or when user toggle to show
                // after hiding this folder.
                if (prefManager.get(PREF_ALL_VIDEOS_FOLDER_STAR, true)) {
                    allVideos.isStarred = true
                    prefManager.put(PREF_ALL_VIDEOS_FOLDER_STAR, false)
                }
                newFolderTagSet.add(allVideos)
            } else {
                newFolderTagSet.remove(allVideos)
            }
            val newFolderTags = newFolderTagSet.toMutableList()
            // Assign user created status
            val userCreatedFolderPaths = folderTagDao.getUserCreatedFolderPaths()
            userCreatedFolderPaths.forEach { userCreatedFolderPath ->
                for (folderTag in newFolderTagSet) {
                    if (folderTag.getFilePath() == userCreatedFolderPath) {
                        folderTag.isUserCreated = true
                        break
                    }
                }
            }
            val assignIsUserCreatedTime = System.currentTimeMillis()
            d("assignIsUserCreatedDuration = ${assignIsUserCreatedTime - walkFoldersTime}")
            // Assign starred status
            val starredFolderPaths = folderTagDao.getStarredFolderPaths()
            starredFolderPaths.forEach { starredFolderPath ->
                for (folderTag in newFolderTagSet) {
                    if (folderTag.getFilePath() == starredFolderPath) {
                        folderTag.isStarred = true
                        break
                    }
                }
            }
            val assignIsStarredTime = System.currentTimeMillis()
            d("assignIsStarredDuration = ${assignIsStarredTime - assignIsUserCreatedTime}")
            val fileTags = fileTagDao.getAll()
            // Assign thumbnail
            assignThumbnail(newFolderTags, fileTags)
            val assignThumbnailTime = System.currentTimeMillis()
            d("assignThumbnailDuration = ${assignThumbnailTime - assignIsStarredTime}")
            // Assign item count
            assignItemCount(newFolderTags, fileTags, excludedFolderPaths)
            val assignFolderCountTime = System.currentTimeMillis()
            d("assignFolderCountDuration = ${assignFolderCountTime - assignThumbnailTime}")
            folderTagDao.deleteAll()
            folderTagDao.update(newFolderTags)
            d("totalDuration = ${System.currentTimeMillis() - start}, size = ${newFolderTags.size}")
        }

        // getIncludedFolderPaths
        private fun getIncludedFolderPaths(): List<String> {
            // Use hashSet to ensure uniqueness
            val includedFolderPathsSet = HashSet<String>()
            // Get folder paths that has media
            includedFolderPathsSet.addAll(folderTagDao.getDistinctFolderPaths())
            // Get all user created folder paths
            includedFolderPathsSet.addAll(folderTagDao.getUserCreatedFolderPaths())
            // Get all starred folder paths
            includedFolderPathsSet.addAll(folderTagDao.getStarredFolderPaths())
            // Get user defined folder paths
            includedFolderPathsSet.addAll(prefManager.getStringArray(PREF_ARRAY_INCLUDED_FOLDER_PATHS))
            // Get created folder paths
            includedFolderPathsSet.addAll(prefManager.getStringArray(PREF_ARRAY_CREATED_FOLDER_PATHS))
            return includedFolderPathsSet.toList()
        }

        // getExcludedFolderPaths
        private fun getExcludedFolderPaths(): List<String> {
            return excludedFolderPaths.toList()
        }

        // updateExcludedFolderPaths
        fun updateExcludedFolderPaths() {
            // Use hashSet to ensure uniqueness
            val excludedFolderPathsSet = HashSet<String>()
            // Get user defined folder paths
            excludedFolderPathsSet.addAll(prefManager.getStringArray(PREF_ARRAY_EXCLUDED_FOLDER_PATHS))
            excludedFolderPaths.clear()
            excludedFolderPaths.addAll(excludedFolderPathsSet)
            excludedFolderPaths.sort()
        }

        // walkActualFolders
        private fun walkFolders(file: File, folderTagSet: HashSet<FolderTag>, excludedFolderPaths: List<String>) {
            if (file.isDirectory && PholderTagUtil.isValidFile(file, excludedFolderPaths)) {
                val folderTag = FolderTag(file)
                val subFiles = file.listFiles()
                if (subFiles != null) {
                    for (subFile in subFiles) {
                        walkFolders(subFile, folderTagSet, excludedFolderPaths)
                    }
                }
                folderTagSet.add(folderTag)
            }
        }

        // walkActualFolders
        private fun walkFoldersReversed(
            file: File,
            folderTagSet: HashSet<FolderTag>,
            excludedFolderPaths: List<String>
        ) {
            if (file.exists() && file.isDirectory && PholderTagUtil.isValidFile(file, excludedFolderPaths)) {
                val folderTag = FolderTag(file)
                val parentFile = file.parentFile
                if (file != PUBLIC_ROOT && parentFile != null && parentFile != PUBLIC_ROOT) {
                    walkFoldersReversed(parentFile, folderTagSet, excludedFolderPaths)
                }
                folderTagSet.add(folderTag)
            }
        }

        // assignThumbnail
        private fun assignThumbnail(folderTags: MutableList<FolderTag>, fileTags: List<FileTag>) {
            // Make sure start from the deepest folder out
            folderTags.sortByDescending { it.getFilePath().length }
            // Assign thumbnail if available
            folderTags.forEach { folderTag ->
                // Reset thumbnail
                folderTag.thumbnail = ""
                val filePath = folderTag.getFilePath()
                // Specific assignment for 'All Videos' folder
                if (filePath == ALL_VIDEOS_FOLDER.absolutePath) {
                    for (fileTag in fileTags) {
                        if (fileTag.isVideo()) {
                            folderTag.thumbnail = fileTag.getGlideLoadPath()
                            if (fileTag.dateTaken > folderTag.lastModified) {
                                folderTag.lastModified = fileTag.dateTaken
                            }
                            break
                        }
                    }
                } else {
                    for (fileTag in fileTags) {
                        if (fileTag.parentPath == filePath) {
                            folderTag.thumbnail = fileTag.getGlideLoadPath()
                            if (fileTag.dateTaken > folderTag.lastModified) {
                                folderTag.lastModified = fileTag.dateTaken
                            }
                            break
                        }
                    }
                }
            }
            // From the deepest folder out i.e. 0 = deepest folder
            folderTags.forEachWithIndex { i, folderTag ->
                // Ignore deepest folder
                if (i - 1 >= 0) {
                    if (folderTag.thumbnail.isEmpty()) {
                        val filePath = folderTag.getFilePath()
                        // Revisit already assigned folder in forward direction
                        for (j in i - 1 downTo 0) {
                            val subFolderTag = folderTags[j]
                            if (subFolderTag.isChildOf(filePath)) {
                                if (subFolderTag.thumbnail.isNotEmpty()) {
                                    // Assign thumbnail as whichever first subFolder with thumbnail
                                    folderTag.thumbnail = subFolderTag.thumbnail
                                    break
                                }
                            }/* else {
                                // This has gone past the subFolders region, pause search
                                break
                            }*/
                        }
                    }
                }
            }
        }

        // assignItemCount
        private fun assignItemCount(
            folderTags: List<FolderTag>,
            fileTags: List<FileTag>,
            excludedFolderPaths: List<String>
        ) {
            // Reset count
            folderTags.forEach { folderTag ->
                folderTag.folderCount = 0
                folderTag.fileCount = 0
            }
            // Create a map for easy access
            val folderMap = folderTags.map { folderTag ->
                folderTag.getFilePath() to folderTag
            }.toMap()
            // Count folders
            val showEmptyFolders = prefManager.get(PREF_SHOW_EMPTY_FOLDERS, false)
            for (folderTag in folderTags) {
                if (!showEmptyFolders) {
                    val parentFolder = folderMap[folderTag.parentPath]
                    if (parentFolder != null) {
                        parentFolder.folderCount++
                    }
                } else {
                    // The folderTags do not include empty folders, hence we need to list files and recount
                    val file = folderTag.toFile()
                    folderTag.folderCount = PholderTagUtil.countActualFolders(file, excludedFolderPaths)
                }
            }
            // Count assignment for 'All Videos' folder
            var videoCount = 0
            // Count files
            fileTags.forEach { fileTag ->
                val folderTag = folderMap[fileTag.parentPath]
                if (folderTag != null) {
                    folderTag.fileCount++
                }
                if (fileTag.isVideo()) {
                    videoCount++
                }
            }
            // Assign count for 'All Videos' folder
            val allVideosFolderTag =
                PholderTagUtil.getPholderTag(folderTags, ALL_VIDEOS_FOLDER.absolutePath) as? FolderTag
            allVideosFolderTag?.fileCount = videoCount
        }

        // buildFolderItems
        fun buildFolderItems(
            rootFile: File,
            showEmptyFolders: Boolean
        ): MutableList<FolderTag> {
            // Get folders from db
            val folderTags = folderTagDao.getChildren(rootFile.absolutePath).toMutableList()
            if (showEmptyFolders) {
                // In order to show empty folders, compare to actual files
                val files = rootFile.listFiles()
                val folderTagsActual = mutableListOf<FolderTag>()
                val excludedFolderPaths = PholderDatabase.getExcludedFolderPaths()
                files?.forEach { file ->
                    if (file.isDirectory && PholderTagUtil.isValidFile(file, excludedFolderPaths)) {
                        folderTagsActual.add(FolderTag(file))
                    }
                }
                // Check if actual folder matches db. If not, count folders in the empty folders and add to list.
                folderTagsActual.forEach { folderTagActual ->
                    if (!folderTags.contains(folderTagActual)) {
                        folderTagActual.folderCount =
                            PholderTagUtil.countActualFolders(folderTagActual.toFile(), excludedFolderPaths)
                        folderTags.add(folderTagActual)
                    }
                }
            }
            return folderTags
        }

        // sortFolderItems
        fun sortFolderItems(folderTags: MutableList<FolderTag>, sortBy: Int, starFirst: Boolean = true) {
            if (starFirst) {
                when (sortBy) {
                    SORT_ORDER_DATE_ASC -> {
                        folderTags.sortWith(compareByDescending<FolderTag> { it.isStarred }.thenBy { it.lastModified })
                    }
                    SORT_ORDER_DATE_DESC -> {
                        folderTags.sortWith(compareByDescending<FolderTag> { it.isStarred }.thenByDescending { it.lastModified })
                    }
                    SORT_ORDER_NAME_ASC -> {
                        folderTags.sortWith(compareByDescending<FolderTag> { it.isStarred }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.fileName })
                    }
                    SORT_ORDER_NAME_DESC -> {
                        folderTags.sortWith(compareByDescending<FolderTag> { it.isStarred }.thenByDescending(String.CASE_INSENSITIVE_ORDER) { it.fileName })
                    }
                }
            } else {
                when (sortBy) {
                    SORT_ORDER_DATE_ASC -> {
                        folderTags.sortBy { it.lastModified }
                    }
                    SORT_ORDER_DATE_DESC -> {
                        folderTags.sortByDescending { it.lastModified }
                    }
                    SORT_ORDER_NAME_ASC -> {
                        folderTags.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.fileName })
                    }
                    SORT_ORDER_NAME_DESC -> {
                        folderTags.sortWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.fileName })
                    }
                }
            }
        }

        // sortFileItems
        @Suppress("UNCHECKED_CAST")
        private fun buildFileItems(
            fileTags: MutableList<FileTag>,
            sortBy: Int = SORT_ORDER_DATE_DESC
        ): MutableList<PholderTag> {
            when (sortBy) {
                SORT_ORDER_DATE_ASC -> {
                    val newFileTags = mutableListOf<PholderTag>()
                    val dateMap = buildFileDateMap(fileTags)
                    fileTags.clear()
                    // forEach here needs to have "( )" due to Java8 not available on some Android versions.
                    // See https://stackoverflow.com/a/42869245/3584439
                    dateMap.toSortedMap(compareBy { it }).forEach { (date, fileTagList) ->
                        if (fileTagList.isNotEmpty()) {
                            newFileTags.add(TitleTag.newDate(date))
                            newFileTags += fileTagList
                        }
                    }
                    return newFileTags
                }
                SORT_ORDER_DATE_DESC -> {
                    val newFileTags = mutableListOf<PholderTag>()
                    val dateMap = buildFileDateMap(fileTags)
                    fileTags.clear()
                    // forEach here needs to have "( )" due to Java8 not available on some Android versions.
                    // See https://stackoverflow.com/a/42869245/3584439
                    dateMap.toSortedMap(compareByDescending { it }).forEach { (date, fileTagList) ->
                        if (fileTagList.isNotEmpty()) {
                            newFileTags.add(TitleTag.newDate(date))
                            newFileTags += fileTagList
                        }
                    }
                    return newFileTags
                }
                SORT_ORDER_NAME_ASC -> {
                    fileTags.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.fileName })
                    return fileTags as MutableList<PholderTag>
                }
                SORT_ORDER_NAME_DESC -> {
                    fileTags.sortWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.fileName })
                    return fileTags as MutableList<PholderTag>
                }
                else -> {
                    return fileTags as MutableList<PholderTag>
                }
            }
        }

        // buildFileDateMap
        private fun buildFileDateMap(fileTags: List<FileTag>): Map<Long, MutableList<FileTag>> {
            val dateMap = mutableMapOf<Long, MutableList<FileTag>>()
            fileTags.forEach { fileTag ->
                val roundedMillis = PholderTagUtil.roundDownMillisToDay(fileTag.dateTaken)
                var fileTagList = dateMap[roundedMillis]
                // Add into map if list is not available
                if (fileTagList == null) {
                    fileTagList = mutableListOf()
                    dateMap[roundedMillis] = fileTagList
                }
                fileTagList.add(fileTag)
            }
            return dateMap
        }

        // buildFolderFileItems
        fun buildFolderFileItems(
            context: Context?,
            folderTags: MutableList<FolderTag>,
            fileTags: MutableList<FileTag>
        ): List<PholderTag> {
            val items = java.util.ArrayList<PholderTag>(folderTags.size + 10 + (fileTags.size * 1.1).toInt())
            if (folderTags.isNotEmpty()) {
                sortFolderItems(folderTags, prefManager.get(PREF_SORT_ORDER_FOLDER, SORT_ORDER_NAME_ASC))
                // Add title
                if (context != null) {
                    items.add(
                        TitleTag.newTitle(
                            context.resources.getString(R.string.categoryLayout_category_folders)
                        )
                    )
                }
                items += folderTags
            }
            if (fileTags.isNotEmpty()) {
                val mediaSortOrder = prefManager.get(PREF_SORT_ORDER_MEDIA, SORT_ORDER_DATE_DESC)
                if (mediaSortOrder == SORT_ORDER_NAME_ASC || mediaSortOrder == SORT_ORDER_NAME_DESC) {
                    if (context != null) {
                        items.add(
                            TitleTag.newTitle(
                                context.resources.getString(R.string.categoryLayout_category_media)
                            )
                        )
                    }
                }
                val fileItems = buildFileItems(fileTags, mediaSortOrder)
                items += fileItems
            }
            return items
        }

        // createFolder
        fun createFolder(context: Context, newFolder: File, starFolder: Boolean): Boolean {
            val applicationContext = context.applicationContext
            val isFolderCreated = newFolder.mkdirs()
            if (isFolderCreated) {
                val newFolderTag = FolderTag(newFolder)
                newFolderTag.isUserCreated = true
                newFolderTag.isStarred = starFolder
                folderTagDao.update(listOf(newFolderTag))
                // Update parents folder count
                val parentFolderTag = folderTagDao.get(newFolderTag.parentPath)
                if (parentFolderTag != null) {
                    parentFolderTag.folderCount++
                    folderTagDao.update(listOf(parentFolderTag))
                }
                MediaIntentService.scanMediaPaths(
                    applicationContext,
                    listOf(Pair(newFolder.absolutePath, SCAN_ACTION_FOLDER_ADD_THIS))
                )
                dataUpdated()
            }
            // Do not perform media scan because this will show the folder as a file instead in MTP
            return isFolderCreated
        }

        // addMediaTaken
        fun addMediaTaken(context: Context, filePath: String, lat: Double = 0.0, lng: Double = 0.0) {
            val applicationContext = context.applicationContext
            val start = System.currentTimeMillis()
            // Add fileTag into database
            val fileTag = FileTag(filePath, -1, true, true)
            fileTag.lat = lat
            fileTag.lng = lng
            fileTagDao.update(listOf(fileTag))
            fileActionDao.update(listOf(FileAction(fileTag, ACTION_ADD, start)))
            // Get parent folderTag
            var folderTag = folderTagDao.get(fileTag.parentPath)
            // Generate based on file in case it was empty and not in db
            if (folderTag == null) {
                val file = File(fileTag.parentPath)
                folderTag = FolderTag(file)
                // Need to count folder since it was not available in db
                folderTag.folderCount = PholderTagUtil.countActualFolders(file, getExcludedFolderPaths())
            }
            folderTag.fileCount++
            // update parent thumbnail
            val newFolderTags =
                updateParentThumbnail(folderTag.getFilePath(), folderTag.thumbnail, filePath).toMutableList()
            // update self
            folderTag.thumbnail = filePath
            folderTag.lastModified = fileTag.dateTaken
            newFolderTags.add(folderTag)
            // update 'All Videos' folder
            if (fileTag.isVideo()) {
                val allVideosFolderTag = folderTagDao.get(ALL_VIDEOS_FOLDER.absolutePath)
                if (allVideosFolderTag != null) {
                    allVideosFolderTag.thumbnail = filePath
                    allVideosFolderTag.lastModified = fileTag.dateTaken
                    allVideosFolderTag.fileCount++
                    newFolderTags.add(allVideosFolderTag)
                }
            }
            folderTagDao.update(newFolderTags)
            MediaIntentService.scanMediaPaths(applicationContext, listOf(Pair(filePath, SCAN_ACTION_FILE_ADD)))
            dataUpdated()
            d("duration = ${System.currentTimeMillis() - start}")
        }

        // updateParentThumbnail
        private fun updateParentThumbnail(
            filePath: String,
            oldThumbnailPath: String,
            newThumbnailPath: String
        ): List<FolderTag> {
            // This operation assumes that all thumbnails are already assigned
            val newFolderTags = mutableListOf<FolderTag>()
            // Update parents in case thumbnail was same as current folder i.e. empty parent
            val folderTags = folderTagDao.getAll()
            folderTags.forEach { folderTag ->
                if (folderTag.isParentOf(filePath) &&
                    folderTag.thumbnail == oldThumbnailPath
                ) {
                    folderTag.thumbnail = newThumbnailPath
                    newFolderTags.add(folderTag)
                }
            }
            return newFolderTags
        }

        // deleteFiles
        @Suppress("UNCHECKED_CAST")
        fun deleteFiles(context: Context, pholderTags: List<PholderTag>?): List<Pair<String, Int>> {
            val applicationContext = context.applicationContext
            val start = System.currentTimeMillis()
            if (pholderTags != null) {
                val scanPairs = ArrayList<Pair<String, Int>>()
                val resultPairs = ArrayList<Pair<String, Int>>()
                // Process fileTags
                val fileTags = pholderTags.filter { it.getType() == TYPE_FILE } as List<FileTag>
                if (fileTags.isNotEmpty()) {
                    scanPairs.ensureCapacity(fileTags.size)
                    resultPairs.ensureCapacity(fileTags.size)
                    val fileActions = ArrayList<FileAction>(fileTags.size)
                    fileTags.forEach { fileTag ->
                        val filePath = fileTag.getFilePath()
                        val file = fileTag.toFile()
                        file.delete()
                        val isDeleted = !file.exists()
                        if (isDeleted) {
                            fileTag.exist = false
                            fileActions.add(FileAction(fileTag, ACTION_DELETE, System.currentTimeMillis()))
                            scanPairs.add(Pair(filePath, SCAN_ACTION_FILE_DELETE))
                            resultPairs.add(Pair(filePath, ACTION_STATUS_OK))
                        } else {
                            resultPairs.add(Pair(filePath, ACTION_STATUS_FAILED))
                        }
                        d("filePath = $filePath, isDeleted = $isDeleted")
                    }
                    fileTagDao.update(fileTags)
                    fileActionDao.update(fileActions)
                }
                // Process folderTags
                val folderTags = pholderTags.filter { it.getType() == PholderTag.TYPE_FOLDER } as List<FolderTag>
                if (folderTags.isNotEmpty()) {
                    val deletedFileMap = HashMap<String, MutableList<String>>()
                    folderTags.forEach { folderTag ->
                        val folder = folderTag.toFile()
                        val folderPath = folderTag.getFilePath()
                        // Only proceed to delete children if is not system file
                        if (!PholderTagUtil.isTopSystemFolder(folder)) {
                            val childFileTags = fileTagDao.getChildren(
                                folderPath,
                                existOnly = false,
                                includeAll = true
                            )
                            resultPairs.ensureCapacity(resultPairs.size + childFileTags.size)
                            val fileActions = ArrayList<FileAction>(childFileTags.size)
                            // Delete child files
                            childFileTags.forEach { childFileTag ->
                                val childFilePath = childFileTag.getFilePath()
                                val childFile = childFileTag.toFile()
                                childFile.delete()
                                val isDeleted = !childFile.exists()
                                if (isDeleted) {
                                    childFileTag.exist = false
                                    fileActions.add(FileAction(childFileTag, ACTION_DELETE, System.currentTimeMillis()))
                                    var deletedFiles = deletedFileMap[childFileTag.parentPath]
                                    if (deletedFiles == null) {
                                        deletedFiles = mutableListOf()
                                        deletedFileMap[childFileTag.parentPath] = mutableListOf()
                                    }
                                    deletedFiles.add(childFilePath)
                                    resultPairs.add(Pair(childFilePath, ACTION_STATUS_OK))
                                } else {
                                    resultPairs.add(Pair(childFilePath, ACTION_STATUS_FAILED))
                                }
                                d("childFilePath = $childFilePath, isDeleted = $isDeleted")
                            }
                            // Update db
                            fileTagDao.update(childFileTags)
                            fileActionDao.update(fileActions)
                            // Get actual folders because folderTagDao may not include empty folders
                            val subFolderTagSet = hashSetOf<FolderTag>()
                            walkFolders(folder, subFolderTagSet, listOf())
                            val subFolderTags = subFolderTagSet.toMutableList()
                            resultPairs.ensureCapacity(resultPairs.size + subFolderTags.size)
                            val deletedFolderPaths = ArrayList<String>(subFolderTags.size)
                            // Delete sub folders, do it in reverse to start from the deepest folder
                            subFolderTags.sortByDescending { it.getFilePath().length }
                            subFolderTags.forEach { subFolderTag ->
                                val subFolder = subFolderTag.toFile()
                                val subFolderPath = subFolderTag.getFilePath()
                                // Do not allow system folders to be deleted
                                if (!PholderTagUtil.isTopSystemFolder(subFolder)) {
                                    subFolder.delete()
                                    val isDeleted = !subFolder.exists()
                                    if (isDeleted) {
                                        // Delete directly since not used by MediaIntentService
                                        folderTagDao.delete(subFolderTag)
                                        deletedFolderPaths.add(subFolderPath)
                                        resultPairs.add(Pair(subFolderPath, ACTION_STATUS_OK))
                                    } else {
                                        resultPairs.add(Pair(subFolderPath, ACTION_STATUS_FAILED))
                                    }
                                    d("subFolderFilePath  = $subFolderPath, isDeleted = $isDeleted")
                                } else {
                                    resultPairs.add(Pair(subFolderPath, ACTION_STATUS_SYSTEM_FOLDER_NOT_ALLOWED))
                                }
                            }
                            // If folder deleted completely, use the top folder for scan
                            if (!folder.exists()) {
                                scanPairs.add(Pair(folderPath, SCAN_ACTION_FOLDER_DELETE))
                            } else {
                                // Else, will have to scan each individual folder that is deleted
                                deletedFolderPaths.forEach { deletedFolderPath ->
                                    if (deletedFileMap.containsKey(deletedFolderPath)) {
                                        deletedFileMap.remove(deletedFolderPath)
                                    }
                                    scanPairs.add(Pair(deletedFolderPath, SCAN_ACTION_FOLDER_DELETE))
                                }
                                // Remainder of map will be files which parent folder is not deleted
                                deletedFileMap.values.forEach { deletedFilePaths ->
                                    deletedFilePaths.forEach { deletedFilePath ->
                                        scanPairs.add(Pair(deletedFilePath, SCAN_ACTION_FILE_DELETE))
                                    }
                                }
                            }
                        } else {
                            // Mark 'All Videos' folder as system folder
                            resultPairs.add(Pair(folderPath, ACTION_STATUS_SYSTEM_FOLDER_NOT_ALLOWED))
                        }
                    }
                }
                updateFolderTags()
                if (scanPairs.isNotEmpty()) {
                    MediaIntentService.scanMediaPaths(applicationContext, scanPairs)
                }
                dataUpdated()
                d("duration = ${System.currentTimeMillis() - start}")
                return resultPairs
            } else {
                return listOf()
            }
        }

        // moveFiles
        @Suppress("UNCHECKED_CAST")
        fun moveFiles(
            context: Context,
            pholderTags: List<PholderTag>?,
            destinationRootPath: String = "",
            fileName: String = ""
        ): List<Pair<String, Int>> {
            val applicationContext = context.applicationContext
            val start = System.currentTimeMillis()
            if (pholderTags != null) {
                val isRenameAction = fileName.isNotEmpty()
                val scanPairs = ArrayList<Pair<String, Int>>(pholderTags.size)
                // Twice capacity for old and new files
                val resultPairs = ArrayList<Pair<String, Int>>(pholderTags.size * 2)
                var i = 0
                // Process fileTags
                val fileTags = pholderTags.filter { it.getType() == TYPE_FILE } as List<FileTag>
                if (fileTags.isNotEmpty()) {
                    val newFileTags = ArrayList<FileTag>(fileTags.size)
                    // Twice capacity for old and new files
                    val fileActions = ArrayList<FileAction>(fileTags.size * 2)
                    fileTags.forEach { fileTag ->
                        val file = fileTag.toFile()
                        val filePath = file.absolutePath
                        val newFile = File(destinationRootPath, fileTag.getFileNameWithExtension())
                        if (!newFile.exists()) {
                            val isRenamed = file.renameTo(newFile)
                            if (isRenamed) {
                                // create new tag
                                val newFileTag = fileTag.copy()
                                newFileTag.mediaStoreId = -1L
                                newFileTag.parentPath = destinationRootPath
                                newFileTags.add(newFileTag)
                                // delete old tag
                                fileTag.exist = false
                                scanPairs.add(Pair(filePath, SCAN_ACTION_FILE_DELETE))
                                scanPairs.add(Pair(newFile.absolutePath, SCAN_ACTION_FILE_ADD))
                                fileActions.add(FileAction(fileTag, ACTION_DELETE, System.currentTimeMillis()))
                                fileActions.add(FileAction(newFileTag, ACTION_ADD, System.currentTimeMillis()))
                                resultPairs.add(Pair(filePath, ACTION_STATUS_OK))
                            } else {
                                resultPairs.add(Pair(filePath, ACTION_STATUS_FAILED))
                            }
                            d("filePath  = $filePath, isRenamed = $isRenamed")
                        } else {
                            resultPairs.add(Pair(filePath, ACTION_STATUS_FILE_COLLISION))
                            d("filePath  = $filePath, isRenamed = ACTION_STATUS_FILE_COLLISION")
                        }
                    }
                    // Update old tags as not exist
                    fileTagDao.update(fileTags)
                    // Update new tags
                    fileTagDao.update(newFileTags)
                    // Update fileActions
                    fileActionDao.update(fileActions)
                }
                // Process folderTags
                val folderTags = pholderTags.filter { it.getType() == PholderTag.TYPE_FOLDER } as List<FolderTag>
                if (folderTags.isNotEmpty()) {
                    for (folderTag in folderTags) {
                        val folder = folderTag.toFile()
                        val folderPath = folderTag.getFilePath()
                        // Do not allow system folders to be changed
                        if (!PholderTagUtil.isTopSystemFolder(folder)) {
                            val newFile: File
                            // For rename, generate folder fileName with index if required
                            if (isRenameAction) {
                                var tempFile = File(folder.parentFile, fileName)
                                while (tempFile.exists()) {
                                    i++
                                    tempFile = File(folder.parentFile, "$fileName ($i)")
                                }
                                newFile = tempFile
                            }
                            // For moving folder
                            else {
                                // If moving folder into self or subFolders
                                val terminatedDestinationRootPath = destinationRootPath + File.separator
                                if (terminatedDestinationRootPath.contains(folderPath + File.separator)) {
                                    resultPairs.add(Pair(folderPath, ACTION_STATUS_MOVE_INTO_SELF))
                                    // skip to next folder
                                    continue
                                } else {
                                    newFile = File(destinationRootPath, folderTag.fileName)
                                }
                            }
                            // Perform rename / move
                            if (!newFile.exists()) {
                                val isRenamed = folder.renameTo(newFile)
                                if (isRenamed) {
                                    // Update all child fileTags
                                    val childFileTags = fileTagDao.getChildren(
                                        folderPath,
                                        existOnly = false,
                                        includeAll = true
                                    )
                                    val newChildFileTags = ArrayList<FileTag>(childFileTags.size)
                                    val fileActions = ArrayList<FileAction>(childFileTags.size * 2)
                                    childFileTags.forEach { childFileTag ->
                                        // Create new fileTag
                                        val newChildFileTag = childFileTag.copy()
                                        newChildFileTag.mediaStoreId = -1L
                                        newChildFileTag.parentPath =
                                            childFileTag.parentPath.replaceFirst(folderPath, newFile.absolutePath)
                                        newChildFileTags.add(newChildFileTag)
                                        // Mark old fileTag as not exist
                                        childFileTag.exist = false
                                        fileActions.add(
                                            FileAction(
                                                childFileTag,
                                                ACTION_DELETE,
                                                System.currentTimeMillis()
                                            )
                                        )
                                        fileActions.add(
                                            FileAction(
                                                newChildFileTag,
                                                ACTION_ADD,
                                                System.currentTimeMillis()
                                            )
                                        )
                                    }
                                    // Update old fileTags
                                    fileTagDao.update(childFileTags)
                                    // Update new fileTags
                                    fileTagDao.update(newChildFileTags)
                                    // Update fileActions
                                    fileActionDao.update(fileActions)
                                    // Update all sub folders
                                    val subFolderTags = folderTagDao.getChildren(folderPath, includeAll = true)
                                    val newSubFolderTags = ArrayList<FolderTag>(subFolderTags.size)
                                    subFolderTags.forEach { subFolderTag ->
                                        // Create new tag
                                        val newSubFolderTag = subFolderTag.copy()
                                        newSubFolderTag.parentPath =
                                            subFolderTag.parentPath.replaceFirst(folderPath, newFile.absolutePath)
                                        newSubFolderTag.lastModified = newFile.lastModified()
                                        newSubFolderTags.add(newSubFolderTag)
                                    }
                                    // Delete old folderTags
                                    val deletedChildren = folderTagDao.deleteChildren(folderPath)
                                    d("deletedChildren = $deletedChildren")
                                    // Update new tags
                                    folderTagDao.update(newSubFolderTags)
                                    // Update self
                                    val newFolderTag = folderTag.copy()
                                    // For rename, only file name is changed
                                    if (isRenameAction) {
                                        newFolderTag.fileName = newFile.name
                                    } else {
                                        // For move file, only parent path is changed
                                        newFolderTag.parentPath = destinationRootPath
                                    }
                                    // Change the modified time
                                    newFolderTag.lastModified = newFile.lastModified()
                                    // Delete old self
                                    folderTagDao.delete(folderTag)
                                    scanPairs.add(Pair(folderPath, SCAN_ACTION_FOLDER_DELETE))
                                    // Update new self
                                    folderTagDao.update(listOf(newFolderTag))
                                    // Only use top folder path for folder and files addition, MediaIntentService will walk folders
                                    scanPairs.add(Pair(newFile.absolutePath, SCAN_ACTION_FOLDER_ADD_ALL))
                                    resultPairs.add(Pair(folderPath, ACTION_STATUS_OK))
                                } else {
                                    resultPairs.add(Pair(folderPath, ACTION_STATUS_FAILED))
                                }
                                d("filePath  = $folderPath, isRenamed = $isRenamed")
                            } else {
                                resultPairs.add(Pair(folderPath, ACTION_STATUS_FILE_COLLISION))
                                d("filePath  = $folderPath, isRenamed = ACTION_STATUS_FILE_COLLISION")
                            }
                        } else {
                            resultPairs.add(Pair(folderPath, ACTION_STATUS_SYSTEM_FOLDER_NOT_ALLOWED))
                        }
                    }
                }
                updateFolderTags()
                if (scanPairs.isNotEmpty()) {
                    MediaIntentService.scanMediaPaths(applicationContext, scanPairs)
                }
                dataUpdated()
                d("duration = ${System.currentTimeMillis() - start}")
                return resultPairs
            }
            return listOf()
        }

        // starFolders
        @Suppress("UNCHECKED_CAST")
        fun starFolders(toStar: Boolean, pholderTags: List<PholderTag>?) {
            if (pholderTags != null) {
                val newFolderTags = ArrayList<FolderTag>(pholderTags.size)
                pholderTags as List<FolderTag>
                pholderTags.forEach { pholderTag ->
                    val newFolderTag = pholderTag.copy()
                    newFolderTag.isStarred = toStar
                    newFolderTags.add(newFolderTag)
                }
                folderTagDao.update(newFolderTags)
            }
            dataUpdated()
        }

    }

}