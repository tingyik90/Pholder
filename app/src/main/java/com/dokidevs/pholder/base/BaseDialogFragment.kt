package com.dokidevs.pholder.base

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.dokidevs.dokilog.DokiLog
import com.dokidevs.dokilog.v

/*--- BaseDialogFragment ---*/
abstract class BaseDialogFragment : DialogFragment(), DokiLog {

    /* companion object */
    companion object {

        /* dialog actions */
        const val CLICK_NEUTRAL = 0
        const val CLICK_POSITIVE = 1
        const val CLICK_NEGATIVE = 2
        const val ON_CANCEL = 11
        const val ON_DISMISS = 12

        /* dialog type */
        const val DIALOG_TYPE = "DIALOG_TYPE"

        // getBaseBundle
        fun getBaseBundle(dialogType: Int): Bundle {
            val bundle = Bundle()
            bundle.putInt(DIALOG_TYPE, dialogType)
            return bundle
        }

    }

    /*--- DialogListener ---*/
    interface DialogListener {

        fun onDialogAction(action: Int, dialogFragment: BaseDialogFragment, data: Bundle? = null)

    }

    /* listeners */
    protected var dialogListener: DialogListener? = null

    /* variables */
    var dialogType = -1
        private set

    // getFragmentTag
    open fun getFragmentTag(): String {
        return if (!tag.isNullOrEmpty()) {
            tag!!
        } else {
            // Return class name if not available
            javaClass.simpleName
        }
    }

    // show
    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, getFragmentTag())
    }

    // onAttach
    override fun onAttach(context: Context?) {
        v(getFragmentTag())
        super.onAttach(context)
        // Dialog will only attach to one parent, either parent fragment or activity
        val parent = parentFragment
        dialogListener =
                if (parent != null) {
                    if (parent is DialogListener) {
                        parent
                    } else {
                        throw RuntimeException("$parent must implement ${getFragmentTag()}.DialogListener")
                    }
                } else {
                    if (context is DialogListener) {
                        context
                    } else {
                        throw RuntimeException("$context must implement ${getFragmentTag()}.DialogListener")
                    }
                }
        dialogType = arguments!!.getInt(DIALOG_TYPE, dialogType)
        onAttachAction(context)
    }

    // onAttachAction
    open fun onAttachAction(context: Context?) {}

    // getActivityContext
    protected fun getActivityContext(): Context {
        return context!!
    }

    // getApplicationContext
    protected fun getApplicationContext(): Context {
        return getActivityContext().applicationContext
    }

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        v(getFragmentTag())
        super.onCreate(savedInstanceState)
        onCreateAction(savedInstanceState)
    }

    // onCreateAction
    open fun onCreateAction(savedInstanceState: Bundle?) {}

    // onCreateDialog
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        v(getFragmentTag())
        // Generate default dialog first
        val dialog = super.onCreateDialog(savedInstanceState)
        // Check if postprocessing is required or a new dialog will be made
        val customDialog = onCreateDialogAction(savedInstanceState, dialog)
        // Return default dialog if new customDialog is returned
        return customDialog ?: dialog
    }

    // onCreateDialogAction
    open fun onCreateDialogAction(savedInstanceState: Bundle?, dialog: Dialog): Dialog? {
        // Default returns super.onCreateDialog()
        return dialog
    }

    // onCreateView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        v(getFragmentTag())
        return onCreateViewAction(inflater, container, savedInstanceState)
            ?: super.onCreateView(inflater, container, savedInstanceState)
    }

    // onCreateViewAction
    open fun onCreateViewAction(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Default returns super.onCreateView()
        return null
    }

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
        onDetachAction()
        super.onDetach()
    }

    // onDetachAction
    open fun onDetachAction() {}

    // onCancel
    override fun onCancel(dialog: DialogInterface?) {
        v(getFragmentTag())
        onCancelAction(dialog)
        super.onCancel(dialog)
    }

    // onCancelAction
    open fun onCancelAction(dialog: DialogInterface?) {}

    // onCancel
    override fun onDismiss(dialog: DialogInterface?) {
        v(getFragmentTag())
        onDismissAction(dialog)
        super.onDismiss(dialog)
    }

    // onDismissAction
    open fun onDismissAction(dialog: DialogInterface?) {}

}