package com.dokidevs.pholder.data

import android.os.Bundle
import androidx.annotation.Keep
import androidx.room.*
import java.io.File
import java.util.*

/*--- FolderTag ---*/
@Keep
@Entity(tableName = "FolderTag", primaryKeys = ["parentPath", "fileName"])
class FolderTag(
    var parentPath: String,
    var fileName: String,
    var lastModified: Long = 0L,
    var thumbnail: String = "",
    var folderCount: Int = 0,
    var fileCount: Int = 0,
    var isUserCreated: Boolean = false,
    var isStarred: Boolean = false
) : PholderTag() {

    /* secondary constructors */
    @Ignore
    constructor(
        file: File
    ) : this(
        "",
        "",
        file.lastModified()
    ) {
        if (file.parentFile != null) {
            parentPath = file.parentFile.absolutePath
        }
        if (file.name != null) {
            fileName = file.name
        }
    }

    /* companion object */
    companion object {

        /* diff bundle */
        const val DIFF_COUNT_FOLDER = "DIFF_COUNT_FOLDER"
        const val DIFF_COUNT_FILE = "DIFF_COUNT_FILE"
        const val DIFF_THUMBNAIL = "DIFF_THUMBNAIL"
        const val DIFF_STAR = "DIFF_STAR"

        // calculateDiff
        fun calculateDiff(oldFolderTag: PholderTag, newFolderTag: PholderTag): Bundle? {
            val diffBundle = Bundle()
            if (oldFolderTag is FolderTag && newFolderTag is FolderTag) {
                if (oldFolderTag.folderCount != newFolderTag.folderCount ||
                    oldFolderTag.fileCount != newFolderTag.fileCount
                ) {
                    diffBundle.putInt(DIFF_COUNT_FOLDER, newFolderTag.folderCount)
                    diffBundle.putInt(DIFF_COUNT_FILE, newFolderTag.fileCount)
                }
                if (oldFolderTag.thumbnail != newFolderTag.thumbnail) {
                    diffBundle.putString(DIFF_THUMBNAIL, newFolderTag.thumbnail)
                }
                if (oldFolderTag.isStarred != newFolderTag.isStarred) {
                    diffBundle.putBoolean(DIFF_STAR, newFolderTag.isStarred)
                }
            }
            return if (diffBundle.isEmpty) {
                null
            } else {
                diffBundle
            }
        }

    }

    // copy
    fun copy(): FolderTag {
        return FolderTag(
            parentPath,
            fileName,
            lastModified,
            thumbnail,
            folderCount,
            fileCount,
            isUserCreated,
            isStarred
        )
    }

    // getType
    override fun getType(): Int {
        return TYPE_FOLDER
    }

    // getUid
    override fun getUid(): String {
        return getFilePath()
    }

    // getFilePath
    fun getFilePath(): String {
        return "$parentPath${File.separator}$fileName"
    }

    // isSamePath
    fun isSamePath(path: String): Boolean {
        return PholderTagUtil.isSamePath(getFilePath(), path)
    }

    // isParentOf
    fun isParentOf(child: String): Boolean {
        return PholderTagUtil.isParentChild(getFilePath(), child)
    }

    // isDirectParentOf
    fun isDirectParentOf(child: String): Boolean {
        return PholderTagUtil.isDirectParentChild(getFilePath(), child)
    }

    // isChildOf
    fun isChildOf(parent: String): Boolean {
        return PholderTagUtil.isParentChild(parent, getFilePath())
    }

    // isDirectChildOf
    fun isDirectChildOf(parent: String): Boolean {
        return PholderTagUtil.isDirectParentChild(parent, getFilePath())
    }

    // toFile
    fun toFile(): File {
        return File(getFilePath())
    }

    // toString
    override fun toString(): String {
        return "FolderTag(" +
                "filePath='${getFilePath()}', " +
                "lastModified='${PholderTagUtil.millisToDateTime(lastModified)}', " +
                "thumbnail='$thumbnail', " +
                "folderCount=$folderCount, " +
                "fileCount=$fileCount, " +
                "isUserCreated=$isUserCreated, " +
                "isStarred=$isStarred, " +
                "isSelected=$isSelected)"
    }

    // equals
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FolderTag
        // Only consider filePath as unique identifier
        if (getFilePath() != other.getFilePath()) return false
        return true
    }

    // hashCode
    override fun hashCode(): Int {
        // Only consider filePath as unique identifier
        return Objects.hash(getFilePath())
    }

}


/*--- FolderTagFilePath ---*/
// Simple class for getting filePath from DAO
class FolderTagFilePath(var parentPath: String, var fileName: String) {

    // getFilePath
    fun getFilePath(): String {
        return "$parentPath${File.separator}$fileName"
    }

}


/*--- FolderTagDao ---*/
@Dao
abstract class FolderTagDao {

    // getAll
    fun getAll(): MutableList<FolderTag> {
        return _getAll()
    }

    // get
    fun get(filePath: String): FolderTag? {
        return _get(PholderTagUtil.getParentPath(filePath), PholderTagUtil.getFileNameWithExtension(filePath))
    }

    @Transaction
    // get
    open fun get(filePaths: List<String>): MutableList<FolderTag> {
        val folderTags = java.util.ArrayList<FolderTag>(filePaths.size)
        filePaths.forEach { filePath ->
            val folderTag = get(filePath)
            if (folderTag != null) {
                folderTags.add(folderTag)
            }
        }
        return folderTags
    }

    // getChildren
    @Transaction
    open fun getChildren(
        parentPath: String,
        includeAll: Boolean = false
    ): MutableList<FolderTag> {
        val folderTags = _getChildren(parentPath)
        if (includeAll) {
            folderTags.addAll(_getChildren(parentPath + File.separator + "%"))
        }
        return folderTags
    }

    // getDistinctFolderPaths
    fun getDistinctFolderPaths(): MutableList<String> {
        return _getDistinctFolderPaths()
    }

    // getUserCreatedFolderPaths
    fun getUserCreatedFolderPaths(): MutableList<String> {
        val folderPaths = java.util.ArrayList<String>()
        val folderTagFilePaths = _getUserCreatedFolderPaths()
        folderPaths.ensureCapacity(folderTagFilePaths.size)
        folderTagFilePaths.mapTo(folderPaths) { folderTagFilePath ->
            folderTagFilePath.getFilePath()
        }
        return folderPaths
    }

    // getStarredFolderPaths
    fun getStarredFolderPaths(): MutableList<String> {
        val folderPaths = java.util.ArrayList<String>()
        val folderTagFilePaths = _getStarredFolderPaths()
        folderPaths.ensureCapacity(folderTagFilePaths.size)
        folderTagFilePaths.mapTo(folderPaths) { folderTagFilePath ->
            folderTagFilePath.getFilePath()
        }
        return folderPaths
    }

    // getStarredFolderTags
    fun getStarredFolderTags(): MutableList<FolderTag> {
        return _getStarredFolderTags()
    }

    // update
    fun update(folderTags: List<FolderTag>): List<Long> {
        return _update(folderTags)
    }

    // delete
    fun delete(folderTags: List<FolderTag>): Int {
        return _delete(folderTags)
    }

    // delete
    fun delete(folderTag: FolderTag): Boolean {
        return _delete(folderTag.parentPath, folderTag.fileName) > 0
    }

    // deleteChildren
    @Transaction
    open fun deleteChildren(parentPath: String): Int {
        var deleteCount = _deleteDirectChildren(parentPath)
        deleteCount += _deleteChildren(parentPath + File.separator + "%")
        return deleteCount
    }

    // deleteAll
    fun deleteAll() {
        _deleteAll()
    }

    @Query("SELECT * FROM FolderTag ORDER BY parentPath ASC, fileName ASC")
    protected abstract fun _getAll(): MutableList<FolderTag>

    @Query("SELECT * FROM FolderTag WHERE parentPath = :parentPath AND fileName = :fileName LIMIT 1")
    protected abstract fun _get(parentPath: String, fileName: String): FolderTag?

    @Query("SELECT DISTINCT parentPath from FileTag")
    protected abstract fun _getDistinctFolderPaths(): MutableList<String>


    @Query("SELECT parentPath, fileName from FolderTag WHERE isUserCreated = 1")
    protected abstract fun _getUserCreatedFolderPaths(): MutableList<FolderTagFilePath>

    @Query("SELECT parentPath, fileName from FolderTag WHERE isStarred = 1")
    protected abstract fun _getStarredFolderPaths(): MutableList<FolderTagFilePath>

    @Query("SELECT * from FolderTag WHERE isStarred = 1")
    protected abstract fun _getStarredFolderTags(): MutableList<FolderTag>

    @Query(
        "SELECT * FROM FolderTag " +
                "WHERE parentPath like :parentPath " +
                "ORDER BY parentPath ASC, fileName ASC"
    )
    protected abstract fun _getChildren(parentPath: String): MutableList<FolderTag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun _update(folderTags: List<FolderTag>): List<Long>

    @Delete
    protected abstract fun _delete(folderTags: List<FolderTag>): Int

    @Query("DELETE FROM FolderTag WHERE parentPath = :parentPath AND fileName = :fileName")
    protected abstract fun _delete(parentPath: String, fileName: String): Int

    @Query("DELETE FROM FolderTag WHERE parentPath = :parentPath")
    protected abstract fun _deleteDirectChildren(parentPath: String): Int

    @Query("DELETE FROM FolderTag WHERE parentPath like :parentPath")
    protected abstract fun _deleteChildren(parentPath: String): Int

    @Query("DELETE FROM FolderTag")
    protected abstract fun _deleteAll()

}