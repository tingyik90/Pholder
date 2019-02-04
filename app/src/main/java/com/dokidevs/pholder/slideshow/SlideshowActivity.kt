package com.dokidevs.pholder.slideshow

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.viewpager.widget.ViewPager
import com.dokidevs.dokilog.d
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.*
import com.dokidevs.pholder.data.FileTag
import com.dokidevs.pholder.data.PholderTagUtil
import com.dokidevs.pholder.dialog.ConfirmationDialog
import com.dokidevs.pholder.info.InfoActivity
import com.dokidevs.pholder.service.FileIntentService
import com.dokidevs.pholder.utils.*
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import kotlinx.android.synthetic.main.activity_slideshow.*


/*--- SlideshowActivity ---*/
class SlideshowActivity :
    BaseActivity(),
    BaseFragment.FragmentOnStartListener,
    SlideshowBaseFragment.FragmentListener,
    BaseDialogFragment.DialogListener {

    /* companion object */
    companion object {

        /* saved instance states */
        private const val SAVED_CURRENT_FILE_PATH = "SAVED_CURRENT_FILE_PATH"
        private const val SAVED_SHOW_SYSTEM_UI = "SAVED_SHOW_SYSTEM_UI"

        /* intents */
        private const val INITIAL_FILE_PATH = "INITIAL_FILE_PATH"
        private const val MAP_KEY = "MAP_KEY"

        /* results */
        const val FROM_SLIDESHOW_FILE_PATH = "FROM_SLIDESHOW_FILE_PATH"

        /* map */
        private val fileTagMap = HashMap<String, List<FileTag>>()

        // newIntent
        fun newIntent(context: Context, initialFilePath: String, fileTags: List<FileTag>): Intent {
            val intent = Intent(context, SlideshowActivity::class.java)
            intent.putExtra(INITIAL_FILE_PATH, initialFilePath)
            val mapKey = PholderTagUtil.insertMapUnique(fileTagMap, fileTags)
            intent.putExtra(MAP_KEY, mapKey)
            return intent
        }

    }

    /*--- SlideshowTransitionInterface ---*/
    interface SlideshowTransitionInterface {

        // onExitPreparation
        fun onExitPreparation()

        // onExitRequiredSharedElements
        fun onExitRequiredSharedElements(): List<View>

        // onReenterPreparation
        fun onReenterPreparation()

        // onReenterRequiredSharedElements
        fun onReenterRequiredSharedElements(): List<View>

        // isThumbnailLoadComplete
        fun isThumbnailLoadComplete(): Boolean

    }

    /* views */
    private val main by lazy { slideshowActivity_main }
    private val lockableViewPager by lazy { slideshowActivity_lockableViewPager }
    private val actionLayout by lazy { slideshowActivity_actionLayout }
    private val shareButton by lazy { slideshowActivity_action_share }
    private val infoButton by lazy { slideshowActivity_action_info }
    private val deleteButton by lazy { slideshowActivity_action_delete }
    private val slideshowAdapter by lazy { SlideshowAdapter(supportFragmentManager) }

    /* transitions */
    private val sharedElementCallback = BaseSharedElementCallback()
    private val entryAnimationDuration by lazy {
        resources.getInteger(R.integer.animation_duration_entry_medium).toLong()
    }
    private val exitAnimationDuration by lazy {
        resources.getInteger(R.integer.animation_duration_exit_medium).toLong()
    }
    private var initialFilePath = ""
    private var isInActivityTransition = false

    /* parameters */
    private val deletedPaths = mutableListOf<String>()
    private var currentItem = 0
    private var resultIntent = Intent()
    private var showSystemUI = true
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var snackProgressBarManager: SnackProgressBarManager

    // onCreateAction
    override fun onCreateAction(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_slideshow)
        val fileTags = fileTagMap[getMapKey()]
        if (fileTags != null && fileTags.isNotEmpty()) {
            prepareEnterTransition()
            val currentFilePath: String
            if (savedInstanceState != null) {
                currentFilePath = savedInstanceState.getString(SAVED_CURRENT_FILE_PATH, "")
                initialFilePath = currentFilePath
                showSystemUI = savedInstanceState.getBoolean(SAVED_SHOW_SYSTEM_UI, showSystemUI)
            } else {
                initialFilePath = intent.getStringExtra(INITIAL_FILE_PATH) ?: ""
                currentFilePath = initialFilePath
                isInActivityTransition = true
                supportPostponeEnterTransition()
            }
            setSnackProgressBarManager()
            setAdapter(fileTags)
            setViewPager(fileTags, currentFilePath)
            setActionLayout()
            showSystemUI(showSystemUI)
            setShareButton()
            setInfoButton()
            setDeleteButton()
            setBroadcastReceiver()
        } else {
            // The activity may have resumed from long idle state and the fileTagMap was GCed, just return to GalleryActivity
            finish()
        }
    }

    // getMapKey
    private fun getMapKey(): String {
        return intent.getStringExtra(MAP_KEY) ?: ""
    }

    // prepareEnterTransition
    private fun prepareEnterTransition() {
        val fade = Fade()
        fade.duration = entryAnimationDuration
        window.enterTransition = fade
        // transitionBackgroundFade happens concurrent to all background transition, set duration to be same as entry transition
        window.transitionBackgroundFadeDuration = entryAnimationDuration
        val sharedElementEnterTransition =
            TransitionInflater.from(this).inflateTransition(R.transition.transition_image_transform)
        sharedElementEnterTransition.duration = entryAnimationDuration
        window.sharedElementEnterTransition = sharedElementEnterTransition
        val sharedElementReturnTransition =
            TransitionInflater.from(this).inflateTransition(R.transition.transition_image_transform)
        sharedElementReturnTransition.duration = exitAnimationDuration
        window.sharedElementReturnTransition = sharedElementReturnTransition
        setEnterSharedElementCallback(sharedElementCallback)
    }

    // setSnackProgressBarManager
    private fun setSnackProgressBarManager() {
        snackProgressBarManager = SnackProgressBarManager(main)
    }

    // setViewsToMove
    private fun setViewsToMove(fragment: SlideshowBaseFragment) {
        if (fragment is SlideshowVideoFragment) {
            snackProgressBarManager.setViewsToMove(arrayOf(actionLayout, fragment.getSeekBarLayout()))
        } else {
            snackProgressBarManager.setViewToMove(actionLayout)
        }
    }

    // setAdapter
    private fun setAdapter(fileTags: List<FileTag>) {
        slideshowAdapter.setFileTags(fileTags)
    }

    // setViewPager
    private fun setViewPager(fileTags: List<FileTag>, currentFilePath: String) {
        // Add more offscreen as buffer
        lockableViewPager.offscreenPageLimit = 2
        lockableViewPager.adapter = slideshowAdapter
        lockableViewPager.pageMargin = resources.getDimension(R.dimen.slideshow_margin_page).toInt()
        // Initiate file position
        resultIntent.putExtra(FROM_SLIDESHOW_FILE_PATH, currentFilePath)
        currentItem = PholderTagUtil.getPholderTagPosition(fileTags, currentFilePath)
        if (currentItem < 0) currentItem = 0
        lockableViewPager.currentItem = currentItem
        lockableViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                // Once scrolling started
                if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                    getCurrentSlideshowBaseFragment()?.onDragStart()
                }
                // Once scrolling settled
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    slideshowAdapter.forEachSlideshowBaseFragment { position, slideshowBaseFragment ->
                        slideshowBaseFragment.onDragIdle(position == currentItem)
                    }
                    // Delete image which is started due to setCurrentItem
                    if (deletedPaths.isNotEmpty()) {
                        deletedPaths.forEach { deletedPath ->
                            removeFileTag(deletedPath)
                            snackProgressBarManager.shortSnackBar(
                                this@SlideshowActivity,
                                preResId = R.string.toast_deleteFile_ok_pre,
                                message = PholderTagUtil.getFileNameWithExtension(deletedPath),
                                postResId = R.string.toast_deleteFile_ok_post
                            )
                        }
                        // Update the map, so that the fileTags are updated
                        fileTagMap[getMapKey()] = slideshowAdapter.getAllFileTags()
                    }
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                // Do nothing
            }

            override fun onPageSelected(position: Int) {
                val previousItem = currentItem
                currentItem = lockableViewPager.currentItem
                if (previousItem != currentItem) {
                    slideshowAdapter.getSlideshowBaseFragment(previousItem)?.onUnselected()
                    val currentFragment = getCurrentSlideshowBaseFragment()
                    if (currentFragment != null) {
                        currentFragment.onSelected()
                        setViewsToMove(currentFragment)
                        resultIntent.putExtra(FROM_SLIDESHOW_FILE_PATH, currentFragment.getFilePath())
                    }
                }
            }
        })
    }

    // setActionLayout
    private fun setActionLayout() {
        ViewCompat.setOnApplyWindowInsetsListener(actionLayout) { view, insets ->
            ViewCompat.setOnApplyWindowInsetsListener(actionLayout, null)
            view.setMargins(
                right = insets.systemWindowInsetRight,
                bottom = insets.systemWindowInsetBottom
            )
            insets
        }
    }

    // setShareButton
    private fun setShareButton() {
        shareButton.setOnClickListener {
            pauseCurrentVideo()
            val fileTag = slideshowAdapter.getFileTag(lockableViewPager.currentItem)
            if (fileTag != null && fileTag.checkExist()) {
                FileIntentService.shareFiles(this@SlideshowActivity, listOf(fileTag))
            } else {
                snackProgressBarManager.shortSnackBar(this, R.string.toast_file_does_not_exist)
            }
        }
    }

    // setInfoButton
    private fun setInfoButton() {
        infoButton.setOnClickListener {
            pauseCurrentVideo()
            val fileTag = slideshowAdapter.getFileTag(lockableViewPager.currentItem)
            if (fileTag != null && fileTag.checkExist()) {
                val intent = InfoActivity.newIntent(this, fileTag)
                startActivity(intent)
            } else {
                snackProgressBarManager.shortSnackBar(this, R.string.toast_file_does_not_exist)
            }
        }
    }

    // setDeleteButton
    private fun setDeleteButton() {
        deleteButton.setOnClickListener {
            pauseCurrentVideo()
            ConfirmationDialog.newInstance(ConfirmationDialog.DIALOG_DELETE).show(supportFragmentManager)
        }
    }

    // setBroadcastReceiver
    @Suppress("UNCHECKED_CAST")
    private fun setBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                when (action) {
                    FileIntentService.ACTION_FILES_DELETED -> {
                        // There is only one delete per time
                        val resultPairs =
                            intent.getSerializableExtra(FileIntentService.FILES_ACTION_RESULT_PAIR_ARRAY) as Array<Pair<String, Int>>
                        val deletedPath = resultPairs[0].first
                        val isDeleted = resultPairs[0].second == FileIntentService.ACTION_STATUS_OK
                        fileDeleted(deletedPath, isDeleted)
                    }
                }
            }
        }
    }

    // onStartAction
    override fun onStartAction() {
        registerLocalBroadcastReceiver(broadcastReceiver, FileIntentService.ACTION_FILES_DELETED)
    }

    // fragmentOnStart
    override fun fragmentOnStart(tag: String, fragment: BaseFragment) {
        fragment as SlideshowBaseFragment
        if (fragment.getFilePath() == initialFilePath) {
            if (isInActivityTransition) {
                // Don't consume the initialFilePath event, do it in onFragmentReady
                fragment.prepareEnterTransition()
                // But don't wait too long until image is loaded, just proceed to transition after some delay.
                // Loading the video to play is usually very fast, it is the image loading that is slow.
                Handler().postDelayed({
                    onFragmentReady(tag, fragment)
                }, 500L)
            } else {
                // This happens when activity is recreated, immediately play video or show image. Consume the event.
                initialFilePath = ""
                if (fragment is SlideshowVideoFragment) {
                    fragment.loadVideo()
                } else {
                    fragment.loadImage()
                }
            }
        } else {
            // For other fragments, just proceed
            if (fragment is SlideshowVideoFragment) {
                fragment.loadImage()
                fragment.loadVideo()
            } else {
                fragment.loadImage()
            }
        }
        fragment.showControlLayout(showSystemUI)
    }

    // onFragmentReady
    override fun onFragmentReady(tag: String, fragment: SlideshowBaseFragment) {
        if (isInActivityTransition) {
            if (fragment.getFilePath() == initialFilePath) {
                // Consume the initialFilePath event
                initialFilePath = ""
                isInActivityTransition = false
                startEnterTransition(fragment)
                setViewsToMove(fragment)
            }
        }
    }

    // getCurrentSlideshowBaseFragment
    private fun getCurrentSlideshowBaseFragment(): SlideshowBaseFragment? {
        return slideshowAdapter.getSlideshowBaseFragment(lockableViewPager.currentItem)
    }

    // pauseCurrentVideo
    private fun pauseCurrentVideo() {
        (getCurrentSlideshowBaseFragment() as? SlideshowVideoFragment)?.pause()
    }

    // startEnterTransition
    private fun startEnterTransition(fragment: SlideshowBaseFragment) {
        // Update sharedElements, supplement other sharedElements if required
        val sharedElements = mutableListOf<View>()
        val images = fragment.getTransitionViews()
        sharedElements.addAll(images)
        val controlLayoutViews = fragment.getControlLayoutViews().toMutableList()
        controlLayoutViews.add(shareButton)
        controlLayoutViews.add(infoButton)
        controlLayoutViews.add(deleteButton)
        sharedElements.addAll(controlLayoutViews)
        // Replace color in window registry to full black first to match GalleryActivity
        val statusBar = findViewById<View>(android.R.id.statusBarBackground)
        if (statusBar != null) {
            sharedElements.add(statusBar)
            window.statusBarColor = ColorUtils.statusBarDefault
        }
        val navigationBar = findViewById<View>(android.R.id.navigationBarBackground)
        if (navigationBar != null) {
            sharedElements.add(navigationBar)
        }
        sharedElementCallback.setSharedElements(sharedElements)
        // sharedElementEnterTransition and sharedElementReturnTransition share the same listener.
        // This is true for sharedElementExitTransition and sharedElementReenterTransition.
        // Adding either will receive the same callback.
        // Add listener for fade in actionLayout
        window.sharedElementEnterTransition.addListener(object : BaseTransitionListener() {
            override fun onTransitionStart(transition: Transition) {
                controlLayoutViews.forEach { view ->
                    view.alpha = 0f
                    view.animate().alpha(1f).setDuration(entryAnimationDuration).start()
                }
                // Animate statusBar to match translucent color.
                if (statusBar != null) {
                    statusBar.alpha = 1f
                    statusBar.animate().alpha(0.5f).setDuration(entryAnimationDuration).start()
                }
                // Animate navigationBar to fadeIn so that it won't overlap the navigationBar in GalleryActivity and
                // causes the alpha to double (looks darker) temporarily until the GalleryActivity is completely
                // not visible.
                if (navigationBar != null) {
                    navigationBar.alpha = 0f
                    navigationBar.animate().alpha(1f).setDuration(entryAnimationDuration).start()
                }
            }

            override fun postRemoveListener(transition: Transition) {
                // Replace the color in window registry here, or else if user show/hide systemUI, the color will revert
                // back to full black.
                if (statusBar != null) {
                    window.statusBarColor = ColorUtils.statusBarTranslucent
                    statusBar.alpha = 1f
                }
                getCurrentSlideshowBaseFragment()?.postEnterTransition()
            }
        })
        supportStartPostponedEnterTransition()
    }

    // finishAfterTransitionAction
    override fun finishAfterTransitionAction() {
        // Show systemUI
        showSystemUI(true)
        // finishAfterTransition() is called after onBackPressed, setResult in onPause is not working
        // because the result is finalised in finish(), so call setResult here
        setResult(Activity.RESULT_OK, resultIntent)
        slideshowAdapter.forEachSlideshowBaseFragment { _, slideshowBaseFragment ->
            slideshowBaseFragment.prepareExitTransition()
        }
        // Prepare window transition
        val fade = Fade()
        fade.duration = exitAnimationDuration
        window.returnTransition = fade
        // Immediately expose GalleryActivity when transition back
        window.transitionBackgroundFadeDuration = exitAnimationDuration
        // Update sharedElements, supplement other sharedElements if required
        val fragment = getCurrentSlideshowBaseFragment()
        if (fragment != null) {
            val sharedElements = mutableListOf<View>()
            val images = fragment.getTransitionViews()
            sharedElements.addAll(images)
            val controlLayoutViews = fragment.getControlLayoutViews().toMutableList()
            controlLayoutViews.add(shareButton)
            controlLayoutViews.add(infoButton)
            controlLayoutViews.add(deleteButton)
            sharedElements.addAll(controlLayoutViews)
            // Replace color in window registry to full black first to match GalleryActivity, set alpha to 50%,
            // so user does not notice any change.
            val statusBar = window.findViewById<View>(android.R.id.statusBarBackground)
            if (statusBar != null) {
                sharedElements.add(statusBar)
                window.statusBarColor = ColorUtils.statusBarDefault
                statusBar.alpha = 0.5f
            }
            val navigationBar = window.findViewById<View>(android.R.id.navigationBarBackground)
            if (navigationBar != null) {
                sharedElements.add(navigationBar)
            }
            sharedElementCallback.setSharedElements(sharedElements)
            // Add listener for fade out actionLayout
            window.sharedElementReturnTransition.addListener(object : BaseTransitionListener() {
                override fun onTransitionStart(transition: Transition) {
                    controlLayoutViews.forEach { view ->
                        view.alpha = 1f
                        view.animate().alpha(0f).setDuration(exitAnimationDuration).start()
                    }
                    // Animate to full black
                    statusBar?.animate()?.alpha(1f)?.setDuration(exitAnimationDuration)?.start()
                    // FadeOut this navigationBar so that the overall alpha remains the same as navigationBar in
                    // GalleryActivity fadeIn.
                    navigationBar?.animate()?.alpha(0f)?.setDuration(exitAnimationDuration)?.start()
                }

                override fun postRemoveListener(transition: Transition) {
                    // Do nothing
                }
            })
        }
    }

    // onMediaTap
    override fun onMediaTap(fragment: SlideshowBaseFragment) {
        // Toggle UI
        showSystemUI(!showSystemUI)
    }

    // onImageError
    override fun onImageError(fragment: SlideshowImageFragment, isExist: Boolean) {
        if (isExist) {
            snackProgressBarManager.shortSnackBar(this, R.string.toast_imageFragment_load_failed)
        } else {
            snackProgressBarManager.shortSnackBar(this, R.string.toast_startSlideshow_file_not_exist)
        }
    }

    // onVideoPlay
    override fun onVideoPlay(fragment: SlideshowVideoFragment) {
        keepScreenOn(true)
    }

    // onVideoPause
    override fun onVideoPause(fragment: SlideshowVideoFragment) {
        keepScreenOn(false)
    }

    // onVideoFinish
    override fun onVideoFinish(fragment: SlideshowVideoFragment) {
        keepScreenOn(false)
        showSystemUI(true)
    }

    // onVideoError
    override fun onVideoError(fragment: SlideshowVideoFragment) {
        if (fragment == getCurrentSlideshowBaseFragment()) {
            snackProgressBarManager.shortSnackBar(this, R.string.toast_videoFragment_play_failed)
        }
    }

    // onVideoHideControlLayout
    override fun onVideoHideControlLayout(fragment: SlideshowVideoFragment) {
        showSystemUI(false)
    }

    // keepScreenOn
    private fun keepScreenOn(keep: Boolean) {
        d("keep = $keep")
        if (keep) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // showSystemUI
    private fun showSystemUI(show: Boolean) {
        showSystemUI = show
        var visibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        if (show) {
            actionLayout.isVisible = true
        } else {
            actionLayout.isVisible = false
            visibility = (visibility or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
        window.decorView.systemUiVisibility = visibility
        slideshowAdapter.forEachSlideshowBaseFragment { _, slideshowBaseFragment ->
            slideshowBaseFragment.showControlLayout(show)
        }
    }

    // enableSwipe
    override fun enableSwipe(fragment: SlideshowBaseFragment, enableSwipe: Boolean) {
        lockableViewPager.canSwipe = enableSwipe
    }

    // onDialogAction
    override fun onDialogAction(action: Int, dialogFragment: BaseDialogFragment, data: Bundle?) {
        when (dialogFragment.dialogType) {
            ConfirmationDialog.DIALOG_DELETE -> {
                if (action == BaseDialogFragment.CLICK_POSITIVE) {
                    deleteFile()
                }
            }
        }
    }

    // deleteFile
    private fun deleteFile() {
        val fileTag = slideshowAdapter.getFileTag(lockableViewPager.currentItem)
        if (fileTag != null) {
            FileIntentService.deleteFiles(applicationContext, listOf(fileTag))
        }
    }

    // fileDeleted
    private fun fileDeleted(deletedPath: String, isDeleted: Boolean) {
        if (isDeleted) {
            deletedPaths.add(deletedPath)
            // If file deleted is showing
            val currentItem = lockableViewPager.currentItem
            val currentFilePath = getCurrentSlideshowBaseFragment()?.getFilePath() ?: ""
            if (deletedPath == currentFilePath) {
                // Check which item to show after delete
                val itemToShowUponDelete =
                    if (currentItem >= slideshowAdapter.getAllFileTags().size - 1) {
                        // Use previous file if last
                        currentItem - 1
                    } else {
                        // Use next file
                        currentItem + 1
                    }
                // If nothing to show, finish activity
                if (itemToShowUponDelete < 0) {
                    // Do not perform transition
                    finish()
                } else {
                    // Scroll to next item
                    lockableViewPager.setCurrentItem(itemToShowUponDelete, true)
                }
            } else {
                // Just remove file path since not visible
                removeFileTag(deletedPath)
            }
        } else {
            snackProgressBarManager.shortSnackBar(
                this,
                preResId = R.string.toast_deleteFile_failed_pre,
                message = PholderTagUtil.getFileNameWithExtension(deletedPath),
                postResId = R.string.toast_deleteFile_failed_post
            )
        }
    }

    // removeFileTag
    private fun removeFileTag(filePath: String) {
        deletedPaths.remove(filePath)
        slideshowAdapter.removeFileTag(filePath)
    }

    // onConfigurationChanged
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        setActionLayout()
    }

    // onSaveInstanceState
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVED_CURRENT_FILE_PATH, getCurrentSlideshowBaseFragment()?.getFilePath() ?: "")
        outState.putBoolean(SAVED_SHOW_SYSTEM_UI, showSystemUI)
    }

    // onStopAction
    override fun onStopAction() {
        unregisterLocalBroadcastReceiver(broadcastReceiver)
    }

    // onFinishAction
    override fun onFinishAction() {
        // onFinish is called after transition, in case finish is called directly, set result here
        setResult(Activity.RESULT_OK, resultIntent)
        // Clear map, don't do in onDestroy as it can happen during rotation
        fileTagMap.remove(getMapKey())
        // Reset showUI in case there was no transition
        showSystemUI(true)
    }

    // onBackPressed
    override fun onBackPressed() {
        // Delegate to fragment if required
        if (getCurrentSlideshowBaseFragment()?.onBackPressed() != true) {
            super.onBackPressed()
        }
    }

}
