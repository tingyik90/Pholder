package com.dokidevs.pholder.gallery

import com.dokidevs.dokilog.d
import com.dokidevs.pholder.PholderApplication.Companion.prefManager
import com.dokidevs.pholder.R
import com.dokidevs.pholder.data.PholderDatabase
import com.dokidevs.pholder.data.PholderDatabase.Companion.ALL_VIDEOS_FOLDER
import com.dokidevs.pholder.data.PholderDatabase.Companion.PHOLDER_FOLDER
import com.dokidevs.pholder.data.PholderDatabase.Companion.PUBLIC_ROOT
import com.dokidevs.pholder.data.PholderDatabase.Companion.SORT_ORDER_NAME_ASC
import com.dokidevs.pholder.data.PholderTag
import com.dokidevs.pholder.data.fileTagDao
import com.dokidevs.pholder.data.folderTagDao
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SHOW_EMPTY_FOLDERS
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_SORT_ORDER_FOLDER
import java.io.File

/*--- StarFragment ---*/
class StarFragment : GalleryBaseFragment() {

    /* companion object */
    companion object {

        /* tag */
        const val FRAGMENT_CLASS = "StarFragment"

        /* root */
        val STAR_ROOT = File("/StarFragment")

    }

    // getFragmentClass
    override fun getFragmentClass(): String {
        return FRAGMENT_CLASS
    }

    // getDefaultRootFile
    override fun getDefaultRootFile(): File {
        return STAR_ROOT
    }

    // getToolbarTitleName
    override fun getToolbarTitleName(): String {
        return when (rootFile) {
            STAR_ROOT -> {
                getString(R.string.toolbar_title_starFragment)
            }
            else -> {
                rootFile.name
            }
        }
    }

    // getEmptyViewMessage
    override fun getEmptyViewMessage(): String {
        // Request user to create folder directly below main folders of PUBLIC_ROOT or in Pholder
        return when (rootFile) {
            STAR_ROOT -> getString(R.string.galleryBaseFragment_emptyView_star)
            PUBLIC_ROOT, PHOLDER_FOLDER -> getString(R.string.galleryBaseFragment_emptyView_folder)
            else -> getString(R.string.galleryBaseFragment_emptyView_photo)
        }
    }

    // generateItems
    override fun generateItems(): List<PholderTag>? {
        val start = System.currentTimeMillis()
        val items = when {
            rootFile == STAR_ROOT -> {
                val folderTags = folderTagDao.getStarredFolderTags()
                PholderDatabase.sortFolderItems(
                    folderTags,
                    prefManager.get(PREF_SORT_ORDER_FOLDER, SORT_ORDER_NAME_ASC)
                )
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