package com.dokidevs.pholder.slideshow

import androidx.core.util.forEach
import androidx.fragment.app.FragmentManager
import com.dokidevs.pholder.base.BaseFragment
import com.dokidevs.pholder.base.BaseFragmentStatePagerAdapter
import com.dokidevs.pholder.data.FileTag
import com.dokidevs.pholder.data.PholderTagUtil

/*--- SlideshowAdapter ---*/
class SlideshowAdapter(
    fragmentManager: FragmentManager,
    private val fileTags: MutableList<FileTag>
) : BaseFragmentStatePagerAdapter(fragmentManager) {

    // getCount
    override fun getCount(): Int {
        return fileTags.size
    }

    // getItem
    override fun getItem(position: Int): BaseFragment {
        val fileTag = fileTags[position]
        return if (fileTag.isVideo()) {
            SlideshowVideoFragment.newInstance(fileTag.getFilePath(), fileTag.duration)
        } else {
            SlideshowImageFragment.newInstance(fileTag.getFilePath())
        }
    }

    // getFragmentTag
    override fun getFragmentTag(position: Int): String? {
        return fileTags[position].getFilePath()
    }

    // getItemNewPosition
    override fun getItemNewPosition(fragment: BaseFragment): Int {
        fragment as SlideshowBaseFragment
        val filePath = fragment.getFilePath()
        val position = PholderTagUtil.getPholderTagPosition(fileTags, filePath)
        return if (position < 0) {
            POSITION_NONE
        } else {
            position
        }
    }

    // getAllFileTags
    fun getAllFileTags(): List<FileTag> {
        return fileTags
    }

    // getFileTag
    fun getFileTag(position: Int): FileTag? {
        return if (fileTags.size > position) {
            fileTags[position]
        } else {
            null
        }
    }

    // removeFileTag
    fun removeFileTag(filePath: String): List<FileTag> {
        val position = PholderTagUtil.getPholderTagPosition(fileTags, filePath)
        if (position >= 0) {
            fileTags.removeAt(position)
            notifyDataSetChanged()
        }
        return fileTags
    }

    // getSlideshowBaseFragment
    fun getSlideshowBaseFragment(position: Int): SlideshowBaseFragment? {
        return registeredFragments[position] as? SlideshowBaseFragment
    }

    // forEachSlideshowBaseFragment
    fun forEachSlideshowBaseFragment(action: (position: Int, slideshowBaseFragment: SlideshowBaseFragment) -> Unit) {
        registeredFragments.forEach { position, fragment ->
            val slideshowBaseFragment = fragment as? SlideshowBaseFragment
            if (slideshowBaseFragment != null) {
                action(position, slideshowBaseFragment)
            }
        }
    }

}