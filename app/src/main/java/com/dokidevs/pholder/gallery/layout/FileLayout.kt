package com.dokidevs.pholder.gallery.layout

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.bumptech.glide.load.engine.GlideException
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.e
import com.dokidevs.pholder.R
import com.dokidevs.pholder.data.FileTag
import com.dokidevs.pholder.data.PholderTag
import com.dokidevs.pholder.data.PholderTagUtil
import com.dokidevs.pholder.slideshow.SlideshowActivity
import com.dokidevs.pholder.utils.GlideUtil
import kotlinx.android.synthetic.main.layout_file.view.*

/*--- FileLayout ---*/
class FileLayout :
    ConstraintLayout,
    GalleryAdapterView,
    SlideshowActivity.SlideshowTransitionInterface,
    DokiLog {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, attributeSetId: Int) : super(context, attrs, attributeSetId)

    /*--- FileLayoutListener ---*/
    interface FileLayoutListener {

        // onThumbnailLoadCompleted
        fun onThumbnailLoadComplete()

    }

    /* listeners */
    private var fileLayoutListener: FileLayoutListener? = null

    // setFileLayoutListener
    fun setFileLayoutListener(fileLayoutListener: FileLayoutListener) {
        this.fileLayoutListener = fileLayoutListener
    }

    /* views */
    private val thumbnail by lazy { fileLayout_thumbnail }
    private val tick by lazy { fileLayout_tick }
    private val bottomGradient by lazy { fileLayout_gradient_bottom }
    private val play by lazy { fileLayout_play }
    private val time by lazy { fileLayout_time }

    /* parameters */
    private var isThumbnailLoadComplete = false

    // setItem
    override fun bindView(item: PholderTag, payloads: MutableList<Any>?) {
        item as FileTag
        setThumbnail(item)
        setSelected(item.isSelected, false)
    }

    // setThumbnail
    @SuppressLint("CheckResult")
    private fun setThumbnail(fileTag: FileTag) {
        isThumbnailLoadComplete = false
        thumbnail.transitionName = fileTag.getFilePath()
        GlideUtil.loadFileLayout(thumbnail, fileTag, object : GlideUtil.LoadListener {
            override fun onLoadSuccess(): Boolean {
                isThumbnailLoadComplete = true
                fileLayoutListener?.onThumbnailLoadComplete()
                return false
            }

            override fun onLoadFailed(ex: GlideException?, loadPath: String): Boolean {
                e(ex)
                isThumbnailLoadComplete = true
                fileLayoutListener?.onThumbnailLoadComplete()
                return false
            }
        })
        val isVideo = fileTag.isVideo()
        bottomGradient.isVisible = isVideo
        play.isVisible = isVideo
        time.isVisible = isVideo
        time.text = PholderTagUtil.videoMillisToDuration(fileTag.duration)
        // Ensure they show in case the alpha is retained as 0f during slideshow
        time.alpha = 1f
        bottomGradient.alpha = 1f
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
                thumbnail.animate()
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

    // onExitPreparation
    override fun onExitPreparation() {
        // Hide time and bottom gradient
        time.alpha = 0f
        bottomGradient.alpha = 0f
    }

    // onExitRequiredSharedElements
    override fun onExitRequiredSharedElements(): List<View> {
        return listOf(thumbnail, play)
    }

    // onReenterPreparation
    override fun onReenterPreparation() {
        // Show time and bottom gradient
        time.alpha = 1f
        bottomGradient.alpha = 1f
    }

    // onReenterRequiredSharedElements
    override fun onReenterRequiredSharedElements(): List<View> {
        return listOf(thumbnail, play, bottomGradient, time)
    }

    // isThumbnailLoadComplete
    override fun isThumbnailLoadComplete(): Boolean {
        return isThumbnailLoadComplete
    }

}