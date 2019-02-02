package com.dokidevs.pholder.base

import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver

/*--- BaseResultReceiver ---*/
class BaseResultReceiver(
    handler: Handler,
    private var resultReceiverListener: ResultReceiverListener? = null
) : ResultReceiver(handler) {

    /*--- FragmentOnStartListener ---*/
    interface ResultReceiverListener {

        // onReceiveResult
        fun onReceiveResult(resultCode: Int, resultData: Bundle)

    }

    // onReceiveResult
    override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
        resultReceiverListener?.onReceiveResult(resultCode, resultData)
    }

    // cancelReceiver
    fun cancelReceiver() {
        resultReceiverListener = null
    }

}