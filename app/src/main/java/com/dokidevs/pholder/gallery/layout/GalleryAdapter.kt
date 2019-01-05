package com.dokidevs.pholder.gallery.layout

import android.os.Parcelable
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.d
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseRecyclerViewAdapter
import com.dokidevs.pholder.base.BaseRecyclerViewRenderer
import com.dokidevs.pholder.data.FolderTag
import com.dokidevs.pholder.data.PholderTag
import com.dokidevs.pholder.data.PholderTag.Companion.TYPE_FILE
import com.dokidevs.pholder.data.PholderTag.Companion.TYPE_FOLDER
import com.dokidevs.pholder.data.PholderTagUtil
import com.dokidevs.pholder.data.TitleTag
import com.dokidevs.pholder.gallery.GalleryBaseFragment
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/*--- GalleryAdapter ---*/
class GalleryAdapter(
    private val fragmentTag: String,
    private var galleryAdapterListener: GalleryAdapterListener
) : BaseRecyclerViewAdapter<PholderTag, GalleryBaseViewHolder>(), DokiLog {

    /* companion object */
    companion object {

        /* layout */
        const val LAYOUT_GRID = 0
        const val LAYOUT_LIST = 1

        /* diffUtil */
        private const val POST_UPDATE_DELAY = 130L

    }

    /*--- GalleryAdapterListener ---*/
    interface GalleryAdapterListener {

        // onGalleryAdapterUpdated
        fun onGalleryAdapterUpdated()

        // onThumbnailLoadCompleted
        fun onThumbnailLoadCompleted(view: View, filePath: String)

        // onFolderClick
        fun onFolderClick(view: View, position: Int)

        // onFileClick
        fun onFileClick(view: View, position: Int)

        // onTitleClick
        fun onTitleClick(view: View, position: Int)

        // onItemLongClick
        fun onItemLongClick(view: View, position: Int): Boolean

        // onSelectionUpdated
        fun onSelectionUpdated()

    }

    /* views */
    var galleryViewType: Int = LAYOUT_GRID

    /* selection */
    private val selectedItemMaps = HashMap<Int, LinkedHashMap<String, PholderTag>>()
    private val dragSelectListener = DragSelectTouchListener()

    /* animation */
    private var detailAnimationDuration = 0L
    private var entryLongDuration = 0L
    private var exitLongDuration = 0L

    /* update */
    private var calculatingDiff = false

    // reattachAdapter
    fun reattachAdapter(galleryViewType: Int, retainState: Boolean = true) {
        // Save and restore layoutManager state to retain scroll position
        if (retainState) {
            val state = getLayoutManagerState()
            this.galleryViewType = galleryViewType
            recyclerView.adapter = this
            setLayoutManagerState(state)
        } else {
            this.galleryViewType = galleryViewType
            recyclerView.adapter = this
        }
    }

    // setRecyclerViewProperties
    override fun setRecyclerViewProperties(recyclerView: RecyclerView) {
        entryLongDuration = recyclerView.resources.getInteger(R.integer.animation_duration_entry_long).toLong()
        exitLongDuration = recyclerView.resources.getInteger(R.integer.animation_duration_exit_long).toLong()
        detailAnimationDuration = recyclerView.resources.getInteger(R.integer.animation_duration_detail).toLong()
        recyclerView.setHasFixedSize(true)
        // clear item decorations
        if (recyclerView.itemDecorationCount > 0) {
            for (i in 0 until recyclerView.itemDecorationCount) {
                recyclerView.removeItemDecorationAt(i)
            }
        }
        optimizeRecycledViewPool(recyclerView)
        setDragSelectListener(recyclerView)
    }

    // optimizeRecycledViewPool
    private fun optimizeRecycledViewPool(recyclerView: RecyclerView) {
        when (galleryViewType) {
            LAYOUT_GRID -> {
                // Preserve 4 rows of TYPE_TITLE
                recyclerView.recycledViewPool.setMaxRecycledViews(PholderTag.TYPE_TITLE, 4)
                // Preserve 6 rows of TYPE_FILE (3 per row)
                recyclerView.recycledViewPool.setMaxRecycledViews(TYPE_FILE, 12)
                // Preserve 4 rows of TYPE_FOLDER (2 per row)
                recyclerView.recycledViewPool.setMaxRecycledViews(TYPE_FOLDER, 8)
            }
            LAYOUT_LIST -> {
                // Preserve 6 rows of TYPE_TITLE
                recyclerView.recycledViewPool.setMaxRecycledViews(PholderTag.TYPE_TITLE, 6)
                // Preserve 10 rows of TYPE_FILE (1 per row)
                recyclerView.recycledViewPool.setMaxRecycledViews(PholderTag.TYPE_FILE, 6)
                // Preserve 6 rows of TYPE_FOLDER (1 per row)
                recyclerView.recycledViewPool.setMaxRecycledViews(PholderTag.TYPE_FOLDER, 6)
            }
        }
    }

    // setDragSelectListener
    private fun setDragSelectListener(recyclerView: RecyclerView) {
        dragSelectListener.withSelectListener(object : DragSelectTouchListener.OnAdvancedDragSelectListener {
            override fun onSelectionStarted(start: Int) {
                applyClick(start = start, isSelected = true)
            }

            override fun onSelectionFinished(end: Int) {
            }

            override fun onSelectChange(start: Int, end: Int, isSelected: Boolean) {
                applyClick(start, end, isSelected)
            }
        })
        // Remove the listener first in case we are reattaching the adapter to avoid adding multiple times
        recyclerView.removeOnItemTouchListener(dragSelectListener)
        recyclerView.addOnItemTouchListener(dragSelectListener)
    }

    // startDragSelection
    fun startDragSelection(position: Int) {
        dragSelectListener.startDragSelection(position)
    }

    // setLayoutManager
    override fun setLayoutManager(recyclerView: RecyclerView): RecyclerView.LayoutManager {
        val layoutManager = GridLayoutManager(recyclerView.context, 1)
        layoutManager.spanSizeLookup.isSpanIndexCacheEnabled = true
        if (galleryViewType == LAYOUT_GRID) {
            val screenWidthPx = recyclerView.resources.displayMetrics.widthPixels
            // Use toInt to round down to nearest integer, so our image will be at least the min size defined
            val fileCount = (screenWidthPx / recyclerView.resources.getDimension(R.dimen.file_min_size)).toInt()
            val folderCount = (screenWidthPx / recyclerView.resources.getDimension(R.dimen.folder_min_size)).toInt()
            val spanCount = fileCount * folderCount
            layoutManager.spanCount = spanCount
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val item = items[position]
                    return when (item.getType()) {
                        // The opposite count is the required span for either object
                        TYPE_FILE -> {
                            folderCount
                        }
                        TYPE_FOLDER -> {
                            fileCount
                        }
                        else -> {
                            spanCount
                        }
                    }
                }
            }
            recyclerView.addItemDecoration(GridSpacingItemDecoration(recyclerView.context))
        } else {
            // Use GridLayoutManager with 1 column only as list
        }
        return layoutManager
    }

    // getLayoutManagerState
    private fun getLayoutManagerState(): Parcelable? {
        return layoutManager.onSaveInstanceState()
    }

    // setLayoutManagerState
    private fun setLayoutManagerState(state: Parcelable?) {
        if (state != null) {
            layoutManager.onRestoreInstanceState(state)
        }
    }

    // setViewRenderer
    override fun setViewRenderer(recyclerView: RecyclerView): BaseRecyclerViewRenderer<PholderTag, GalleryBaseViewHolder> {
        return if (galleryViewType == LAYOUT_GRID) {
            GridViewRenderer(galleryAdapterListener)
        } else {
            ListViewRenderer(galleryAdapterListener)
        }
    }

    // updateItems
    fun updateItems(
        newItems: List<PholderTag>,
        calculateDiff: Boolean,
        scrollToUid: String,
        smoothScroll: Boolean,
        highlightItem: Boolean
    ) {
        val start = System.currentTimeMillis()
        // Update selections to make sure selected items do not disappear.
        // This is possible if files are updated while still in selectionMode.
        val wasSelectionMode = isSelectionMode()
        if (wasSelectionMode) {
            recheckSelection(newItems)
        }
        if (calculateDiff && items.size != 0 && Math.abs(newItems.size - items.size) < 150) {
            // Make sure previous calculation is completed as DiffUtil can take a long time if large changes is involved.
            // We can ignore new updates temporarily and let user to call refresh if they want to later.
            // We also don't calculate if list size difference is too much.
            if (!calculatingDiff) {
                doAsync {
                    calculatingDiff = true
                    val diffResult = calculateDiff(newItems)
                    uiThread {
                        if (diffResult != null) {
                            items.clear()
                            items.addAll(newItems)
                            diffResult.dispatchUpdatesTo(this@GalleryAdapter)
                        }
                        calculatingDiff = false
                        d("fragmentTag = $fragmentTag, diffResult = $diffResult, duration = ${System.currentTimeMillis() - start}, size = ${items.size}")
                        recyclerView.postDelayed({
                            postUpdateItems(scrollToUid, smoothScroll, highlightItem)
                        }, POST_UPDATE_DELAY)
                        if (wasSelectionMode) {
                            galleryAdapterListener.onSelectionUpdated()
                        }
                    }
                }
            }
        } else {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
            d("fragmentTag = $fragmentTag, duration = ${System.currentTimeMillis() - start}, size = ${items.size}")
            recyclerView.postDelayed({
                postUpdateItems(scrollToUid, smoothScroll, highlightItem)
            }, POST_UPDATE_DELAY)
            if (wasSelectionMode) {
                galleryAdapterListener.onSelectionUpdated()
            }
        }
    }

    // recheckSelection
    private fun recheckSelection(newItems: List<PholderTag>) {
        // Clear titleTag selection because they will be rechecked
        getSelectedItemMap(PholderTag.TYPE_TITLE).clear()
        // Mark selected items
        for (type_selectedItemMap in selectedItemMaps) {
            val type = type_selectedItemMap.key
            val selectedItemMap = type_selectedItemMap.value
            val newSelectedItemMap = LinkedHashMap<String, PholderTag>(selectedItemMap.size)
            if (selectedItemMap.isNotEmpty()) {
                selectedItemMap.keys.forEach { uid ->
                    for (newItem in newItems) {
                        if (newItem.getUid() == uid) {
                            newItem.isSelected = true
                            newSelectedItemMap[uid] = newItem
                            break
                        }
                    }
                }
                // Reassign selection to map
                selectedItemMaps[type] = newSelectedItemMap
            }
        }
        // If still is selection mode, update the titleTag
        if (isSelectionMode()) {
            var isEveryItemSelected = true
            val newSelectedTitleMap = LinkedHashMap<String, PholderTag>()
            val lastIndex = newItems.size - 1
            // Work from bottom up for easier logic
            for (i in lastIndex downTo 0) {
                val newItem = newItems[i]
                if (newItem.getType() == PholderTag.TYPE_TITLE) {
                    newItem as TitleTag
                    // Show tick box for titleTag
                    newItem.showTickBox = true
                    // Update selection status
                    newItem.isSelected = isEveryItemSelected
                    if (isEveryItemSelected) {
                        newSelectedTitleMap[newItem.getUid()] = newItem
                    }
                    // Reset marker
                    isEveryItemSelected = true
                } else {
                    // Check items below the titleTag
                    if (!newItem.isSelected) {
                        isEveryItemSelected = false
                    }
                }
            }
        }
    }

    // calculateDiff
    private fun calculateDiff(newItems: List<PholderTag>): DiffUtil.DiffResult? {
        val oldItems = items.toList()
        var hasDiff = false
        if (oldItems.size != newItems.size) {
            hasDiff = true
        }
        val diffUtilCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return oldItems.size
            }

            override fun getNewListSize(): Int {
                return newItems.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldPholderTag = oldItems[oldItemPosition]
                val newPholderTag = newItems[newItemPosition]
                if (oldPholderTag != newPholderTag) {
                    hasDiff = true
                    return false
                }
                return true
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldPholderTag = oldItems[oldItemPosition]
                val newPholderTag = newItems[newItemPosition]
                // Check selection status
                if (oldPholderTag.isSelected != newPholderTag.isSelected) {
                    hasDiff = true
                    return false
                }
                when (oldPholderTag.getType()) {
                    TYPE_FOLDER -> {
                        if (FolderTag.calculateDiff(oldPholderTag, newPholderTag) != null) {
                            hasDiff = true
                            return false
                        }
                    }
                    PholderTag.TYPE_TITLE -> {
                        oldPholderTag as TitleTag
                        newPholderTag as TitleTag
                        if (oldPholderTag.showTickBox != newPholderTag.showTickBox) {
                            hasDiff = true
                            return false
                        }
                    }
                }
                return true
            }

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                val oldPholderTag = oldItems[oldItemPosition]
                val newPholderTag = newItems[newItemPosition]
                return when (oldPholderTag.getType()) {
                    TYPE_FOLDER -> {
                        FolderTag.calculateDiff(oldPholderTag, newPholderTag)
                    }
                    else -> null
                }
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffUtilCallback, true)
        return if (hasDiff) {
            diffResult
        } else {
            null
        }
    }

    // postUpdateItems
    private fun postUpdateItems(scrollToUid: String, smoothScroll: Boolean, highlightItem: Boolean) {
        // When diffUtil.dispatchUpdatesTo() is called, changes to adapter is immediate i.e. adapterDataObserver will give callback immediately.
        // However, this does not guarantee the view is processed immediately, even with recyclerView.post() called.
        // onGlobalLayout is not called if the added or removed item is not within original view.
        // onLayoutChanged is not called if the recyclerView itself is not changed in dimension.
        // Hence, there is no way of knowing when the view is processed. The only way is to check whether hasPendingAdapterUpdates(),
        // but there is no callback. After experimenting a lot, the most reliable way is still to add a small delay to allow view to update first.
        // See https://stackoverflow.com/a/36512407/3584439
        var onScrollListener: RecyclerView.OnScrollListener? = null
        if (highlightItem) {
            val position = PholderTagUtil.getPholderTagPosition(items, scrollToUid)
            if (position >= 0) {
                onScrollListener = object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            recyclerView.removeOnScrollListener(this)
                            val itemView = recyclerView.findViewHolderForAdapterPosition(position)?.itemView
                            if (itemView is FolderLayout) itemView.highlight()
                            if (itemView is FolderListLayout) itemView.highlight()
                        }
                    }
                }
                recyclerView.addOnScrollListener(onScrollListener)
            }
        }
        scrollToUid(scrollToUid, smoothScroll, onScrollListener)
        galleryAdapterListener.onGalleryAdapterUpdated()
        // Typically updateItems() is not triggered in selection mode, unless user pause and resume after a long time
        // Then we need to make sure
        if (isSelectionMode()) {
            galleryAdapterListener.onSelectionUpdated()
        }
    }

    // scrollToUid
    fun scrollToUid(
        scrollToUid: String,
        smoothScroll: Boolean,
        onScrollListener: RecyclerView.OnScrollListener? = null
    ): Int {
        val position = if (scrollToUid == GalleryBaseFragment.SCROLL_TO_TOP) {
            0
        } else {
            if (scrollToUid.isNotEmpty()) {
                PholderTagUtil.getPholderTagPosition(items, scrollToUid)
            } else {
                -1
            }
        }
        var firstVisible = -1
        var lastVisible = -1
        var scrollToPosition = -1
        if (position >= 0) {
            val gridLayoutManager = layoutManager as GridLayoutManager
            // Method for checking whether view is completely visible
            firstVisible = gridLayoutManager.findFirstCompletelyVisibleItemPosition()
            lastVisible = gridLayoutManager.findLastCompletelyVisibleItemPosition()
            val isCompletelyVisible = position in firstVisible..lastVisible
            // Method for checking whether view is partially visible or not visible
            // See https://android-developers.googleblog.com/2018/02/continuous-shared-element-transitions.html
            var isVisible = true
            val viewAtPosition = gridLayoutManager.findViewByPosition(position)
            if (viewAtPosition == null || gridLayoutManager.isViewPartiallyVisible(viewAtPosition, false, true)) {
                isVisible = false
            }
            d(
                "fragment = $fragmentTag, scrollToUid = $scrollToUid, position = $position, " +
                        "isCompletelyVisible = $isCompletelyVisible, isVisible = $isVisible"
            )
            if (!isVisible) {
                scrollToPosition = when {
                    // If is scrolling down, scroll to position
                    position > lastVisible -> position
                    // If scrolling up, for aesthetic purpose, scroll to one position before so it won't stick to top
                    position - 1 >= 0 -> position - 1
                    // Else, just go to position
                    else -> position
                }
            }
        }
        if (scrollToPosition >= 0) {
            if (!smoothScroll ||
                // Don't smoothScroll if too far
                Math.abs(scrollToPosition - (firstVisible + lastVisible) / 2) > 60
            ) {
                recyclerView.post { recyclerView.scrollToPosition(scrollToPosition) }
            } else {
                recyclerView.post { recyclerView.smoothScrollToPosition(scrollToPosition) }
            }
        } else {
            // Forcefully notify onScrollListener that scrolling event is completed without scrolling
            onScrollListener?.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_IDLE)
        }
        return position
    }

    // isSelectionMode
    private fun isSelectionMode(): Boolean {
        selectedItemMaps.forEach { type_SelectedItemMap ->
            if (type_SelectedItemMap.value.isNotEmpty()) {
                return true
            }
        }
        return false
    }

    // startSelectionMode
    fun startSelectionMode() {
        showTickBoxes(true)
    }

    // titleSelect
    fun titleSelect(position: Int) {
        val titleTag = items[position]
        if (titleTag is TitleTag) {
            // Inverse the selection
            val isSelected = !titleTag.isSelected
            applySelection(titleTag, isSelected)
            // Continue to set the same status until the next titleTag
            for (i in position + 1 until items.size) {
                val item = items[i]
                if (item is TitleTag) break
                applySelection(item, isSelected)
            }
            galleryAdapterListener.onSelectionUpdated()
        }
    }

    // applyClick
    fun applyClick(start: Int, end: Int = -1, isSelected: Boolean) {
        if (end == -1) {
            // For single item
            applySelection(items[start], isSelected)
            // Check titleTag is selected or not
            updateTitleSelection(start, isSelected)
        } else {
            // For multiple items
            for (position in start..end) {
                // Always set isSelected for item in list. This is because after any action, items are regenerated
                // and existing viewHolders are still holding reference to oldItems. They are only refreshed at onBindViewHolder.
                val item = items[position]
                // Skip applying selection for title during multi select
                if (item.getType() != PholderTag.TYPE_TITLE) {
                    applySelection(item, isSelected)
                }
            }
            // Check titleTag is selected or not
            updateTitleSelection(start, isSelected)
            updateTitleSelection(end, isSelected)
        }
        galleryAdapterListener.onSelectionUpdated()
    }

    // updateTitleSelection
    private fun updateTitleSelection(position: Int, isSelected: Boolean) {
        // Ignore if selection type is title itself
        if (items[position].getType() != PholderTag.TYPE_TITLE) {
            if (!isSelected) {
                // Work upwards to deselect dateTag
                for (i in position downTo 0) {
                    val item = items[i]
                    if (item.getType() == PholderTag.TYPE_TITLE) {
                        applySelection(item, false)
                        return
                    }
                }
            } else {
                // Work upwards to check if all other tags are selected
                var titleTag: TitleTag? = null
                for (i in position downTo 0) {
                    val item = items[i]
                    if (item.getType() == PholderTag.TYPE_TITLE) {
                        titleTag = item as TitleTag
                        break
                    }
                    // Return as long as one item is not selected
                    if (!item.isSelected) return
                }
                // Work downwards to check if all other tags are selected
                for (i in position until items.size) {
                    val item = items[i]
                    if (item.getType() == PholderTag.TYPE_TITLE) break
                    // Return as long as one item is not selected
                    if (!item.isSelected) return
                }
                if (titleTag != null) {
                    applySelection(titleTag, true)
                }
            }
        }
    }

    // applySelection
    private fun applySelection(item: PholderTag, isSelected: Boolean) {
        item.isSelected = isSelected
        updateSelectedItem(item)
        // If viewHolder is valid, setSelected will update the status for tag attached to the viewHolder
        getViewHolder(item.getUid())?.setSelected(isSelected, true)
    }

    // selectAll
    fun selectAll() {
        clearSelectedItems()
        items.forEach { item ->
            item.isSelected = true
            updateSelectedItem(item)
        }
        existingViewHolders.forEach { viewHolder ->
            viewHolder.setSelected(true, true)
        }
        galleryAdapterListener.onSelectionUpdated()
    }

    // deselectAll
    private fun deselectAll() {
        clearSelectedItems()
        items.forEach { item ->
            item.isSelected = false
        }
        existingViewHolders.forEach { viewHolder ->
            viewHolder.setSelected(false, true)
        }
    }

    // endSelectionMode
    fun endSelectionMode() {
        deselectAll()
        showTickBoxes(false)
    }

    // getSelectedItemMap
    fun getSelectedItemMap(type: Int): LinkedHashMap<String, PholderTag> {
        var selectedItemMap = selectedItemMaps[type]
        // If hashMap is empty, initialise it
        if (selectedItemMap == null) {
            selectedItemMap = linkedMapOf()
            selectedItemMaps[type] = selectedItemMap
        }
        return selectedItemMap
    }

    // getItem
    fun getItem(uid: String): PholderTag? {
        for (item in items) {
            if (item.getUid() == uid) {
                return item
            }
        }
        return null
    }

    // isSelected
    private fun isSelected(item: PholderTag): Boolean {
        return getSelectedItemMap(item.getType()).contains(item.getUid())
    }

    // updateSelectedItem
    private fun updateSelectedItem(item: PholderTag) {
        if (item.isSelected) {
            getSelectedItemMap(item.getType())[item.getUid()] = item
        } else {
            getSelectedItemMap(item.getType()).remove(item.getUid())
        }
    }

    // clearSelectedItems
    private fun clearSelectedItems() {
        selectedItemMaps.forEach { it.value.clear() }
    }

    // explodeViews
    fun explodeViews(originX: Float, originY: Float, originWidth: Int, originHeight: Int) {
        if (originX != 0f || originY != 0f) {
            existingViewHolders.forEach { viewHolder ->
                val view = viewHolder.itemView
                view.x = originX + originWidth / 2 - view.width / 2
                view.y = originY + originHeight / 2 - view.height / 2
                view.animate()
                    .translationX(0f)
                    .translationY(0f)
                    .setDuration(detailAnimationDuration)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .withEndAction {
                        // In case the animation is cancelled, make sure the views are at correct location
                        view.translationX = 0f
                        view.translationY = 0f
                    }
                    .start()
            }
        }
    }

    // collapseViews
    fun collapseViews(destinationX: Float, destinationY: Float, destinationWidth: Int, destinationHeight: Int) {
        if (destinationX != 0f || destinationY != 0f) {
            existingViewHolders.forEach { viewHolder ->
                val view = viewHolder.itemView
                view.animate()
                    .x(destinationX + destinationWidth / 2 - view.width / 2)
                    .y(destinationY + destinationHeight / 2 - view.height / 2)
                    .setDuration(detailAnimationDuration)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            }
        }
    }

    // showTickBoxes
    private fun showTickBoxes(show: Boolean) {
        items.forEach { item ->
            if (item.getType() == PholderTag.TYPE_TITLE) {
                item as TitleTag
                item.showTickBox = show
            }
        }
        existingViewHolders.forEach { viewHolder ->
            if (viewHolder.getType() == PholderTag.TYPE_TITLE) {
                viewHolder as TitleViewHolder
                viewHolder.showTickBox(show)
            }
        }
    }

}