package com.dokidevs.pholder.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseRecyclerViewAdapter
import com.dokidevs.pholder.base.BaseRecyclerViewHolder
import com.dokidevs.pholder.base.BaseRecyclerViewItem

/*--- DirectoryListAdapter ---*/
class DirectoryListAdapter(
    private val directoryListAdapterListener: DirectoryListAdapterListener
) : BaseRecyclerViewAdapter<DirectoryListAdapter.DirectoryListItem, DirectoryListAdapter.ViewHolderPath>() {

    /*--- DirectoryListAdapterListener ---*/
    interface DirectoryListAdapterListener {

        // onDeleteClick
        fun onDeleteClick(filePath: String)

    }

    // setFilePaths
    fun setFilePaths(filePaths: List<String>) {
        val newItems = filePaths.map { DirectoryListItem(it) }
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // addFilePath
    fun addFilePath(filePath: String) {
        val newItem = DirectoryListItem(filePath)
        if (!items.contains(newItem)) {
            items.add(newItem)
            items.sortBy { it.filePath }
            val position = items.indexOf(newItem)
            notifyItemInserted(position)
        }
    }

    // deleteFilePath
    fun deleteFilePath(filePath: String) {
        val item = DirectoryListItem(filePath)
        val position = items.indexOf(item)
        items.remove(item)
        notifyItemRemoved(position)
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
    override fun onCreateViewHolderAction(parent: ViewGroup, viewType: Int): DirectoryListAdapter.ViewHolderPath {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.layout_item_directory_list, parent, false)
        return ViewHolderPath(itemView, directoryListAdapterListener)
    }


    /*--- ViewHolderPath ---*/
    class ViewHolderPath(
        view: View,
        private val directoryListAdapterListener: DirectoryListAdapterListener
    ) : BaseRecyclerViewHolder<DirectoryListItem>(view) {

        /* views */
        private var path = view.findViewById<TextView>(R.id.directoryListItemLayout_path)
        private var delete = view.findViewById<ImageView>(R.id.directoryListItemLayout_delete)

        /* init */
        init {
            delete.setOnClickListener {
                directoryListAdapterListener.onDeleteClick(getUid())
            }
        }

        // getType
        override fun getType(): Int {
            return getTag()?.getType() ?: 0
        }

        // getUid
        override fun getUid(): String {
            return getTag()?.getUid() ?: ""
        }

        // bindView
        override fun bindView(item: DirectoryListItem, payloads: MutableList<Any>?) {
            path.text = item.filePath
        }

    }

    /*--- DirectoryListItem ---*/
    class DirectoryListItem(var filePath: String) : BaseRecyclerViewItem {

        // getType
        override fun getType(): Int {
            return 0
        }

        // getUid
        override fun getUid(): String {
            return filePath
        }

        // toString
        override fun toString(): String {
            return "DirectoryListItem(filePath='$filePath')"
        }

        // equals
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as DirectoryListItem
            if (filePath != other.filePath) return false
            return true
        }

        // hashCode
        override fun hashCode(): Int {
            return filePath.hashCode()
        }

    }

}