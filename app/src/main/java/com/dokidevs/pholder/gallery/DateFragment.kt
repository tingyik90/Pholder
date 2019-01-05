package com.dokidevs.pholder.gallery

import com.dokidevs.dokilog.d
import com.dokidevs.pholder.R
import com.dokidevs.pholder.data.PholderDatabase
import com.dokidevs.pholder.data.PholderTag
import com.dokidevs.pholder.data.fileTagDao
import java.io.File

/*--- DateFragment ---*/
class DateFragment : GalleryBaseFragment() {

    /* companion object */
    companion object {

        /* tag */
        const val FRAGMENT_CLASS = "DateFragment"

        /* root */
        val DATE_ROOT = File("/DateFragment")

    }

    // getFragmentClass
    override fun getFragmentClass(): String {
        return FRAGMENT_CLASS
    }

    // getDefaultRootFile
    override fun getDefaultRootFile(): File {
        return DATE_ROOT
    }

    // getToolbarTitleName
    override fun getToolbarTitleName(): String {
        return getString(R.string.toolbar_title_dateFragment)
    }

    // getEmptyViewMessage
    override fun getEmptyViewMessage(): String {
        return getString(R.string.galleryBaseFragment_emptyView_photo)
    }

    // generateItems
    override fun generateItems(): List<PholderTag>? {
        val start = System.currentTimeMillis()
        val items = fileTagDao.getAll()
        d("${getFragmentTag()} - duration = ${System.currentTimeMillis() - start}, size = ${items.size}")
        return PholderDatabase.buildFolderFileItems(getApplicationContext(), mutableListOf(), items)
    }

}