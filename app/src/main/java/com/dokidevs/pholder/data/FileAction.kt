package com.dokidevs.pholder.data

import androidx.annotation.Keep
import androidx.room.*
import java.util.*

/*--- FileAction ---*/
@Keep
@Entity(tableName = "FileAction", primaryKeys = ["parentPath", "fileName", "extension"], indices = [(Index("time"))])
class FileAction(
    parentPath: String,
    fileName: String,
    extension: String,
    dateTaken: Long = 0,
    duration: Int = 0,
    lat: Double = 0.0,
    lng: Double = 0.0,
    exist: Boolean = true,
    var action: Int = ACTION_NONE,
    var time: Long = 0
) : FileTag(
    parentPath,
    fileName,
    extension,
    -1L, // mediaStoreId is always invalid for fileAction
    dateTaken,
    duration,
    lat,
    lng,
    exist
) {

    /* secondary constructors */
    @Ignore
    constructor(
        fileTag: FileTag,
        action: Int = 0,
        time: Long = 0
    ) : this(
        fileTag.parentPath,
        fileTag.fileName,
        fileTag.extension,
        fileTag.dateTaken,
        fileTag.duration,
        fileTag.lat,
        fileTag.lng,
        fileTag.exist,
        action,
        time
    )

    /* companion object */
    companion object {

        /* actions */
        const val ACTION_NONE = 0
        const val ACTION_ADD = 1
        const val ACTION_DELETE = 2

    }

    // toFileTag
    fun toFileTag(): FileTag {
        return FileTag(parentPath, fileName, extension, -1L, dateTaken, duration, lat, lng, exist)
    }

    // getActionString
    private fun getActionString(): String {
        return when (action) {
            ACTION_NONE -> "ACTION_NONE"
            ACTION_ADD -> "ACTION_ADD"
            ACTION_DELETE -> "ACTION_DELETE"
            else -> "NOT IMPLEMENTED"
        }
    }

    // toString
    override fun toString(): String {
        return "FileAction(" +
                "filePath='${getFilePath()}', " +
                "mediaStoreId=$mediaStoreId, " +
                "dateTaken='${PholderTagUtil.millisToDateTime(dateTaken)}', " +
                "duration=$duration, " +
                "latLng=($lat,$lng), " +
                "exist=$exist, " +
                "action='${getActionString()}', " +
                "time=$time)"
    }

    // equals
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileAction
        // Only consider filePath and action as unique identifier
        if (getFilePath() != other.getFilePath()) return false
        if (action != other.action) return false
        return true
    }

    // hashCode
    override fun hashCode(): Int {
        // Only consider filePath and action as unique identifier
        return Objects.hash(getFilePath(), action)
    }

}


/*--- FileActionDao ---*/
@Dao
abstract class FileActionDao {

    // getAll
    fun getAll(): List<FileAction> {
        return _getAll()
    }

    // update
    fun update(fileActions: List<FileAction>): List<Long> {
        return _update(fileActions)
    }

    // delete
    fun delete(fileActions: List<FileAction>): Int {
        return _delete(fileActions)
    }

    // delete
    fun delete(fileAction: FileAction): Boolean {
        return _delete(fileAction) > 0
    }

    @Query("SELECT * FROM FileAction ORDER BY time ASC")
    protected abstract fun _getAll(): List<FileAction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun _update(fileActions: List<FileAction>): List<Long>

    @Delete
    protected abstract fun _delete(fileActions: List<FileAction>): Int

    @Delete
    protected abstract fun _delete(fileAction: FileAction): Int

}
