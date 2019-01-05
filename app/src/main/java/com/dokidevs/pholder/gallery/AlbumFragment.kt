package com.dokidevs.pholder.gallery

import com.dokidevs.dokilog.d
import com.dokidevs.pholder.PholderApplication.Companion.prefManager
import com.dokidevs.pholder.R
import com.dokidevs.pholder.data.PholderDatabase
import com.dokidevs.pholder.data.PholderDatabase.Companion.ALL_VIDEOS_FOLDER
import com.dokidevs.pholder.data.PholderTag
import com.dokidevs.pholder.data.fileTagDao
import com.dokidevs.pholder.data.folderTagDao
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_ARRAY_INCLUDED_FOLDER_PATHS
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SHOW_EMPTY_FOLDERS
import java.io.File

/*--- AlbumFragment ---*/
class AlbumFragment : GalleryBaseFragment() {

    /* companion object */
    companion object {

        /* tag */
        const val FRAGMENT_CLASS = "AlbumFragment"

        /* root */
        val ALBUM_ROOT = File("/AlbumFragment")

    }

    // getFragmentClass
    override fun getFragmentClass(): String {
        return FRAGMENT_CLASS
    }

    // getDefaultRootFile
    override fun getDefaultRootFile(): File {
        return ALBUM_ROOT
    }

    // getEmptyViewMessage
    override fun getEmptyViewMessage(): String {
        // Request user to create folder directly in ALBUM_ROOT
        return if (rootFile == ALBUM_ROOT) {
            getString(R.string.galleryBaseFragment_emptyView_folder)
        } else {
            getString(R.string.galleryBaseFragment_emptyView_photo)
        }
    }

    // getToolbarTitleName
    override fun getToolbarTitleName(): String {
        return when (rootFile) {
            ALBUM_ROOT -> {
                getString(R.string.toolbar_title_albumFragment)
            }
            else -> {
                rootFile.name
            }
        }
    }

    // generateItems
    override fun generateItems(): List<PholderTag>? {
        val start = System.currentTimeMillis()
        val items = when {
            rootFile == ALBUM_ROOT -> {
                // Use hashSet to ensure uniqueness
                val folderPathSet = HashSet<String>()
                // Get all folders with media, user created folders, included folder paths, starred folders
                folderPathSet.addAll(folderTagDao.getDistinctFolderPaths())
                folderPathSet.addAll(folderTagDao.getUserCreatedFolderPaths())
                folderPathSet.addAll(folderTagDao.getStarredFolderPaths())
                folderPathSet.addAll(prefManager.getStringArray(PREF_ARRAY_INCLUDED_FOLDER_PATHS))
                // The db already removed excluded file paths, so the returned list is correct regardless of above paths
                val folderTags = folderTagDao.get(folderPathSet.toList())
                // Add 'All Videos' folder if does not exist
                val allVideosFolderTag = folderTagDao.get(ALL_VIDEOS_FOLDER.absolutePath)
                if (allVideosFolderTag != null && !folderTags.contains(allVideosFolderTag) && allVideosFolderTag.fileCount > 0) {
                    folderTags.add(allVideosFolderTag)
                }
                PholderDatabase.buildFolderFileItems(getApplicationContext(), folderTags, mutableListOf())
            }
            rootFile == ALL_VIDEOS_FOLDER -> {
                val allVideoFileTags = fileTagDao.getAllVideos()
                PholderDatabase.buildFolderFileItems(getApplicationContext(), mutableListOf(), allVideoFileTags)
            }
            rootFile.exists() -> {
                val folderTags =
                    PholderDatabase.buildFolderItems(rootFile, prefManager.get(PREF_SHOW_EMPTY_FOLDERS, false))
                val fileTags = fileTagDao.getChildren(rootFile.absolutePath)
                PholderDatabase.buildFolderFileItems(getApplicationContext(), folderTags, fileTags)
            }
            else -> {
                return null
            }
        }
        d("${getFragmentTag()} - duration = ${System.currentTimeMillis() - start}, size = ${items.size}")
        return items
    }

}