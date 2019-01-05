package com.dokidevs.pholder.dialog.folderpicker

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.e
import com.dokidevs.pholder.utils.ColorUtils.Companion.colorAccent
import com.dokidevs.pholder.utils.ColorUtils.Companion.colorTransparent
import com.dokidevs.pholder.utils.GlideApp
import kotlinx.android.synthetic.main.layout_item_folder_picker.view.*

/*--- FolderPickerItemLayout ---*/
class FolderPickerItemLayout : ConstraintLayout, DokiLog {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, attributeSetId: Int) : super(context, attrs, attributeSetId)

    /* views */
    private val name by lazy { folderPickerItemLayout_name }
    private val count by lazy { folderPickerItemLayout_count }
    private val icon by lazy { folderPickerItemLayout_icon }
    private val next by lazy { folderPickerItemLayout_next }

    // setName
    fun setName(name: String) {
        this.name.text = name
    }

    // setCount
    fun setCount(count: Int) {
        val itemString = if (count > 1) {
            "folders"
        } else {
            "folder"
        }
        val countString = "$count $itemString"
        this.count.text = countString
    }

    // setThumbnail
    fun setThumbnail(filePath: String) {
        if (filePath.isNotEmpty()) {
            icon.isVisible = true
            GlideApp.with(context)
                .load(filePath)
                .centerCrop()
                .dontAnimate()
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onLoadFailed(
                        ex: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        e(ex)
                        icon.isVisible = false
                        return false
                    }
                })
                .into(icon)
        } else {
            icon.isVisible = false
        }
    }

    // setIsSelected
    fun setIsSelected(isSelected: Boolean) {
        if (isSelected) {
            setBackgroundColor(colorAccent)
        } else {
            setBackgroundColor(colorTransparent)
        }
    }

    // getNextImage
    fun getNextImage(): ImageView {
        return next
    }

}