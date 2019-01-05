package com.dokidevs.pholder.base

import android.transition.Transition
import androidx.annotation.CallSuper

/*--- BaseTransitionListener ---*/
// Class created to simplify call of TransitionListener and enforce certain methods
abstract class BaseTransitionListener : Transition.TransitionListener {

    // onTransitionStart
    override fun onTransitionStart(transition: Transition) {}

    // onTransitionPause
    override fun onTransitionPause(transition: Transition) {}

    // onTransitionResume
    override fun onTransitionResume(transition: Transition) {}

    // onTransitionEnd
    @CallSuper
    override fun onTransitionEnd(transition: Transition) {
        transition.removeListener(this)
        postRemoveListener(transition)
    }

    // onTransitionCancel
    @CallSuper
    override fun onTransitionCancel(transition: Transition) {
        transition.removeListener(this)
        postRemoveListener(transition)
    }

    // removeListener
    abstract fun postRemoveListener(transition: Transition)

}