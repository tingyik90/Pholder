package com.dokidevs.pholder.base

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/*--- LockableViewPager ---*/
class LockableViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {

    /* parameters */
    var canSwipe = true

    // onTouchEvent
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (canSwipe) {
            super.onTouchEvent(event)
        } else {
            false
        }
    }

    // onInterceptTouchEvent
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return if (canSwipe) {
            super.onInterceptTouchEvent(event)
        } else {
            false
        }
    }

    // onSizeChanged
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Size changes don't play nicely with ViewPagers that have a non-zero margin, which makes the page run away from center.
        // See https://stackoverflow.com/a/21740950/3584439
        super.onSizeChanged(w - pageMargin, h, oldw - pageMargin, oldh)
    }

}