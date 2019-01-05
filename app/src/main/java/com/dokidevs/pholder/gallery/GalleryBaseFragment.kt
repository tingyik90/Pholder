package com.dokidevs.pholder.gallery

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dokidevs.pholder.PholderApplication.Companion.prefManager
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseFragment
import com.dokidevs.pholder.data.FileTag
import com.dokidevs.pholder.data.FolderTag
import com.dokidevs.pholder.data.PholderDatabase.Companion.PUBLIC_ROOT
import com.dokidevs.pholder.data.PholderTag
import com.dokidevs.pholder.data.PholderTag.Companion.TYPE_FILE
import com.dokidevs.pholder.data.PholderTag.Companion.TYPE_FOLDER
import com.dokidevs.pholder.data.PholderTagUtil
import com.dokidevs.pholder.gallery.layout.GalleryAdapter
import com.dokidevs.pholder.gallery.layout.GalleryAdapter.Companion.LAYOUT_GRID
import com.dokidevs.pholder.slideshow.SlideshowActivity
import com.dokidevs.pholder.utils.PrefManager.Companion.PREF_GALLERY_VIEW_TYPE
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File

/*--- GalleryBaseFragment ---*/
abstract class GalleryBaseFragment : BaseFragment(), GalleryAdapter.GalleryAdapterListener {

    /* companion object */
    companion object {

        /* scroll */
        const val SCROLL_TO_UID = "SCROLL_TO_UID"
        const val SCROLL_TO_TOP = "SCROLL_TO_TOP"

        /* saved instance states */
        private const val SAVED_FOLDER_CLICK_PATH = "SAVED_FOLDER_CLICK_PATH"

        /* intents */
        private const val ROOT_PATH = "ROOT_PATH"
        private const val ORIGIN_X = "ORIGIN_X"
        private const val ORIGIN_Y = "ORIGIN_Y"
        private const val ORIGIN_WIDTH = "ORIGIN_WIDTH"
        private const val ORIGIN_HEIGHT = "ORIGIN_HEIGHT"

    }

    /*--- FragmentListener ---*/
    interface FragmentListener {

        // onGalleryBaseFragmentReady
        fun onGalleryBaseFragmentReady(tabPosition: Int, fragment: GalleryBaseFragment, isFirstReady: Boolean)

        // startSelectionMode
        fun startSelectionMode()

        // onSelectionUpdated
        fun onSelectionUpdated()

        // endSelectionMode()
        fun endSelectionMode()

        // swipeRefresh
        fun swipeRefresh(updateNow: Boolean = true)

        // startSlideshow
        fun startSlideshow(initialFilePath: String, view: View, fileTags: List<FileTag>)

        // onThumbnailLoadCompleted
        fun onThumbnailLoadCompleted(fragment: GalleryBaseFragment, view: View, filePath: String)

        // cancelSlideshowBackTransition
        fun cancelSlideshowBackTransition()

    }

    /* onPopBackStackReadyListener */
    private var onPopBackStackReadyListener: (
        (destinationX: Float, destinationY: Float, destinationWidth: Int, destinationHeight: Int) -> Unit
    )? = null

    // setOnPopBackStackReadyListener
    fun setOnPopBackStackReadyListener(
        onPopBackStackReadyListener: (
            (destinationX: Float, destinationY: Float, destinationWidth: Int, destinationHeight: Int) -> Unit
        )?
    ) {
        this.onPopBackStackReadyListener = onPopBackStackReadyListener
    }

    /* FragmentListener */
    protected var fragmentListener: FragmentListener? = null

    /* views */
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private var globalLayoutHandler: Handler? = null
    private var globalLayoutRunnable: Runnable? = null
    private var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var swipeRefreshStartOffSet = 0
    private var swipeRefreshEndOffSet = 0
    private var swipeRefreshDistance = 0
    private var galleryViewTypeHandler: Handler? = null
    private var galleryViewTypeRunnable: Runnable? = null

    /* parameters */
    var isSelectionMode = false
        protected set
    var rootFile = PUBLIC_ROOT
        protected set
    private var folderClickPath = ""
    private var isFirstStart = true
    private var isExplodingViews = false
    private var checkedVideoThumbnails = hashSetOf<String>()

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

    // setBundle
    fun setBundle(rootPath: String, originX: Float, originY: Float, originWidth: Int, originHeight: Int) {
        val bundle = Bundle()
        bundle.putString(ROOT_PATH, rootPath)
        bundle.putFloat(ORIGIN_X, originX)
        bundle.putFloat(ORIGIN_Y, originY)
        bundle.putInt(ORIGIN_WIDTH, originWidth)
        bundle.putInt(ORIGIN_HEIGHT, originHeight)
        arguments = bundle
    }

    // onAttachAction
    override fun onAttachAction(context: Context?) {
        if (context is FragmentListener) {
            fragmentListener = context
        } else {
            throw RuntimeException("$context must implement ${getFragmentClass()}.FragmentListener")
        }
    }

    // getGalleryTabFragment
    private fun getGalleryTabFragment(): GalleryTabFragment? {
        return parentFragment as GalleryTabFragment?
    }

    // onCreateAction
    override fun onCreateAction(savedInstanceState: Bundle?) {
        // Keep the same instance of adapter to avoid losing instance when replace fragment
        // or during rotation (if rotation is not handled manually)
        galleryAdapter = GalleryAdapter(getFragmentTag(), this)
        rootFile = File(getRootPath())
        // In case activity was destroyed to free up memory, need to restore the parameters
        if (savedInstanceState != null) {
            isFirstStart = false
            folderClickPath = savedInstanceState.getString(SAVED_FOLDER_CLICK_PATH, folderClickPath)
        }
    }

    // getRootPath
    fun getRootPath(): String {
        val rootPath = arguments?.getString(ROOT_PATH)
        return if (rootPath != null && rootPath.isNotEmpty()) {
            return rootPath
        } else {
            getDefaultRootFile().absolutePath
        }
    }

    abstract fun getDefaultRootFile(): File

    // getOriginX
    private fun getOriginX(): Float {
        return arguments?.getFloat(ORIGIN_X) ?: 0f
    }

    // getOriginY
    private fun getOriginY(): Float {
        return arguments?.getFloat(ORIGIN_Y) ?: 0f
    }

    // getOriginWidth
    private fun getOriginWidth(): Int {
        return arguments?.getInt(ORIGIN_WIDTH) ?: 0
    }

    // getOriginHeight
    private fun getOriginHeight(): Int {
        return arguments?.getInt(ORIGIN_HEIGHT) ?: 0
    }

    // onCreateViewAction
    override fun onCreateViewAction(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gallery_base, container, false)
    }

    // onViewCreatedAction
    override fun onViewCreatedAction(view: View, savedInstanceState: Bundle?) {
        swipeRefreshLayout = view.findViewById(R.id.galleryBaseFragment_swipeRefreshLayout)
        emptyView = view.findViewById(R.id.galleryBaseFragment_emptyView)
        recyclerView = view.findViewById(R.id.galleryBaseFragment_recyclerView)
        val galleryViewType = prefManager.get(PREF_GALLERY_VIEW_TYPE, LAYOUT_GRID)
        setGalleryViewType(galleryViewType)
        setSwipeRefresh()
        setRecyclerView()
        setEmptyView()
        // Request inset again, because the window will only send insets down automatically if the
        // aggregated system ui visibility value for the entire view hierarchy changes.
        // See https://medium.com/androiddevelopers/windows-insets-fragment-transitions-9024b239a436
        ViewCompat.requestApplyInsets(view)
        // Always postpone transition and update items when view is created, this can be first creation
        // or coming back from popBackStack.
        postponeEnterTransition()
        updateItems(false)
    }

    // setGalleryViewType
    private fun setGalleryViewType(galleryViewType: Int) {
        // For first instance, attach the new adapter
        if (recyclerView.adapter == null) {
            galleryAdapter.galleryViewType = galleryViewType
            recyclerView.adapter = galleryAdapter
        }
        // For navigating back from subfolder, reattach adapter if galleryViewType is different. Retain scroll state.
        else if (galleryAdapter.galleryViewType != galleryViewType) {
            galleryAdapter.reattachAdapter(galleryViewType, true)
        }
    }

    // switchGalleryViewType
    fun switchGalleryViewType(galleryViewType: Int) {
        galleryAdapter.reattachAdapter(galleryViewType, true)
    }

    // setSwipeRefresh
    private fun setSwipeRefresh() {
        // Set own value to give consistent behaviour before and after selection mode.
        // There is a "mUsingCustomStart" parameter that caused the calculation method to differ from default
        // whenever user set their own value. Below values are set as per original swipeRefreshLayout values
        // that offset "mUsingCustomStart" effects.
        swipeRefreshStartOffSet = -(swipeRefreshLayout.progressCircleDiameter)
        swipeRefreshEndOffSet = (swipeRefreshLayout.progressCircleDiameter * 0.6f).toInt()
        swipeRefreshDistance = (swipeRefreshLayout.progressCircleDiameter * 1.6f).toInt()
        swipeRefreshLayout.setProgressViewOffset(true, swipeRefreshStartOffSet, swipeRefreshEndOffSet)
        swipeRefreshLayout.setDistanceToTriggerSync(swipeRefreshDistance)
        swipeRefreshLayout.setOnRefreshListener {
            fragmentListener?.swipeRefresh()
        }
    }

    // animateSwipeRefresh
    fun animateSwipeRefresh() {
        if (!swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = true
        }
    }

    // setRecyclerView
    private fun setRecyclerView() {
        val paddingBottom = resources.getDimension(R.dimen.grid_2x).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { view, insets ->
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                insets.systemWindowInsetBottom + paddingBottom
            )
            insets
        }
    }

    // setEmptyView
    private fun setEmptyView() {
        emptyView.text = getEmptyViewMessage()
        emptyView.isVisible = galleryAdapter.getAllItems().isEmpty()
    }

    // getEmptyViewMessage
    protected abstract fun getEmptyViewMessage(): String

    // onResumeAction
    override fun onResumeAction() {
        // 1. For normal session, when the new fragment is created, onResume is postponed until
        //    galleryAdapter is updated and called startPostponedEnterTransition.
        //    Call notifyFragmentReady() in onResume to proceed with second update.
        // 2. After onPause, the view is already ready, so can call notifyFragmentReady() immediately.
        // 3. When resume from a destroyed state due to android memory limit, postponeEnterTransition()
        //    has no effect and fragment will proceed to onResume immediately, even when the galleryAdapter
        //    is still updating. This is ok as we will drop the back transition if view is not found via
        //    cancelSlideshowBackTransition().
        notifyFragmentReady()
    }

    // notifyFragmentReady
    private fun notifyFragmentReady() {
        val galleryTabFragment = getGalleryTabFragment()
        if (galleryTabFragment != null) {
            val isFirstReady = !galleryTabFragment.isReady
            galleryTabFragment.isReady = true
            fragmentListener?.onGalleryBaseFragmentReady(galleryTabFragment.getTabPosition(), this, isFirstReady)
        }
    }

    // onThumbnailLoadCompleted
    override fun onThumbnailLoadCompleted(view: View, filePath: String) {
        // Inform activity to perform transition if required
        fragmentListener?.onThumbnailLoadCompleted(this, view, filePath)
        checkVideoThumbnail(view, filePath)
    }

    // checkVideoThumbnail
    private fun checkVideoThumbnail(view: View, filePath: String) {
        // If it is video and is not checked yet
        if (PholderTagUtil.isVideo(filePath) && !checkedVideoThumbnails.contains(filePath)) {
            val fileTag = view.tag as? FileTag
            // If it has a valid MediaStoreId
            if (fileTag != null && fileTag.mediaStoreId > -1L) {
                doAsync {
                    // Check if thumbnail is present in MediaStore
                    val cursor = context?.contentResolver?.query(
                        MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Video.Thumbnails.DATA),
                        MediaStore.Video.Thumbnails.VIDEO_ID + " = ?",
                        arrayOf(fileTag.mediaStoreId.toString()),
                        null
                    )
                    if (cursor != null && cursor.moveToNext()) {
                        // There is a valid thumbnail
                    } else {
                        // Else, force generate thumbnail so that it will load faster next time
                        val contentResolver = context?.contentResolver
                        if (contentResolver != null) {
                            MediaStore.Video.Thumbnails.getThumbnail(
                                contentResolver,
                                fileTag.mediaStoreId,
                                MediaStore.Video.Thumbnails.MINI_KIND,
                                null
                            )
                        }
                    }
                    cursor?.close()
                }
            }
            checkedVideoThumbnails.add(filePath)
        }
    }

    // onConfigurationChanged
    override fun onConfigurationChanged(newConfig: Configuration?) {
        // Refresh recyclerView to make sure span is correct, retain same view type
        galleryAdapter.reattachAdapter(galleryAdapter.galleryViewType)
        super.onConfigurationChanged(newConfig)
    }

    // getToolbarTitle
    fun getToolbarTitle(): String {
        return if (isSelectionMode) {
            val selectedItemCount =
                galleryAdapter.getSelectedItemMap(TYPE_FOLDER).size + galleryAdapter.getSelectedItemMap(TYPE_FILE).size
            return "$selectedItemCount selected"
        } else {
            getToolbarTitleName()
        }
    }

    // getToolbarTitleName
    protected abstract fun getToolbarTitleName(): String

    // onReenterFromSlideshowActivity
    fun onReenterFromSlideshowActivity(scrollToUid: String) {
        globalLayoutHandler = Handler()
        globalLayoutRunnable = Runnable {
            recyclerView.viewTreeObserver.dispatchOnGlobalLayout()
        }
        // onGlobalLayoutChangeListener listens to child change as well, onLayoutChange is only triggered if there is
        // dimension change to recyclerView itself, which is not happening if no orientation change after navigating
        // back from SlideshowActivity. See https://stackoverflow.com/a/38884124/3584439
        onGlobalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                globalLayoutHandler?.removeCallbacks(globalLayoutRunnable)
                val viewHolder = galleryAdapter.getViewHolder(scrollToUid)
                if (viewHolder != null) {
                    // viewHolder is available, scroll to position
                    val thumbnailLayout = viewHolder.itemView as SlideshowActivity.SlideshowTransitionInterface
                    if (PholderTagUtil.isVideo(scrollToUid)) {
                        thumbnailLayout.onReenterPreparation()
                    }
                    val onScrollListener = object : RecyclerView.OnScrollListener() {
                        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                            // galleryAdapter.scrollToUid() will call onScrollStateChanged(SCROLL_STATE_IDLE) if no scrolling occurs
                            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                                recyclerView.removeOnScrollListener(this)
                                if (thumbnailLayout.isThumbnailLoadComplete()) {
                                    // Immediately start transition
                                    onThumbnailLoadCompleted(thumbnailLayout as View, scrollToUid)
                                } else {
                                    // Postpone transition until onThumbnailLoadCompleted
                                }
                            }
                        }

                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            // Code below is left here in case scrollToPosition is used instead while the view is partially visible.
                            // This will call onScrolled with dy = 0. See https://stackoverflow.com/a/27830439/3584439
                            if (dy == 0) {
                                recyclerView.removeOnScrollListener(this)
                                if (thumbnailLayout.isThumbnailLoadComplete()) {
                                    onThumbnailLoadCompleted(thumbnailLayout as View, scrollToUid)
                                }
                            }
                        }
                    }
                    recyclerView.addOnScrollListener(onScrollListener)
                    // Use smoothScroll to ensure onScrollListener is called
                    // onScrollListener might not be called when the view is partially visible and calling scrollToPosition
                    // See https://stackoverflow.com/a/45687986/3584439
                    galleryAdapter.scrollToUid(scrollToUid, true, onScrollListener)
                } else {
                    // viewHolder not available, scroll to position and postpone transition until onThumbnailLoadCompleted
                    val itemPosition = galleryAdapter.scrollToUid(scrollToUid, false)
                    // In case activity is destroyed or item is not available, force transition to start to avoid app hanging
                    if (itemPosition < 0) {
                        fragmentListener?.cancelSlideshowBackTransition()
                    }
                }
            }
        }
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
        // In the rare event where onGlobalLayout is not called due to activity destroyed causing race conditions etc,
        // force the listener to be called after some delay.
        globalLayoutHandler?.postDelayed(globalLayoutRunnable, 150L)
    }

    // updateItems
    fun updateItems(
        calculateDiff: Boolean,
        scrollToUid: String = "",
        smoothScroll: Boolean = false,
        highlightItem: Boolean = false
    ) {
        doAsync {
            // This will return null if the rootFile does not exist anymore
            val newItems = generateItems()
            uiThread {
                if (newItems != null) {
                    val scrollToUidUpdated = if (newItems.isNotEmpty() && scrollToUid == SCROLL_TO_TOP) {
                        newItems[0].getUid()
                    } else {
                        scrollToUid
                    }
                    galleryAdapter.updateItems(
                        newItems,
                        calculateDiff,
                        scrollToUidUpdated,
                        smoothScroll,
                        highlightItem
                    )
                } else {
                    // In case folder deleted, but fragment is still in its sub-folder, move back in history
                    getGalleryTabFragment()?.onBackPressed()
                }
            }
        }
    }

    // generateItems
    protected abstract fun generateItems(): List<PholderTag>?

    // onGalleryAdapterUpdated
    override fun onGalleryAdapterUpdated() {
        // onGalleryAdapterUpdated can be called by handler after async operation, which by then the fragment can be detached.
        // Check that the fragment is attached before making UI changes.
        if (context != null) {
            // Cancel refresh animation
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }
            setEmptyView()
            // Animate if is first start from folder click
            if (isFirstStart) {
                isFirstStart = false
                isExplodingViews = true
                galleryAdapter.explodeViews(getOriginX(), getOriginY(), getOriginWidth(), getOriginHeight())
                // Don't allow click until after animation done, to avoid animation cancel
                val handler = Handler()
                handler.postDelayed({
                    isExplodingViews = false
                }, resources.getInteger(R.integer.animation_duration_detail).toLong())
            }
            // Animate if back from back pressed
            if (folderClickPath.isNotEmpty()) {
                val viewHolder = galleryAdapter.getViewHolder(folderClickPath)
                folderClickPath = ""
                if (viewHolder != null) {
                    val view = viewHolder.itemView
                    onPopBackStackReadyListener?.invoke(view.x, view.y, view.width, view.height)
                }
            }
            startPostponedEnterTransition()
            notifyFragmentReady()
        }
    }

    // animatePopBackStack
    fun animatePopBackStack(destinationX: Float, destinationY: Float, destinationWidth: Int, destinationHeight: Int) {
        galleryAdapter.collapseViews(destinationX, destinationY, destinationWidth, destinationHeight)
    }

    // onFolderClick
    override fun onFolderClick(view: View, position: Int) {
        // Only react after animation
        if (!isExplodingViews) {
            val folderTag = view.tag as FolderTag
            if (!isSelectionMode) {
                val rootPath = folderTag.getFilePath()
                folderClickPath = rootPath
                getGalleryTabFragment()?.showNextFragment(rootPath, view.x, view.y, view.width, view.height)
            } else {
                // Toggle click
                galleryAdapter.applyClick(start = position, isSelected = !folderTag.isSelected)
            }
        }
    }

    // onFileClick
    @Suppress("UNCHECKED_CAST")
    override fun onFileClick(view: View, position: Int) {
        // Only react after animation
        if (!isExplodingViews) {
            val fileTag = view.tag as FileTag
            if (!isSelectionMode) {
                val fileTags = galleryAdapter.getAllItems().filter { item ->
                    item.getType() == TYPE_FILE
                } as List<FileTag>
                fragmentListener?.startSlideshow(fileTag.getFilePath(), view, fileTags)
            } else {
                // Toggle click
                galleryAdapter.applyClick(start = position, isSelected = !fileTag.isSelected)
            }
        }
    }

    // onTitleClick
    override fun onTitleClick(view: View, position: Int) {
        // Only react after animation
        if (!isExplodingViews) {
            if (isSelectionMode) {
                galleryAdapter.titleSelect(position)
            }
        }
    }

    // onItemLongClick
    override fun onItemLongClick(view: View, position: Int): Boolean {
        // Only react after animation
        if (!isExplodingViews) {
            if (!isSelectionMode) {
                startSelectionMode()
            }
            galleryAdapter.startDragSelection(position)
        }
        // Always consume long click
        return true
    }

    // startSelectionMode
    private fun startSelectionMode() {
        isSelectionMode = true
        fragmentListener?.startSelectionMode()
        galleryAdapter.startSelectionMode()
        // Disable swipe refresh, but the refresh is still triggered if user move finger in first long click.
        // Hide the refresh animation to below layout and make the trigger distance longer than layout height to prevent refresh.
        swipeRefreshLayout.isEnabled = false
        swipeRefreshLayout.setProgressViewOffset(true, swipeRefreshLayout.height + 300, swipeRefreshLayout.height + 500)
        swipeRefreshLayout.setDistanceToTriggerSync(swipeRefreshLayout.height * 2)
    }

    // getItemSize
    fun getItemSize(): Int {
        return galleryAdapter.itemCount
    }

    // getSelectedTags
    fun getSelectedTags(): List<PholderTag> {
        val selectedTags = mutableListOf<PholderTag>()
        selectedTags.addAll(getSelectedFolderTags())
        selectedTags.addAll(getSelectedFileTags())
        return selectedTags
    }

    // getSelectedFolderTags
    @Suppress("UNCHECKED_CAST")
    fun getSelectedFolderTags(): List<FolderTag> {
        return galleryAdapter.getSelectedItemMap(TYPE_FOLDER).values.toList() as List<FolderTag>
    }

    // getSelectedFileTags
    @Suppress("UNCHECKED_CAST")
    fun getSelectedFileTags(): List<FileTag> {
        return galleryAdapter.getSelectedItemMap(TYPE_FILE).values.toList() as List<FileTag>
    }

    // getSelectedFolderCount
    fun getSelectedFolderCount(): Int {
        return galleryAdapter.getSelectedItemMap(TYPE_FOLDER).size
    }

    // getSelectedFileCount
    fun getSelectedFileCount(): Int {
        return galleryAdapter.getSelectedItemMap(TYPE_FILE).size
    }

    override fun onSelectionUpdated() {
        if (galleryAdapter.getSelectedItemMap(TYPE_FOLDER).size > 0 ||
            galleryAdapter.getSelectedItemMap(TYPE_FILE).size > 0
        ) {
            fragmentListener?.onSelectionUpdated()
        } else {
            endSelectionMode()
        }
    }

    // selectAll
    fun selectAll() {
        if (isSelectionMode) {
            galleryAdapter.selectAll()
        }
    }

    // endSelectionMode
    fun endSelectionMode() {
        isSelectionMode = false
        galleryAdapter.endSelectionMode()
        fragmentListener?.endSelectionMode()
        // Restore swipe refresh layout
        swipeRefreshLayout.isEnabled = true
        swipeRefreshLayout.setProgressViewOffset(true, swipeRefreshStartOffSet, swipeRefreshEndOffSet)
        swipeRefreshLayout.setDistanceToTriggerSync(swipeRefreshDistance)
    }

    // onSaveInstanceState
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVED_FOLDER_CLICK_PATH, folderClickPath)
    }

    // onDestroyViewAction
    override fun onDestroyViewAction() {
        // Remove adapter and its interface to avoid leak
        globalLayoutHandler?.removeCallbacks(globalLayoutRunnable)
        galleryViewTypeHandler?.removeCallbacks(galleryViewTypeRunnable)
        recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        recyclerView.adapter = null
    }

    // onDetachAction
    override fun onDetachAction() {
        fragmentListener = null
    }

    // onBackPressed
    override fun onBackPressed(): Boolean {
        if (isSelectionMode) {
            endSelectionMode()
            return true
        }
        // Consume backPress if it is pop back stack
        val isPopBackStack = getGalleryTabFragment()?.onBackPressed() ?: false
        if (isPopBackStack) return true
        return false
    }

}

