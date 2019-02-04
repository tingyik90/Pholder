package com.dokidevs.pholder.gallery

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.transition.Transition
import android.util.DisplayMetrics
import android.view.View
import androidx.annotation.CallSuper
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import com.dokidevs.dokilog.d
import com.dokidevs.pholder.base.BaseActivity
import com.dokidevs.pholder.base.BaseFragment
import com.dokidevs.pholder.base.BaseSharedElementCallback
import com.dokidevs.pholder.base.BaseTransitionListener
import com.dokidevs.pholder.data.FileTag
import com.dokidevs.pholder.slideshow.SlideshowActivity
import com.dokidevs.pholder.utils.REQUEST_SLIDESHOW_ACTIVITY
import com.dokidevs.pholder.utils.logActivityResult

import java.io.File
import androidx.core.util.Pair as AndroidPair

/*--- GalleryBaseActivity ---*/
// Abstract class to handle transition animation between SlideshowActivity and GalleryActivity
abstract class GalleryBaseActivity :
    BaseActivity(),
    BaseFragment.FragmentOnStartListener,
    GalleryBaseFragment.FragmentListener {

    /* transitions */
    protected var width = 0
    private var isInSlideshowBackTransition = false
    private var isSlideshowStarted = false
    private var toSlideshowFilePath = ""
    private var fromSlideshowFilePath = ""
    private var sharedElementCallback = BaseSharedElementCallback()

    // onCreateAction
    @CallSuper
    override fun onCreateAction(savedInstanceState: Bundle?) {
        width = getScreenWidth()
        window.sharedElementsUseOverlay = false
        // Disable sharedElement overlay. Note that if enabled, the sharedElements are drawn on ViewOverlay and this will cause
        // the animation to be on the top most layer. If disabled, it might cause some problems but it is fine for now.
        // See https://www.androiddesignpatterns.com/2015/01/activity-fragment-shared-element-transitions-in-depth-part3a.html
        // When enabled, the calls are:
        // B.finishAfterTransition() -> A.onReenterFromSlideshowActivity() -> call postponeEnterTransition() and startPostponedEnterTransition() when ready
        // -> A.onTransitionStart() -> B.finish() -> B.onPause -> A.onActivityResult() -> A.onResume() -> B.onStop() -> B.onDestroy() -> A.onTransitionEnd()
        // The ViewOverlay is only removed after onTransitionEnd(), which means ActivityA can only respond after ActivityB is fully destroyed. This makes the
        // app laggy.
        // When disabled, the calls are:
        // B.finishAfterTransition() -> A.onReenterFromSlideshowActivity() -> call postponeEnterTransition() and startPostponedEnterTransition() when ready
        // -> A.onTransitionStart() -> A.onTransitionEnd() (called almost immediately) -> B.finish() -> B.onPause -> A.onActivityResult() -> A.onResume()
        // This means that ActivityA can respond to user faster without waiting for ActivityB to fully destroy, since ViewOverlay does not consume user click,
        // same as usual activity lifecycle.
    }

    // getScreenWidth
    private fun getScreenWidth(): Int {
        // This method ensures that you get real time dimensions. Getting orientation from methods such as
        // "resources.configuration.orientation" or "resources.displayMetrics.widthPixels" is not reliable
        // because they are not yet updated when navigated back from other activity.
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels
    }

    // getCurrentGalleryBaseFragment
    abstract fun getCurrentGalleryBaseFragment(): GalleryBaseFragment?

    // startSlideshowPreparation
    abstract fun startSlideshowPreparation(filePath: String, view: View, fileTags: List<FileTag>)

    // startSlideshow
    override fun startSlideshow(initialFilePath: String, view: View, fileTags: List<FileTag>) {
        // To prevent double click and launching slideshow twice
        if (!isSlideshowStarted) {
            val file = File(initialFilePath)
            if (file.exists()) {
                isSlideshowStarted = true
                toSlideshowFilePath = initialFilePath
                startSlideshowPreparation(initialFilePath, view, fileTags)
                val intent = SlideshowActivity.newIntent(this, initialFilePath, fileTags)
                val pair = mutableListOf<androidx.core.util.Pair<View, String>>()
                if (view is SlideshowActivity.SlideshowTransitionInterface) {
                    view.onExitPreparation()
                    val sharedElements = view.onExitRequiredSharedElements()
                    sharedElements.forEach { sharedElement ->
                        pair.add(androidx.core.util.Pair(sharedElement, sharedElement.transitionName))
                    }
                }
                val statusBar = window.findViewById<View>(android.R.id.statusBarBackground)
                if (statusBar != null) {
                    pair.add(androidx.core.util.Pair(statusBar, statusBar.transitionName))
                }
                // Don't add the navigationBar in GalleryActivity as sharedElements, so that we have a background
                // during transition. Add the navigationBar in SlideshowActivity only as sharedElements, so that it
                // floats on top of this navigationBar. Slowly fadeIn for SlideshowActivity as GalleryActivity will
                // fadeOut due to enterTransition of SlideshowActivity.
                // If we add navigationBar in GalleryActivity as sharedElements, it will cause a flicker in animation
                // as the navigationBar becomes invisible, then visible again when brought up to overLay window.
                // See https://stackoverflow.com/a/26748694/3584439
                val option = ActivityOptionsCompat.makeSceneTransitionAnimation(this, *pair.toTypedArray())
                ActivityCompat.startActivityForResult(this, intent, REQUEST_SLIDESHOW_ACTIVITY, option.toBundle())
            } else {
                slideshowFileNotExist(initialFilePath)
            }
        }
    }

    abstract fun slideshowFileNotExist(filePath: String)

    // onReenterFromSlideshowActivity
    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        fromSlideshowFilePath = data?.getStringExtra(SlideshowActivity.FROM_SLIDESHOW_FILE_PATH) ?: ""
        d("fromSlideshowFilePath = $fromSlideshowFilePath")
        if (fromSlideshowFilePath.isNotEmpty()) {
            isInSlideshowBackTransition = true
            sharedElementCallback = BaseSharedElementCallback()
            setExitSharedElementCallback(sharedElementCallback)
            supportPostponeEnterTransition()
            // Transition only works if the screen orientation does not change in SlideshowActivity
            val oldWidth = width
            width = getScreenWidth()
            if (oldWidth == width) {
                getCurrentGalleryBaseFragment()?.onReenterFromSlideshowActivity(
                    toSlideshowFilePath,
                    fromSlideshowFilePath
                )
            } else {
                // Do nothing and only proceed after onConfigurationChanged has updated recyclerView
            }
        }
        // Details for the whole transition as below:
        // Call startSlideshow() and map sharedElements as option bundle. In SlideshowActivity, call supportPostponeEnterTransition().
        // BaseSharedElementCallback class is made specially so that the sharedElements can be modified anytime before
        // supportStartPostponedEnterTransition() is called (when layout ready), which eventually calls onMapSharedElements().
        // When going back, onBackPressed() will automatically call finishAfterTransition(). This delays finish of SlideshowActivity
        // and call in GalleryActivity onActivityReenter() -> onStart(). As long as you call supportPostponeEnterTransition() in between,
        // onStart() is the last call by the system and will wait until you call supportStartPostponedEnterTransition(). During the process,
        // you can use BaseSharedElementCallback to map sharedElements again.
        // The problem happens when you rotate SlideshowActivity. The call after finishAfterTransition() is in the following orders:
        // onActivityReenter() -> onStart() -> onConfigurationChanged(). This is the usual sequence where onDestroy is called immediately.
        // onConfigurationChanged() is called even if android:configChanges="orientation|screenSize" is not set in AndroidManifest.
        // During that time, the resources "resources.configuration.orientation" or "resources.displayMetrics.widthPixels"
        // is still showing the old orientation of GalleryActivity. You must check the orientation using windowManager instead.
        // onConfigurationChanged() is the last call if supportPostponeEnterTransition() is called in between.
        // However, even if you proceed to map the sharedElements correctly and finish transition, the activity will still be
        // destroyed and recreated, while onActivityReenter() is called again after onStart().
        // As such, to prevent activity being destroyed, android:configChanges="orientation|screenSize" is used and rotation is handled manually.
    }

    // onThumbnailLoadCompleted
    override fun onThumbnailLoadCompleted(fragment: GalleryBaseFragment, view: View, filePath: String) {
        // Start transition if required
        if (isInSlideshowBackTransition) {
            if (filePath == fromSlideshowFilePath) {
                d(filePath)
                view as SlideshowActivity.SlideshowTransitionInterface
                startSlideshowBackTransition(view.onReenterRequiredSharedElements())
            }
        }
    }

    // startSlideshowBackTransition
    private fun startSlideshowBackTransition(fragmentSharedElements: List<View>) {
        isInSlideshowBackTransition = false
        toSlideshowFilePath = ""
        fromSlideshowFilePath = ""
        // Update sharedElements, supplement other sharedElements if required
        window.sharedElementReenterTransition.addListener(object : BaseTransitionListener() {
            override fun onTransitionStart(transition: Transition) {
                // Do nothing
            }

            override fun postRemoveListener(transition: Transition) {
                // Remove callBack once done, or else the next transition will still call it
                ActivityCompat.setExitSharedElementCallback(this@GalleryBaseActivity, null)
                postSlideshowBackTransition()
            }
        })
        val sharedElements = fragmentSharedElements.toMutableList()
        // Add navigationBar
        val navigationBar = findViewById<View>(android.R.id.navigationBarBackground)
        if (navigationBar != null) {
            sharedElements.add(navigationBar)
        }
        sharedElementCallback.setSharedElements(sharedElements)
        supportStartPostponedEnterTransition()
        // The background of SlideshowActivity will fadeOut and this will already provide fadeIn effect for this
        // navigationBar. So, fix the alpha of the navigationBar while fadeOut the navigationBar in SlideshowActivity
        // so they counter each other and retain overall alpha.
        if (navigationBar != null) {
            navigationBar.alpha = 1f
            navigationBar.animate().alpha(1f).setDuration(250).start()
        }
    }

    // cancelSlideshowBackTransition
    override fun cancelSlideshowBackTransition() {
        // Provide empty list and force transition
        startSlideshowBackTransition(listOf())
    }

    // postSlideshowBackTransition
    abstract fun postSlideshowBackTransition()

    // onActivityResult
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        d(logActivityResult(resultCode))
        // Handle local result
        when (requestCode) {
            REQUEST_SLIDESHOW_ACTIVITY -> {
                isSlideshowStarted = false
                // With transition, back navigation will call onActivityReenter -> onStart -> onActivityResult
                // Without transition, typical back navigation will call onActivityResult -> onStart
                // In case finishAfterTransition is not called and called finish directly
                val fromSlideshowFilePath = data?.getStringExtra(SlideshowActivity.FROM_SLIDESHOW_FILE_PATH) ?: ""
                if (fromSlideshowFilePath.isEmpty()) {
                    ActivityCompat.setExitSharedElementCallback(this@GalleryBaseActivity, null)
                    postSlideshowBackTransition()
                }
            }
            else -> {
                onActivityResultAction(requestCode, resultCode, data)
            }
        }
        // Delegate onActivityResult to fragment if required
        super.onActivityResult(requestCode, resultCode, data)
    }

    // onActivityResultAction
    abstract fun onActivityResultAction(requestCode: Int, resultCode: Int, data: Intent?)

    // onConfigurationChanged
    override fun onConfigurationChanged(newConfig: Configuration?) {
        // This is called after onConfigurationChanged is called on fragments.
        // At this time, the fragments should be ready and onReenterFromSlideshowActivity can be called.
        super.onConfigurationChanged(newConfig)
        width = getScreenWidth()
        if (isInSlideshowBackTransition) {
            getCurrentGalleryBaseFragment()?.onReenterFromSlideshowActivity(toSlideshowFilePath, fromSlideshowFilePath)
        }
    }

}