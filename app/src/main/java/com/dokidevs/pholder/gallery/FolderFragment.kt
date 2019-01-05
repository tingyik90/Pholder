package com.dokidevs.pholder.gallery

import com.dokidevs.dokilog.d
import com.dokidevs.pholder.PholderApplication.Companion.prefManager
import com.dokidevs.pholder.R
import com.dokidevs.pholder.data.PholderDatabase
import com.dokidevs.pholder.data.PholderDatabase.Companion.ALL_VIDEOS_FOLDER
import com.dokidevs.pholder.data.PholderDatabase.Companion.PHOLDER_FOLDER
import com.dokidevs.pholder.data.PholderDatabase.Companion.PUBLIC_ROOT
import com.dokidevs.pholder.data.PholderTag
import com.dokidevs.pholder.data.fileTagDao
import com.dokidevs.pholder.data.folderTagDao
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SHOW_EMPTY_FOLDERS
import java.io.File

/*--- FolderFragment ---*/
class FolderFragment : GalleryBaseFragment() {

    /* companion object */
    companion object {

        /* fragment */
        const val FRAGMENT_CLASS = "FolderFragment"

    }

    // getFragmentClass
    override fun getFragmentClass(): String {
        return FRAGMENT_CLASS
    }

    // getDefaultRootFile
    override fun getDefaultRootFile(): File {
        return PUBLIC_ROOT
    }

    // getToolbarTitleName
    override fun getToolbarTitleName(): String {
        return when (rootFile) {
            PUBLIC_ROOT -> {
                getString(R.string.toolbar_title_folderFragment)
            }
            else -> {
                rootFile.name
            }
        }
    }

    // getEmptyViewMessage
    override fun getEmptyViewMessage(): String {
        // Request user to create folder directly below main folders of PUBLIC_ROOT or in Pholder or in ALBUM_ROOT
        return if (rootFile.parentFile == PUBLIC_ROOT ||
            rootFile == PHOLDER_FOLDER
        ) {
            getString(R.string.galleryBaseFragment_emptyView_folder)
        } else {
            getString(R.string.galleryBaseFragment_emptyView_photo)
        }
    }

    // generateItems
    override fun generateItems(): List<PholderTag>? {
        val start = System.currentTimeMillis()
        val items = when {
            rootFile == ALL_VIDEOS_FOLDER -> {
                val allVideoFileTags = fileTagDao.getAllVideos()
                PholderDatabase.buildFolderFileItems(getApplicationContext(), mutableListOf(), allVideoFileTags)
            }
            rootFile.exists() -> {
                val folderTags =
                    PholderDatabase.buildFolderItems(rootFile, prefManager.get(PREF_SHOW_EMPTY_FOLDERS, false))
                // Add 'All Videos' folder at root only
                if (rootFile == PUBLIC_ROOT) {
                    val allVideosFolderTag = folderTagDao.get(ALL_VIDEOS_FOLDER.absolutePath)
                    if (allVideosFolderTag != null && !folderTags.contains(allVideosFolderTag) && allVideosFolderTag.fileCount > 0) {
                        folderTags.add(allVideosFolderTag)
                    }
                }
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

