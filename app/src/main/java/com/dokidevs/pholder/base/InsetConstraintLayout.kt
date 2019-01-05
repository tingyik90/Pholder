package com.dokidevs.pholder.base

import android.content.Context
import android.util.AttributeSet
import android.view.WindowInsets
import androidx.constraintlayout.widget.ConstraintLayout

/*--- InsetsConstraintLayout ---*/
class InsetsConstraintLayout : ConstraintLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, attributeSetId: Int) : super(context, attrs, attributeSetId)

    // onApplyWindowInsets
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        // Dispatch insets to all children
        val childCount = childCount
        for (index in 0 until childCount)
            getChildAt(index).dispatchApplyWindowInsets(insets)
        return insets
    }

}