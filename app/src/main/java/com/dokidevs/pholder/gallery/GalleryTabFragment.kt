package com.dokidevs.pholder.gallery

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.transition.Fade
import androidx.transition.Transition
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseFragment

/*--- GalleryTabFragment ---*/
class GalleryTabFragment : BaseFragment(), BaseFragment.ChildFragmentOnStartListener {

    /* companion object */
    companion object {

        /* intents */
        private const val CHILD_FRAGMENT_CLASS = "CHILD_FRAGMENT_CLASS"
        private const val TAB_POSITION = "TAB_POSITION"

        /* saved instance states */
        private const val SAVED_FRAGMENT_TAGS = "SAVED_FRAGMENT_TAGS"

        // newInstance
        fun newInstance(tabPosition: Int, childFragmentClass: String): GalleryTabFragment {
            val galleryTabFragment = GalleryTabFragment()
            val bundle = Bundle()
            bundle.putInt(TAB_POSITION, tabPosition)
            bundle.putString(CHILD_FRAGMENT_CLASS, childFragmentClass)
            galleryTabFragment.arguments = bundle
            return galleryTabFragment
        }

    }

    /* transitions */
    private var animationDuration = 0L
    private lateinit var fadeIn: Transition
    private lateinit var fadeOut: Transition

    /* parameters */
    var isReady = false
    var galleryBaseFragment: GalleryBaseFragment? = null
        private set
    private val fragmentTags = mutableListOf<String>()

    // getTabPosition
    fun getTabPosition(): Int {
        return arguments?.getInt(TAB_POSITION, -1) ?: -1
    }

    // getChildFragmentClass
    private fun getChildFragmentClass(): String {
        return arguments?.getString(CHILD_FRAGMENT_CLASS) ?: ""
    }

    // onAttachAction
    override fun onAttachAction(context: Context?) {
        animationDuration = resources.getInteger(R.integer.animation_duration_entry_medium).toLong()
        fadeIn = Fade(androidx.transition.Visibility.MODE_IN).setDuration(animationDuration)
        fadeOut = Fade(androidx.transition.Visibility.MODE_OUT).setDuration(animationDuration)
    }

    // childFragmentOnStart
    override fun childFragmentOnStart(tag: String, fragment: BaseFragment) {
        if (fragment is GalleryBaseFragment) {
            galleryBaseFragment = fragment
        }
    }

    // onCreateAction
    override fun onCreateAction(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            val fragmentTagArray = savedInstanceState.getStringArray(SAVED_FRAGMENT_TAGS)
            if (fragmentTagArray != null) {
                fragmentTags.addAll(fragmentTagArray)
            }
        }
    }

    // onCreateViewAction
    override fun onCreateViewAction(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gallery_tab, container, false)
    }

    // onViewCreatedAction
    override fun onViewCreatedAction(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            showNextFragment("", 0f, 0f, 0, 0)
        }
    }

    // addNextFragment
    fun showNextFragment(rootPath: String, originX: Float, originY: Float, originWidth: Int, originHeight: Int) {
        val nextFragment = when (getChildFragmentClass()) {
            AlbumFragment.FRAGMENT_CLASS -> AlbumFragment()
            StarFragment.FRAGMENT_CLASS -> StarFragment()
            FolderFragment.FRAGMENT_CLASS -> FolderFragment()
            DateFragment.FRAGMENT_CLASS -> DateFragment()
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
        nextFragment.setBundle(rootPath, originX, originY, originWidth, originHeight)
        // Prepare transition
        val currentFragment = galleryBaseFragment
        currentFragment?.exitTransition = fadeOut
        nextFragment.enterTransition = fadeIn
        // Begin transaction
        val tag = "${nextFragment.getFragmentClass()}-${nextFragment.getRootPath()}"
        fragmentTags.add(tag)
        childFragmentManager.beginTransaction()
            .setReorderingAllowed(true)  // This is required to enable postponeEnterTransition
            .replace(R.id.galleryTabFragment_container, nextFragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    // onSaveInstanceState
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray(SAVED_FRAGMENT_TAGS, fragmentTags.toTypedArray())
    }

    // onStopAction
    override fun onStopAction() {
        // Do this in onStop or else this will be triggered whenever there is a dialog as onPause is called
        isReady = false
    }

    // onBackPressed
    override fun onBackPressed(): Boolean {
        val fragmentTagsCount = fragmentTags.size
        return if (fragmentTagsCount > 1) {
            val currentFragment =
                childFragmentManager.findFragmentByTag(fragmentTags[fragmentTagsCount - 1]) as GalleryBaseFragment?
            val previousFragment =
                childFragmentManager.findFragmentByTag(fragmentTags[fragmentTagsCount - 2]) as GalleryBaseFragment?
            // Prepare transition
            currentFragment?.returnTransition = fadeOut
            previousFragment?.reenterTransition = fadeIn
            previousFragment?.setOnPopBackStackReadyListener { destinationX, destinationY, destinationWidth, destinationHeight ->
                previousFragment.setOnPopBackStackReadyListener(null)
                currentFragment?.animatePopBackStack(destinationX, destinationY, destinationWidth, destinationHeight)
            }
            fragmentTags.removeAt(fragmentTagsCount - 1)
            childFragmentManager.popBackStack()
            true
        } else {
            false
        }
    }

}