package com.dokidevs.pholder.gallery.layout

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dokidevs.pholder.R
import com.dokidevs.pholder.data.PholderTag
import com.dokidevs.pholder.data.PholderTag.Companion.TYPE_FILE
import com.dokidevs.pholder.data.PholderTag.Companion.TYPE_FOLDER

/*--- GridSpacingItemDecoration ---*/
class GridSpacingItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    /* parameters */
    private val thumbnailSpacingPx by lazy { context.resources.getDimension(R.dimen.file_spacing) }
    private val folderSpacingPx by lazy { context.resources.getDimension(R.dimen.folder_spacing) }

    // getItemOffsets
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val pholderTag = view.tag as PholderTag
        if (needOffsets(pholderTag)) {
            val manager = parent.layoutManager as GridLayoutManager
            val position = parent.getChildAdapterPosition(view)
            // position = -1 for views that are already removed by diffUtil
            if (position >= 0) {
                val spanCount = manager.spanCount
                val spanIndex = manager.spanSizeLookup.getSpanIndex(position, spanCount)
                val spanSize = manager.spanSizeLookup.getSpanSize(position)
                val spacingPx = getSpacingPx(pholderTag)
                if (includeHorizontalEdge(pholderTag)) {
                    outRect.left = Math.round(spacingPx.first - spanIndex * spacingPx.first / spanCount)
                    outRect.right = Math.round((spanIndex + spanSize) * spacingPx.first / spanCount)
                    outRect.top = Math.round(spacingPx.second)
                    outRect.bottom = Math.round(spacingPx.third)
                } else {
                    outRect.left = Math.round(spanIndex * spacingPx.first / spanCount)
                    outRect.right = Math.round(spacingPx.first - (spanIndex + spanSize) * spacingPx.first / spanCount)
                    outRect.top = Math.round(spacingPx.second)
                    outRect.bottom = Math.round(spacingPx.third)
                }
            }
        }
    }

    // needOffsets
    private fun needOffsets(pholderTag: PholderTag): Boolean {
        return when (pholderTag.getType()) {
            TYPE_FOLDER, TYPE_FILE -> true
            else -> false
        }
    }

    // includeHorizontalEdge
    private fun includeHorizontalEdge(pholderTag: PholderTag): Boolean {
        return when (pholderTag.getType()) {
            TYPE_FOLDER, TYPE_FILE -> false
            else -> false
        }
    }

    // getSpacingPx
    private fun getSpacingPx(pholderTag: PholderTag): Triple<Float, Float, Float> {
        // Spacing for horizontal, top, bottom
        return when (pholderTag.getType()) {
            TYPE_FOLDER -> {
                Triple(folderSpacingPx, 0f, folderSpacingPx)
            }
            TYPE_FILE -> {
                Triple(thumbnailSpacingPx, 0f, thumbnailSpacingPx)
            }
            else -> {
                Triple(0f, 0f, 0f)
            }
        }
    }

}