package com.dokidevs.pholder.gallery.layout

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.dokidevs.pholder.PholderApplication.Companion.isDarkTheme
import com.dokidevs.pholder.R
import com.dokidevs.pholder.data.PholderTag
import com.dokidevs.pholder.data.TitleTag
import com.dokidevs.pholder.data.TitleTag.Companion.CENTER
import com.dokidevs.pholder.data.TitleTag.Companion.LEFT
import com.dokidevs.pholder.utils.ColorUtils.Companion.colorTransparent
import kotlinx.android.synthetic.main.layout_title.view.*

/*--- TitleLayout ---*/
class TitleLayout : ConstraintLayout, GalleryAdapterView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, attributeSetId: Int) : super(context, attrs, attributeSetId)


    /* views */
    private val main by lazy { titleLayout_main }
    private val title by lazy { titleLayout_title }
    private val tickBox by lazy { titleLayout_tickBox }

    // setItem
    override fun bindView(item: PholderTag, payloads: MutableList<Any>?) {
        main.setBackgroundColor(colorTransparent)
        item as TitleTag
        setTitle(item.title, item.alignment)
        setSelected(item.isSelected, false)
        showTickBox(item.showTickBox)
    }

    // setTitle
    @SuppressLint("RtlHardcoded")
    private fun setTitle(title: String, alignment: Int) {
        when (alignment) {
            CENTER -> this.title.gravity = Gravity.CENTER
            LEFT -> this.title.gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
        }
        this.title.text = title
    }

    // setSelected
    override fun setSelected(isSelected: Boolean, withAnimation: Boolean) {
        if (!isSelected) {
            if (isDarkTheme) {
                tickBox.setBackgroundResource(R.drawable.bg_tick_circle_empty_white)
            } else {
                tickBox.setBackgroundResource(R.drawable.bg_tick_circle_empty_black)
            }
            tickBox.setImageResource(android.R.color.transparent)
        } else {
            tickBox.setBackgroundResource(R.drawable.bg_tick_circle_filled_white)
            tickBox.setImageResource(R.drawable.ic_tick_white_24dp)
        }
    }

    // showTickBox
    fun showTickBox(show: Boolean) {
        tickBox.isVisible = show
    }

}