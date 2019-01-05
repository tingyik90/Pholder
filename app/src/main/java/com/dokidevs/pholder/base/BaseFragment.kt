package com.dokidevs.pholder.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.e
import com.dokidevs.dokilog.v

/*--- BaseFragment ---*/
abstract class BaseFragment : Fragment(), DokiLog {

    /* companion object */
    companion object {

        /* animation */
        const val MAX_ANIMATION_DURATION = 600L     // Typically 500L for large animation, add 100 for delay.

    }

    /*--- FragmentOnStartListener ---*/
    // Use onStart instead of onAttach.
    // When fragment A is replaced by fragment B, usually onDestroy is not called. Only onDestroyView is called.
    // Hence, then popBackStack is called, fragment A reappear without calling onAttach and onCreate.
    // Using onStart is a better point to check that the fragment is attached and showing.
    interface FragmentOnStartListener {

        // fragmentOnStart
        fun fragmentOnStart(tag: String, fragment: BaseFragment)

    }

    /*--- ChildFragmentOnStartListener ---*/
    interface ChildFragmentOnStartListener {

        // childFragmentOnStart
        fun childFragmentOnStart(tag: String, fragment: BaseFragment)

    }

    /* listeners */
    protected var fragmentOnStartListener: FragmentOnStartListener? = null
    protected var childFragmentOnStartListener: ChildFragmentOnStartListener? = null

    // getFragmentTag
    open fun getFragmentTag(): String {
        return if (!tag.isNullOrEmpty()) {
            tag!!
        } else {
            // Return class name if not available
            javaClass.simpleName
        }
    }

    // onAttach
    @Suppress("UNCHECKED_CAST")
    override fun onAttach(context: Context?) {
        v(getFragmentTag())
        super.onAttach(context)
        if (context is FragmentOnStartListener) {
            fragmentOnStartListener = context
        } else {
            throw RuntimeException("$context must implement ${getFragmentTag()}.FragmentOnStartListener")
        }
        val parent = parentFragment
        if (parent != null) {
            if (parent is ChildFragmentOnStartListener) {
                childFragmentOnStartListener = parent
            } else {
                throw RuntimeException("$parent must implement ${getFragmentTag()}.ChildFragmentOnStartListener")
            }
        }
        onAttachAction(context)
    }

    // onAttachAction
    open fun onAttachAction(context: Context?) {}

    // getApplicationContext
    protected fun getApplicationContext(): Context? {
        return context?.applicationContext
    }

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        v(getFragmentTag())
        super.onCreate(savedInstanceState)
        onCreateAction(savedInstanceState)
    }

    // onCreateAnimation
    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        // This is added to solve childFragment being dismissed before parentFragment transition animation
        // See https://stackoverflow.com/a/23276145/3584439
        val parent = parentFragment
        return if (!enter && parent != null && parent.isRemoving) {
            val doNothingAnim = AlphaAnimation(1f, 1f)
            doNothingAnim.duration = getNextAnimationDuration(parent, MAX_ANIMATION_DURATION)
            doNothingAnim
        } else {
            super.onCreateAnimation(transit, enter, nextAnim)
        }
    }

    // getNextAnimationDuration
    private fun getNextAnimationDuration(fragment: Fragment, defValue: Long): Long {
        try {
            val animInfoField = fragment.javaClass.getDeclaredField("mAnimationInfo")
            animInfoField.isAccessible = true
            val animationInfo = animInfoField.get(fragment)
            if (animationInfo != null) {
                val nextAnimField = animationInfo.javaClass.getDeclaredField("mNextAnim")
                nextAnimField.isAccessible = true
                val nextAnimResource = nextAnimField.getInt(animationInfo)
                val nextAnim = AnimationUtils.loadAnimation(fragment.activity, nextAnimResource)
                return nextAnim?.duration ?: defValue
            }
            return defValue
        } catch (ex: Exception) {
            e(ex)
            return defValue
        }
    }

    // onCreateAction
    open fun onCreateAction(savedInstanceState: Bundle?) {}

    // onCreateView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        v(getFragmentTag())
        return onCreateViewAction(inflater, container, savedInstanceState)
    }

    // onCreateViewAction
    abstract fun onCreateViewAction(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?

    // onViewCreated
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        v(getFragmentTag())
        super.onViewCreated(view, savedInstanceState)
        onViewCreatedAction(view, savedInstanceState)
    }

    // onViewCreatedAction
    open fun onViewCreatedAction(view: View, savedInstanceState: Bundle?) {}

    // onStart
    override fun onStart() {
        v(getFragmentTag())
        super.onStart()
        fragmentOnStartListener?.fragmentOnStart(getFragmentTag(), this)
        childFragmentOnStartListener?.childFragmentOnStart(getFragmentTag(), this)
        onStartAction()
    }

    // onStartAction
    open fun onStartAction() {}

    // onResume
    override fun onResume() {
        v(getFragmentTag())
        super.onResume()
        onResumeAction()
    }

    // onResumeAction
    open fun onResumeAction() {}

    // onPause
    override fun onPause() {
        v(getFragmentTag())
        onPauseAction()
        super.onPause()
    }

    // onPauseAction
    open fun onPauseAction() {}

    // onStop
    override fun onStop() {
        v(getFragmentTag())
        onStopAction()
        super.onStop()
    }

    // onStopAction
    open fun onStopAction() {}

    // onDestroyView
    override fun onDestroyView() {
        v(getFragmentTag())
        onDestroyViewAction()
        super.onDestroyView()
    }

    // onDestroyViewAction
    open fun onDestroyViewAction() {}

    // onDestroy
    override fun onDestroy() {
        v(getFragmentTag())
        onDestroyAction()
        super.onDestroy()
    }

    // onDestroyAction
    open fun onDestroyAction() {}

    // onDetach
    override fun onDetach() {
        v(getFragmentTag())
        fragmentOnStartListener = null
        childFragmentOnStartListener = null
        onDetachAction()
        super.onDetach()
    }

    // onDetachAction
    open fun onDetachAction() {}

    // onBackPressed
    open fun onBackPressed(): Boolean {
        // Don't consume back press by default
        return false
    }

}


/*

// Template for subclass

// onAttachAction
open fun onAttachAction(context: Context?) {}

// onCreateAction
open fun onCreateAction(savedInstanceState: Bundle?) {}

// onCreateViewAction
abstract fun onCreateViewAction(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?

// onViewCreatedAction
open fun onViewCreatedAction(view: View, savedInstanceState: Bundle?) {}

// onStartAction
open fun onStartAction() {}

// onResumeAction
open fun onResumeAction() {}

// onPauseAction
open fun onPauseAction() {}

// onStopAction
open fun onStopAction() {}

// onDestroyViewAction
open fun onDestroyViewAction() {}

// onDestroyAction
open fun onDestroyAction() {}

// onDetachAction
open fun onDetachAction() {}

// onBackPressed
open fun onBackPressed(): Boolean {
    return false
}

*/