package com.dokidevs.pholder.data

import androidx.annotation.Keep
import androidx.room.Ignore
import com.dokidevs.pholder.base.BaseRecyclerViewItem

/*--- PholderTag ---*/
@Keep
abstract class PholderTag : BaseRecyclerViewItem {

    /* companion object */
    companion object {

        /* type */
        const val TYPE_EMPTY = 0
        const val TYPE_TITLE = 100
        const val TYPE_FOLDER = 200
        const val TYPE_FILE = 300

    }

    /* parameter */
    @Ignore
    var isSelected = false

}