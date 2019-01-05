package com.dokidevs.pholder.slideshow

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dokidevs.pholder.base.BaseFragment

/*--- SlideshowBaseFragment ---*/
abstract class SlideshowBaseFragment : BaseFragment() {

    /* companion object */
    companion object {

        /* intents */
        private const val FILE_PATH = "FILE_PATH"

        // getBaseBundle
        fun getBaseBundle(filePath: String): Bundle {
            val bundle = Bundle()
            bundle.putString(FILE_PATH, filePath)
            return bundle
        }

    }

    /*--- FragmentListener ---*/
    interface FragmentListener {

        // onFragmentReady
        fun onFragmentReady(tag: String, fragment: SlideshowBaseFragment)

        // onMediaTap
        fun onMediaTap(fragment: SlideshowBaseFragment)

        // onImageError
        fun onImageError(fragment: SlideshowImageFragment, isExist: Boolean)

        // onVideoPlay
        fun onVideoPlay(fragment: SlideshowVideoFragment)

        // onVideoPause
        fun onVideoPause(fragment: SlideshowVideoFragment)

        // onVideoFinish
        fun onVideoFinish(fragment: SlideshowVideoFragment)

        // onVideoError
        fun onVideoError(fragment: SlideshowVideoFragment)

        // onVideoHideControlLayout
        fun onVideoHideControlLayout(fragment: SlideshowVideoFragment)

        // enableSwipe
        fun enableSwipe(fragment: SlideshowBaseFragment, enableSwipe: Boolean)

    }

    /* FragmentListener */
    protected var fragmentListener: FragmentListener? = null

    // getFragmentTag
    override fun getFragmentTag(): String {
        return if (!tag.isNullOrEmpty()) {
            tag!!
        } else {
            // Override BaseFragment, such that static class name is returned.
            // Otherwise, proguard will change class name.
            getFragmentClass()
        }
    }

    // getFragmentClass
    abstract fun getFragmentClass(): String

    // onAttachAction
    override fun onAttachAction(context: Context?) {
        if (context is FragmentListener) {
            fragmentListener = context
        } else {
            throw RuntimeException("$context must implement ${getFragmentClass()}.FragmentListener")
        }
    }

    // onViewCreatedAction
    @CallSuper
    override fun onViewCreatedAction(view: View, savedInstanceState: Bundle?) {
        setInsetsListener(view)
        // Request insets on first creation, since listener is not called if no change to activity
        ViewCompat.requestApplyInsets(view)
    }

    // setInsetsListener
    private fun setInsetsListener(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
            insetsUpdated(insets)
            insets
        }
    }

    // insetsUpdated
    abstract fun insetsUpdated(insets: WindowInsetsCompat)

    // getMainView
    abstract fun getMainView(): View

    // getFilePath
    fun getFilePath(): String {
        return arguments?.getString(FILE_PATH) ?: ""
    }

    // prepareEnterTransition
    abstract fun prepareEnterTransition()

    // loadImage
    abstract fun loadImage()

    // onDragStart
    abstract fun onDragStart()

    // onDragIdle
    abstract fun onDragIdle(isCurrentItem: Boolean)

    // onSelect
    abstract fun onSelected()

    // onDeselect
    abstract fun onUnselected()

    // postEnterTransition
    abstract fun postEnterTransition()

    // prepareExitTransition
    abstract fun prepareExitTransition()

    // showControlLayout
    abstract fun showControlLayout(show: Boolean)

    // getTransitionViews
    abstract fun getTransitionViews(): List<View>

    // getControlLayoutViews
    abstract fun getControlLayoutViews(): List<View>

    // onConfigurationChanged
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        setInsetsListener(getMainView())
        // Wait for SlideshowActivity to get the update
    }

    // onDetachAction
    override fun onDetachAction() {
        fragmentListener = null
    }

}