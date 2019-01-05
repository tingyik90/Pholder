package com.dokidevs.pholder.data

import android.annotation.SuppressLint
import java.util.*

class TitleTag(val title: String, val alignment: Int) : PholderTag() {

    /* companion object */
    companion object {

        /* alignment */
        const val CENTER = 0
        const val LEFT = 1

        // newDate
        @SuppressLint("SimpleDateFormat")
        fun newDate(millis: Long): TitleTag {
            return TitleTag(PholderTagUtil.millisToDate(millis), CENTER)
        }

        fun newTitle(title: String): TitleTag {
            return TitleTag(title, LEFT)
        }

    }

    /* parameters */
    var showTickBox = false

    // getType
    override fun getType(): Int {
        return TYPE_TITLE
    }

    // getUid
    override fun getUid(): String {
        return title
    }

    // getAlignmentString
    private fun getAlignmentString(): String {
        return when (alignment) {
            CENTER -> "CENTER"
            LEFT -> "LEFT"
            else -> "NOT IMPLEMENTED"
        }
    }

    // toString
    override fun toString(): String {
        return "TitleTag(" +
                "title='$title', " +
                "alignment='${getAlignmentString()}', " +
                "showTickBox=$showTickBox)"
    }

    // equals
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TitleTag
        if (title != other.title) return false
        if (alignment != other.alignment) return false
        return true
    }

    // hashCode
    override fun hashCode(): Int {
        return Objects.hash(title, alignment)
    }

}