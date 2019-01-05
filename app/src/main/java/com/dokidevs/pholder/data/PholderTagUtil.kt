package com.dokidevs.pholder.data

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.e
import com.dokidevs.pholder.R
import com.dokidevs.pholder.data.PholderDatabase.Companion.ALL_VIDEOS_FOLDER
import com.dokidevs.pholder.data.PholderDatabase.Companion.PUBLIC_ROOT
import com.dokidevs.pholder.utils.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/*--- PholderTagUtil ---*/
object PholderTagUtil : DokiLog {

    // getFileName
    fun getFileName(filePath: String): String {
        return try {
            // Get file name without extension
            filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.lastIndexOf('.'))
        } catch (ex: Exception) {
            e(ex)
            // If there is no extension on the file, ignore the extension request
            getFileNameWithExtension(filePath)
        }
    }

    // getFileNameWithExtension
    fun getFileNameWithExtension(filePath: String): String {
        return try {
            filePath.substring(filePath.lastIndexOf(File.separator) + 1)
        } catch (ex: Exception) {
            e(ex)
            ""
        }
    }

    // getParentPath
    fun getParentPath(filePath: String): String {
        return try {
            filePath.substring(0, filePath.lastIndexOf(File.separator))
        } catch (ex: Exception) {
            e(ex)
            ""
        }
    }

    // getExtension
    fun getExtension(filePath: String): String {
        return try {
            // Get extension without '.'
            filePath.substring(filePath.lastIndexOf('.') + 1)
        } catch (ex: Exception) {
            e(ex)
            ""
        }
    }

    // isImage
    fun isImage(filePath: String): Boolean {
        return isJpg(filePath) ||
                isPng(filePath) ||
                isBmp(filePath) ||
                isGif(filePath) ||
                isWebP(filePath)
    }

    // isJpg
    fun isJpg(filePath: String): Boolean {
        return filePath.endsWith(JPG, true) || filePath.endsWith(JPEG, true)
    }

    // isPng
    fun isPng(filePath: String): Boolean {
        return filePath.endsWith(PNG, true)
    }

    // isBmp
    fun isBmp(filePath: String): Boolean {
        return filePath.endsWith(BMP, true)
    }

    // isGif
    fun isGif(filePath: String): Boolean {
        return filePath.endsWith(GIF, true)
    }

    // isGif
    fun isWebP(filePath: String): Boolean {
        return filePath.endsWith(WEBP, true)
    }

    // isVideo
    fun isVideo(filePath: String): Boolean {
        return isMp4(filePath) ||
                isWebM(filePath) ||
                is3gp(filePath)
    }

    // isMp4
    fun isMp4(filePath: String): Boolean {
        return filePath.endsWith(MP4, true)
    }

    // isWebM
    fun isWebM(filePath: String): Boolean {
        return filePath.endsWith(WEBM, true)
    }

    // is3gp
    fun is3gp(filePath: String): Boolean {
        return filePath.endsWith(TGP, true)
    }

    // isSamePath
    fun isSamePath(filePath1: String, filePath2: String): Boolean {
        return filePath1.equals(filePath2, true)
    }

    // isParentChild
    fun isParentChild(parent: String, child: String): Boolean {
        // If is parentPath child, case must be same
        return child.contains(parent + File.separator)
    }

    // isDirectParentChild
    fun isDirectParentChild(parent: String, child: String): Boolean {
        // If is parentPath child, case must be same
        return getParentPath(child) == parent
    }

    // getExternalContentUri
    fun getExternalContentUri(filePath: String): Uri {
        return if (PholderTagUtil.isVideo(filePath)) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    // getPholderTagPosition
    fun getPholderTagPosition(pholderTags: List<PholderTag>, uid: String): Int {
        pholderTags.forEachIndexed { i, pholderTag ->
            if (pholderTag.getUid() == uid) {
                return i
            }
        }
        return -1
    }

    // getPholderTag
    fun getPholderTag(pholderTags: List<PholderTag>, uid: String): PholderTag? {
        val position = getPholderTagPosition(pholderTags, uid)
        if (position > -1) {
            return pholderTags[position]
        }
        return null
    }

    // getAbsolutePathFromPublicRoot
    fun getAbsolutePathFromPublicRoot(relativePaths: List<String>): List<String> {
        val publicRootPath = PUBLIC_ROOT.absolutePath
        val absolutePaths = java.util.ArrayList<String>(relativePaths.size)
        relativePaths.mapTo(absolutePaths) { relativePath ->
            publicRootPath + File.separator + relativePath
        }
        return absolutePaths
    }

    // getRelativePathFromPublicRoot
    fun getRelativePathFromPublicRoot(absolutePath: String): String {
        return absolutePath.replaceFirst(PUBLIC_ROOT.absolutePath, "")
    }

    // isValidFile
    fun isValidFile(file: File, excludedFolderPaths: List<String>? = null): Boolean {
        return !isSystemFile(file) && !isExcludedFile(file.absolutePath, excludedFolderPaths)
    }

    // isSystemFile
    private fun isSystemFile(file: File): Boolean {
        if (file.isHidden) return true
        if (file.name.isNotEmpty() && file.name[0] == '.') return true
        return false
    }

    // isTopSystemFolder
    fun isTopSystemFolder(file: File): Boolean {
        if (file == PUBLIC_ROOT) return true
        if (file == ALL_VIDEOS_FOLDER) return true
        return PholderTagUtil.isDirectParentChild(PUBLIC_ROOT.absolutePath, file.absolutePath)
    }

    // isTopSystemFolder
    fun isTopSystemFolder(filePath: String): Boolean {
        return isTopSystemFolder(File(filePath))
    }

    // isExcludedFile
    fun isExcludedFile(filePath: String, excludedFolderPaths: List<String>?): Boolean {
        if (excludedFolderPaths == null) return false
        if (excludedFolderPaths.isEmpty()) return false
        for (excludedFolderPath in excludedFolderPaths) {
            if (filePath == excludedFolderPath || filePath.contains(excludedFolderPath + File.separator)) {
                return true
            }
        }
        return false
    }

    // countActualFolders
    fun countActualFolders(file: File, excludedFolderPaths: List<String>): Int {
        var folderCount = 0
        val subFiles = file.listFiles()
        subFiles?.forEach { subFile ->
            if (subFile.isDirectory && isValidFile(subFile, excludedFolderPaths)) {
                folderCount++
            }
        }
        return folderCount
    }

    // getDateTaken
    @SuppressLint("SimpleDateFormat")
    fun getDateTaken(filePath: String): Long {
        // Extract exif for jpg only, use lastModified for others
        try {
            if (isJpg(filePath)) {
                val dateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss")
                dateFormat.timeZone = TimeZone.getDefault()
                val date = ExifInterface(filePath).getAttribute(ExifInterface.TAG_DATETIME)
                if (date != null && date.isNotEmpty() && date != "0") {
                    return dateFormat.parse(date).time
                }
            }
            return File(filePath).lastModified()
        } catch (ex: Exception) {
            e(ex)
            return File(filePath).lastModified()
        }
    }

    // millisToDateTime
    @SuppressLint("SimpleDateFormat")
    fun millisToDateTime(millis: Long): String {
        val dateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss")
        dateFormat.timeZone = TimeZone.getDefault()
        return dateFormat.format(millis)
    }

    // millisToDate
    @SuppressLint("SimpleDateFormat")
    fun millisToDate(millis: Long): String {
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy")
        dateFormat.timeZone = TimeZone.getDefault()
        return dateFormat.format(millis)
    }

    // millisToTime
    @SuppressLint("SimpleDateFormat")
    fun millisToTime(millis: Long): String {
        val dateFormat = SimpleDateFormat("hh:mm a")
        dateFormat.timeZone = TimeZone.getDefault()
        return dateFormat.format(millis)
    }

    // getVideoDurationMillis
    fun getVideoDurationMillis(filePath: String): Int {
        // This only works for mp4
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            timeString?.toInt() ?: 0
        } catch (ex: Exception) {
            e(ex)
            0
        }
    }

    // videoMillisToDuration
    fun videoMillisToDuration(millis: Int): String {
        if (millis == 0) return "00:00"
        val minute = (millis / 1000 / 60 % 60)
        val second = (millis / 1000 % 60)
        return if (millis >= 3600000) {
            val hour = (millis / 1000 / 3600)
            String.format("%d:%02d:%02d", hour, minute, second)
        } else {
            String.format("%02d:%02d", minute, second)
        }
    }

    // videoMillisToDurations
    fun videoMillisToDurationWords(context: Context, millis: Int): String {
        val applicationContext = context.applicationContext
        val stringBuilder = StringBuilder()
        if (millis >= TIME_HOUR) {
            val hour = millis / TIME_HOUR
            if (hour > 1) {
                stringBuilder.append("$hour ${applicationContext.resources.getString(R.string.time_hours)} ")
            } else {
                stringBuilder.append("$hour ${applicationContext.resources.getString(R.string.time_hour)} ")
            }
        }
        if (millis >= TIME_MINUTE) {
            val minute = millis / TIME_MINUTE % 60
            if (minute > 1) {
                stringBuilder.append("$minute ${applicationContext.resources.getString(R.string.time_minutes)} ")
            } else {
                stringBuilder.append("$minute ${applicationContext.resources.getString(R.string.time_minute)} ")
            }
            val second = millis / TIME_SECOND % 60
            if (second > 1) {
                stringBuilder.append("$second ${applicationContext.resources.getString(R.string.time_seconds)}")
            } else {
                stringBuilder.append("$second ${applicationContext.resources.getString(R.string.time_second)}")
            }
        } else {
            // This clip is less than 1 minutes, show 1 decimal
            val second = millis / TIME_SECOND.toFloat()
            if (second > 1.00f) {
                stringBuilder.append(
                    String.format(
                        "%.1f ${applicationContext.resources.getString(R.string.time_seconds)}",
                        second
                    )
                )
            } else {
                stringBuilder.append(
                    String.format(
                        "%.1f ${applicationContext.resources.getString(R.string.time_second)}",
                        second
                    )
                )
            }
        }
        return stringBuilder.toString()
    }

    // roundDownMillisToDay
    fun roundDownMillisToDay(millis: Long): Long {
        val calender: Calendar = GregorianCalendar()
        calender.timeInMillis = millis
        calender.timeZone = TimeZone.getDefault()
        calender.set(Calendar.MILLISECOND, 0)
        calender.set(Calendar.SECOND, 30)
        calender.set(Calendar.MINUTE, 0)
        calender.set(Calendar.HOUR_OF_DAY, 0)
        return calender.timeInMillis
    }

    // getFileSize
    fun getFileSize(file: File): String {
        val bytes = file.length()
        return when {
            bytes >= 1048576 -> {
                val megaBytes = bytes / 1048576.0
                val size = String.format("%.1f", megaBytes)
                "$size MB"
            }
            bytes >= 1024 -> {
                val kiloBytes = bytes / 1024.0
                val size = String.format("%.1f", kiloBytes)
                "$size kB"
            }
            else -> {
                "$bytes B"
            }
        }
    }

    // insertMapUnique
    fun <T> insertMapUnique(hashMap: HashMap<String, T>, item: T): String {
        var mapKey = System.currentTimeMillis().toString()
        // ensure key is unique
        while (hashMap.containsKey(mapKey)) {
            mapKey += "0"
        }
        hashMap[mapKey] = item
        return mapKey
    }

}