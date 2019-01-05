package com.dokidevs.pholder.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.v

/*--- BaseActivity ---*/
abstract class BaseActivity : AppCompatActivity(), DokiLog {

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        v()
        onCreatePreAction(savedInstanceState)
        super.onCreate(savedInstanceState)
        onCreateAction(savedInstanceState)
    }

    // onCreatePreAction
    open fun onCreatePreAction(savedInstanceState: Bundle?) {}

    // onCreateAction
    open fun onCreateAction(savedInstanceState: Bundle?) {}

    // onStart
    override fun onStart() {
        v()
        super.onStart()
        onStartAction()
    }

    // onStartAction
    open fun onStartAction() {}

    // onResume
    override fun onResume() {
        v()
        super.onResume()
        onResumeAction()
    }

    // onResumeAction
    open fun onResumeAction() {}

    // finishAfterTransition
    override fun finishAfterTransition() {
        v()
        finishAfterTransitionAction()
        super.finishAfterTransition()
    }

    // finishAfterTransitionAction
    open fun finishAfterTransitionAction() {}

    // finish
    override fun finish() {
        v()
        onFinishAction()
        super.finish()
    }

    // onFinishAction
    open fun onFinishAction() {}

    // onPause
    override fun onPause() {
        v()
        onPauseAction()
        super.onPause()
    }

    // onPauseAction
    open fun onPauseAction() {}

    // onStop
    override fun onStop() {
        v()
        onStopAction()
        super.onStop()
    }

    // onStopAction
    open fun onStopAction() {}

    // onDestroy
    override fun onDestroy() {
        v()
        onDestroyAction()
        super.onDestroy()
    }

    // onDestroyAction
    open fun onDestroyAction() {}

}


/*

// Template for subclass

// onCreatePreAction
open fun onCreatePreAction(savedInstanceState: Bundle?) {}

// onCreateAction
open fun onCreateAction(savedInstanceState: Bundle?) {}

// onStartAction
open fun onStartAction() {}

// onResumeAction
open fun onResumeAction() {}

// finishAfterTransitionAction
open fun finishAfterTransitionAction() {}

// onFinishAction
open fun onFinishAction() {}

// onPauseAction
open fun onPauseAction() {}

// onStopAction
open fun onStopAction() {}

// onDestroyAction
open fun onDestroyAction() {}

*/