package com.dokidevs.pholder.data

import androidx.annotation.Keep
import androidx.room.*
import java.io.File
import java.util.*

/*--- FileTag ---*/
@Keep
@Entity(tableName = "FileTag", primaryKeys = ["parentPath", "fileName", "extension"], indices = [(Index("dateTaken"))])
open class FileTag(
    var parentPath: String,
    var fileName: String,
    var extension: String,
    var mediaStoreId: Long,
    var dateTaken: Long = 0,
    var duration: Int = 0,
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var exist: Boolean = true
) : PholderTag() {

    /* secondary constructors */
    @Ignore
    constructor(
        filePath: String,
        mediaStoreId: Long,
        dateTaken: Long = 0,
        duration: Int = 0,
        lat: Double = 0.0,
        lng: Double = 0.0,
        exist: Boolean = true
    ) : this(
        PholderTagUtil.getParentPath(filePath),
        PholderTagUtil.getFileName(filePath),
        PholderTagUtil.getExtension(filePath),
        mediaStoreId,
        dateTaken,
        duration,
        lat,
        lng,
        exist
    )

    @Ignore
    constructor(
        filePath: String,
        mediaStoreId: Long,
        getDateTaken: Boolean = false,
        getDuration: Boolean = false
    ) : this(
        PholderTagUtil.getParentPath(filePath),
        PholderTagUtil.getFileName(filePath),
        PholderTagUtil.getExtension(filePath),
        mediaStoreId
    ) {
        dateTaken = if (getDateTaken) {
            PholderTagUtil.getDateTaken(filePath)
        } else {
            File(filePath).lastModified()
        }
        if (getDuration && isMp4()) {
            duration = PholderTagUtil.getVideoDurationMillis(filePath)
        }
    }

    // copy
    fun copy(): FileTag {
        return FileTag(
            parentPath,
            fileName,
            extension,
            mediaStoreId,
            dateTaken,
            duration,
            lat,
            lng,
            exist
        )
    }

    // getType
    override fun getType(): Int {
        return TYPE_FILE
    }

    // getUid
    override fun getUid(): String {
        return getFilePath()
    }

    // getFilePath
    fun getFilePath(): String {
        return "$parentPath${File.separator}$fileName.$extension"
    }

    fun getFileNameWithExtension(): String {
        return "$fileName.$extension"
    }

    // getMediaStoreUri
    fun getMediaStoreUri(): String {
        return if (isVideo()) {
            "content://media/external/video/media/$mediaStoreId"
        } else {
            "content://media/external/images/media/$mediaStoreId"
        }
    }

    // getGlideLoadPath
    // When using MediaStoreUri, Glide will attempt to use thumbnail from MediaStore.
    // This will significantly speed up video thumbnail if available. Already tested with file path vs
    // MediaStoreUri, it is indeed faster. However, there is a condition that MediaStore already generated the
    // Thumbnail. MediaScannerConnection.scanFile() doesn't generate the thumbnail. As such, there is a need to
    // generate them via MediaStore.Video.Thumbnails.getThumbnail(). This method will block the thread until the
    // thumbnail is returned, so it must not run on main thread.
    // See https://github.com/bumptech/glide/issues/2372 and
    // https://github.com/bumptech/glide/blob/master/library/src/main/java/com/bumptech/glide/load/data/mediastore/ThumbFetcher.java
    fun getGlideLoadPath(): String {
        // Don't use MediaStoreUri got gif, so that they can animate when required.
        return if (mediaStoreId > -1L && !isGif()) {
            getMediaStoreUri()
        } else {
            getFilePath()
        }
    }

    // isImage
    fun isImage(): Boolean {
        return PholderTagUtil.isImage(extension)
    }

    // isJpg
    fun isJpg(): Boolean {
        return PholderTagUtil.isJpg(extension)
    }

    // isPng
    fun isPng(): Boolean {
        return PholderTagUtil.isPng(extension)
    }

    // isBmp
    fun isBmp(): Boolean {
        return PholderTagUtil.isBmp(extension)
    }

    // isGif
    fun isGif(): Boolean {
        return PholderTagUtil.isGif(extension)
    }

    // isGif
    fun isWebP(): Boolean {
        return PholderTagUtil.isWebP(extension)
    }

    // isVideo
    fun isVideo(): Boolean {
        return PholderTagUtil.isVideo(extension)
    }

    // isMp4
    fun isMp4(): Boolean {
        return PholderTagUtil.isMp4(extension)
    }

    // isWebM
    fun isWebM(): Boolean {
        return PholderTagUtil.isWebM(extension)
    }

    // is3gp
    fun is3gp(): Boolean {
        return PholderTagUtil.is3gp(extension)
    }

    // isSamePath
    fun isSamePath(path: String): Boolean {
        return PholderTagUtil.isSamePath(getFilePath(), path)
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

    // checkExist
    fun checkExist(): Boolean {
        exist = toFile().exists()
        return exist
    }

    // toString
    override fun toString(): String {
        return "FileTag(" +
                "filePath='${getFilePath()}', " +
                "mediaStoreId=$mediaStoreId, " +
                "dateTaken='${PholderTagUtil.millisToDateTime(dateTaken)}', " +
                "duration=$duration, " +
                "latLng=($lat,$lng), " +
                "exist=$exist, " +
                "isSelected=$isSelected)"
    }

    // equals
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileTag
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


/*--- FileTagDao ---*/
@Dao
abstract class FileTagDao {

    // getAll
    fun getAll(existOnly: Boolean = true): MutableList<FileTag> {
        return _getAll(existOnly)
    }

    // getChildren
    @Transaction
    open fun getChildren(
        parentPath: String,
        existOnly: Boolean = true,
        includeAll: Boolean = false,
        limit: Int = -1
    ): MutableList<FileTag> {
        val fileTags = _getChildren(parentPath, existOnly, limit).toMutableList()
        if (includeAll) {
            fileTags.addAll(_getChildren(parentPath + File.separator + "%", existOnly, limit))
        }
        return fileTags
    }

    // getLastVideo
    fun getAllVideos(existOnly: Boolean = true): MutableList<FileTag> {
        return _getAllVideos(existOnly)
    }

    // update
    fun update(fileTags: List<FileTag>): List<Long> {
        return _update(fileTags)
    }

    // delete
    fun delete(fileTag: FileTag): Boolean {
        return _delete(fileTag) > 0
    }

    // delete
    fun delete(fileTags: List<FileTag>): Int {
        return _delete(fileTags)
    }

    // delete
    fun delete(filePath: String): Boolean {
        return _delete(
            PholderTagUtil.getParentPath(filePath),
            PholderTagUtil.getFileName(filePath),
            PholderTagUtil.getExtension(filePath)
        ) > 0
    }

    @Transaction
    // deleteChildren
    open fun deleteChildren(parentPath: String, includeAll: Boolean = false): Int {
        var deleteCount = _deleteDirectChildren(parentPath)
        if (includeAll) {
            deleteCount += _deleteChildren(parentPath + File.separator + "%")
        }
        return deleteCount
    }

    // deleteAll
    fun deleteAll() {
        _deleteAll()
    }

    @Query(
        "SELECT * FROM FileTag " +
                "WHERE (exist = :existOnly OR exist = 1) " +
                "ORDER BY dateTaken DESC, parentPath ASC, fileName ASC"
    )
    protected abstract fun _getAll(existOnly: Boolean): MutableList<FileTag>

    @Query(
        "SELECT * FROM FileTag " +
                "WHERE parentPath like :parentPath AND (exist = :existOnly OR exist = 1) " +
                "ORDER BY dateTaken DESC, fileName ASC " +
                "LIMIT :limit"
    )
    protected abstract fun _getChildren(parentPath: String, existOnly: Boolean, limit: Int = -1): List<FileTag>

    @Query(
        "SELECT * FROM FileTag " +
                "WHERE parentPath like :parentPath AND fileName = :fileName AND extension = :extension " +
                "LIMIT 1"
    )
    protected abstract fun _get(parentPath: String, fileName: String, extension: String): FileTag?

    @Query(
        "SELECT * FROM FileTag " +
                "WHERE (extension = 'mp4' OR extension = 'webm' OR extension = '3gp') AND (exist = :existOnly OR exist = 1) " +
                "ORDER BY dateTaken DESC, fileName ASC"
    )
    protected abstract fun _getAllVideos(existOnly: Boolean): MutableList<FileTag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun _update(fileTags: List<FileTag>): List<Long>

    @Delete
    protected abstract fun _delete(fileTag: FileTag): Int

    @Delete
    protected abstract fun _delete(fileTags: List<FileTag>): Int

    @Query("DELETE FROM FileTag WHERE parentPath = :parentPath AND fileName = :fileName AND extension = :extension")
    protected abstract fun _delete(parentPath: String, fileName: String, extension: String): Int

    @Query("DELETE FROM FileTag WHERE parentPath = :parentPath")
    protected abstract fun _deleteDirectChildren(parentPath: String): Int

    @Query("DELETE FROM FileTag WHERE parentPath LIKE :parentPath")
    protected abstract fun _deleteChildren(parentPath: String): Int

    @Query("DELETE FROM FileTag")
    protected abstract fun _deleteAll()

}
