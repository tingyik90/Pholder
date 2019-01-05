package com.dokidevs.pholder.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseRecyclerViewAdapter
import com.dokidevs.pholder.base.BaseRecyclerViewHolder
import com.dokidevs.pholder.base.BaseRecyclerViewItem
import java.util.*

/*--- InfoListAdapter ---*/
class InfoListAdapter : BaseRecyclerViewAdapter<InfoListAdapter.InfoSet, InfoListAdapter.ViewHolderInfo>() {

    // addInfo
    fun addInfo(infoSet: InfoSet, notifyInsert: Boolean = false) {
        items.add(infoSet)
        if (notifyInsert) {
            notifyItemInserted(items.size - 1)
        }
    }

    // setRecyclerViewProperties
    override fun setRecyclerViewProperties(recyclerView: RecyclerView) {
        // Do nothing
    }

    // setLayoutManager
    override fun setLayoutManager(recyclerView: RecyclerView): RecyclerView.LayoutManager {
        return LinearLayoutManager(recyclerView.context, RecyclerView.VERTICAL, false)
    }

    // onCreateViewHolderAction
    override fun onCreateViewHolderAction(parent: ViewGroup, viewType: Int): ViewHolderInfo {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.layout_item_info, parent, false)
        return ViewHolderInfo(itemView)
    }


    /*--- ViewHolderInfo ---*/
    class ViewHolderInfo(view: View) : BaseRecyclerViewHolder<InfoSet>(view) {

        override fun getType(): Int {
            return getTag()?.getType() ?: 0
        }

        override fun getUid(): String {
            return getTag()?.getUid() ?: ""
        }

        /* views */
        private var icon = view.findViewById<ImageView>(R.id.infoItemLayout_icon)
        private var textPrimary = view.findViewById<TextView>(R.id.infoItemLayout_text_primary)
        private var textSecondary = view.findViewById<TextView>(R.id.infoItemLayout_text_secondary)

        override fun bindView(item: InfoSet, payloads: MutableList<Any>?) {
            icon.setImageResource(item.resId)
            textPrimary.text = item.textPrimary
            textSecondary.text = item.textSecondary
            textSecondary.isVisible = item.textSecondary.isNotEmpty()
        }

    }


    /*--- InfoSet ---*/
    class InfoSet(var resId: Int = 0, var textPrimary: String = "", var textSecondary: String = "") :
        BaseRecyclerViewItem {

        // getType
        override fun getType(): Int {
            return 0
        }

        // getUid
        override fun getUid(): String {
            return textPrimary + textSecondary
        }

        // toString
        override fun toString(): String {
            return "InfoSet(resId=$resId, textPrimary='$textPrimary', textSecondary='$textSecondary')"
        }

        // equals
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as InfoSet
            if (resId != other.resId) return false
            if (textPrimary != other.textPrimary) return false
            if (textSecondary != other.textSecondary) return false
            return true
        }

        // hashCode
        override fun hashCode(): Int {
            return Objects.hash(resId, textPrimary, textSecondary)
        }

    }

}