package com.dokidevs.pholder.gallery

import androidx.core.util.forEach
import androidx.fragment.app.FragmentManager
import com.dokidevs.pholder.base.BaseFragment
import com.dokidevs.pholder.base.BaseFragmentStatePagerAdapter
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_LIST_BROWSING_MODE_ALBUM
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_LIST_BROWSING_MODE_FILE_EXPLORER

/*--- GalleryTabAdapter ---*/
class GalleryTabAdapter(private val browsingMode: String, fragmentManager: FragmentManager) :
    BaseFragmentStatePagerAdapter(fragmentManager) {

    /* companion object */
    companion object {

        /* fragment tag */
        const val ALBUM_TAG_TAB_0 = "GalleryTabFragment-AlbumFragment"
        const val ALBUM_TAG_TAB_1 = "GalleryTabFragment-DateFragment"
        const val FILE_EXPLORER_TAG_TAB_0 = "GalleryTabFragment-StarFragment"
        const val FILE_EXPLORER_TAG_TAB_1 = "GalleryTabFragment-FolderFragment"
        const val FILE_EXPLORER_TAG_TAB_2 = "GalleryTabFragment-DateFragment"

        /* fragment class */
        const val ALBUM_CLASS_TAB_0 = AlbumFragment.FRAGMENT_CLASS
        const val ALBUM_CLASS_TAB_1 = DateFragment.FRAGMENT_CLASS
        const val FILE_EXPLORER_CLASS_TAB_0 = StarFragment.FRAGMENT_CLASS
        const val FILE_EXPLORER_CLASS_TAB_1 = FolderFragment.FRAGMENT_CLASS
        const val FILE_EXPLORER_CLASS_TAB_2 = DateFragment.FRAGMENT_CLASS

    }

    // getCount
    override fun getCount(): Int {
        return when (browsingMode) {
            PREF_LIST_BROWSING_MODE_ALBUM -> 2
            PREF_LIST_BROWSING_MODE_FILE_EXPLORER -> 3
            else -> 0
        }
    }

    // getFragmentTag
    override fun getFragmentTag(position: Int): String? {
        return when (browsingMode) {
            PREF_LIST_BROWSING_MODE_ALBUM -> {
                when (position) {
                    0 -> ALBUM_TAG_TAB_0
                    1 -> ALBUM_TAG_TAB_1
                    else -> throw UnsupportedOperationException("Not yet implemented")
                }
            }
            PREF_LIST_BROWSING_MODE_FILE_EXPLORER -> {
                when (position) {
                    0 -> FILE_EXPLORER_TAG_TAB_0
                    1 -> FILE_EXPLORER_TAG_TAB_1
                    2 -> FILE_EXPLORER_TAG_TAB_2
                    else -> throw UnsupportedOperationException("Not yet implemented")
                }
            }
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
    }

    // getItem
    override fun getItem(position: Int): BaseFragment {
        val fragment = registeredFragments[position]
        if (fragment != null) return fragment
        return when (browsingMode) {
            PREF_LIST_BROWSING_MODE_ALBUM -> {
                when (position) {
                    0 -> GalleryTabFragment.newInstance(0, ALBUM_CLASS_TAB_0)
                    1 -> GalleryTabFragment.newInstance(1, ALBUM_CLASS_TAB_1)
                    else -> throw UnsupportedOperationException("Not yet implemented")
                }
            }
            PREF_LIST_BROWSING_MODE_FILE_EXPLORER -> {
                when (position) {
                    0 -> GalleryTabFragment.newInstance(0, FILE_EXPLORER_CLASS_TAB_0)
                    1 -> GalleryTabFragment.newInstance(1, FILE_EXPLORER_CLASS_TAB_1)
                    2 -> GalleryTabFragment.newInstance(2, FILE_EXPLORER_CLASS_TAB_2)
                    else -> throw UnsupportedOperationException("Not yet implemented")
                }
            }
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
    }

    // getItemNewPosition
    override fun getItemNewPosition(fragment: BaseFragment): Int {
        return POSITION_UNCHANGED
    }

    // areFragmentsReady
    fun areFragmentsReady(): Boolean {
        registeredFragments.forEach { _, fragment ->
            val tabFragment = fragment as? GalleryTabFragment
            if (tabFragment != null) {
                if (!tabFragment.isReady) return false
            }
        }
        return true
    }

    // getGalleryBaseFragment
    fun getGalleryBaseFragment(tabPosition: Int): GalleryBaseFragment? {
        val tabFragment = registeredFragments[tabPosition] as? GalleryTabFragment
        return tabFragment?.galleryBaseFragment
    }

    // forEachGalleryBaseFragment
    fun forEachGalleryBaseFragment(action: (position: Int, galleryBaseFragment: GalleryBaseFragment) -> Unit) {
        registeredFragments.forEach { position, fragment ->
            val tabFragment = fragment as? GalleryTabFragment
            if (tabFragment != null) {
                val galleryBaseFragment = tabFragment.galleryBaseFragment
                if (galleryBaseFragment != null) {
                    action(position, galleryBaseFragment)
                }
            }
        }
    }

}