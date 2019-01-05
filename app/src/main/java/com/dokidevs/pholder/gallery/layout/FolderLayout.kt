package com.dokidevs.pholder.gallery.layout

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.bumptech.glide.load.engine.GlideException
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.e
import com.dokidevs.pholder.PholderApplication.Companion.isDarkTheme
import com.dokidevs.pholder.R
import com.dokidevs.pholder.data.FolderTag
import com.dokidevs.pholder.data.FolderTag.Companion.DIFF_COUNT_FILE
import com.dokidevs.pholder.data.FolderTag.Companion.DIFF_COUNT_FOLDER
import com.dokidevs.pholder.data.FolderTag.Companion.DIFF_STAR
import com.dokidevs.pholder.data.FolderTag.Companion.DIFF_THUMBNAIL
import com.dokidevs.pholder.data.PholderTag
import com.dokidevs.pholder.utils.GlideApp
import com.dokidevs.pholder.utils.GlideUtil
import kotlinx.android.synthetic.main.layout_folder.view.*

/*--- FolderLayout ---*/
class FolderLayout : ConstraintLayout, GalleryAdapterView, DokiLog {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, attributeSetId: Int) : super(context, attrs, attributeSetId)

    /* views */
    private val main by lazy { folderLayout_main }
    private val name by lazy { folderLayout_name }
    private val count by lazy { folderLayout_count }
    private val star by lazy { folderLayout_star }
    private val thumbnail by lazy { folderLayout_thumbnail }
    private val tick by lazy { folderLayout_tick }

    // setItem
    override fun bindView(item: PholderTag, payloads: MutableList<Any>?) {
        if (isDarkTheme) {
            main.setBackgroundResource(R.drawable.bg_rectangle_rounded_white)
        } else {
            main.setBackgroundResource(R.drawable.bg_rectangle_rounded_black)
        }
        if (payloads != null && payloads.isNotEmpty()) {
            val diffBundle = payloads[0] as Bundle
            if (diffBundle.containsKey(DIFF_THUMBNAIL)) {
                setThumbnail(diffBundle.getString(DIFF_THUMBNAIL, ""))
            }
            if (diffBundle.containsKey(DIFF_COUNT_FOLDER) || diffBundle.containsKey(DIFF_COUNT_FILE)) {
                setCount(
                    diffBundle.getInt(DIFF_COUNT_FOLDER, 0),
                    diffBundle.getInt(DIFF_COUNT_FILE, 0)
                )
            }
            if (diffBundle.containsKey(DIFF_STAR)) {
                setStar(diffBundle.getBoolean(DIFF_STAR, false))
            }
            setSelected(item.isSelected, false)
        } else {
            item as FolderTag
            setThumbnail(item.thumbnail)
            setName(item.fileName)
            setCount(item.folderCount, item.fileCount)
            setStar(item.isStarred)
            setSelected(item.isSelected, false)
        }
    }

    // setThumbnail
    private fun setThumbnail(thumbnailPath: String) {
        if (thumbnailPath.isNotEmpty()) {
            GlideUtil.loadFolderLayout(thumbnail, thumbnailPath, object : GlideUtil.LoadListener {
                override fun onLoadSuccess(): Boolean {
                    return false
                }

                override fun onLoadFailed(ex: GlideException?, loadPath: String): Boolean {
                    e(ex)
                    return false
                }
            })
        } else {
            GlideApp.with(context).clear(thumbnail)
            thumbnail.setImageResource(R.drawable.ic_folder_white_48dp)
        }
    }

    // setName
    private fun setName(name: String) {
        // Set selected to enable ellipsize marquee effect
        this.name.isSelected = true
        this.name.text = name
    }

    // setCount
    private fun setCount(folderCount: Int, fileCount: Int) {
        val countString = when {
            folderCount != 0 && fileCount != 0 -> {
                val stringBuilder = StringBuilder()
                if (folderCount > 1) {
                    stringBuilder.append("$folderCount ${resources.getString(R.string.folderLayout_count_folders)}, ")
                } else {
                    stringBuilder.append("$folderCount ${resources.getString(R.string.folderLayout_count_folder)}, ")
                }
                if (fileCount > 1) {
                    stringBuilder.append("$fileCount ${resources.getString(R.string.folderLayout_count_files)}")
                } else {
                    stringBuilder.append("$fileCount ${resources.getString(R.string.folderLayout_count_file)}")
                }
                stringBuilder.toString()
            }
            folderCount != 0 -> {
                if (folderCount > 1) {
                    "$folderCount ${resources.getString(R.string.folderLayout_count_folders)}"
                } else {
                    "$folderCount ${resources.getString(R.string.folderLayout_count_folder)}"
                }
            }
            fileCount != 0 -> {
                if (fileCount > 1) {
                    "$fileCount ${resources.getString(R.string.folderLayout_count_files)}"
                } else {
                    "$fileCount ${resources.getString(R.string.folderLayout_count_file)}"
                }
            }
            else -> {
                resources.getString(R.string.folderLayout_count_empty)
            }
        }
        this.count.text = countString
    }

    // setStar
    private fun setStar(isStarred: Boolean) {
        star.isVisible = isStarred
    }

    // setSelected
    override fun setSelected(isSelected: Boolean, withAnimation: Boolean) {
        if (isSelected) {
            tick.isVisible = true
            if (withAnimation && thumbnail.scaleX != 0.8f) {
                thumbnail.animate()
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .setDuration(resources.getInteger(R.integer.animation_duration_shape_change).toLong())
                    .start()
            } else {
                thumbnail.scaleX = 0.8f
                thumbnail.scaleY = 0.8f
            }
        } else {
            tick.isVisible = false
            if (withAnimation && thumbnail.scaleX != 1f) {
                ViewCompat.animate(thumbnail)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .setDuration(resources.getInteger(R.integer.animation_duration_shape_change).toLong())
                    .start()
            } else {
                thumbnail.scaleX = 1f
                thumbnail.scaleY = 1f
            }
        }
    }

    // highlight
    fun highlight() {
        val animation = AlphaAnimation(1f, 0f)
        animation.duration = resources.getInteger(R.integer.animation_duration_detail).toLong()
        animation.interpolator = LinearInterpolator()
        animation.repeatCount = 2
        animation.repeatMode = Animation.REVERSE
        thumbnail.startAnimation(animation)
    }

}