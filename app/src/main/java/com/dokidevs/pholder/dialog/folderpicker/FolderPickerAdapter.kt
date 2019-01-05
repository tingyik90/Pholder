package com.dokidevs.pholder.dialog.folderpicker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseRecyclerViewAdapter
import com.dokidevs.pholder.base.BaseRecyclerViewHolder
import com.dokidevs.pholder.data.FolderTag

/*--- FolderPickerAdapter ---*/
class FolderPickerAdapter(
    private val folderPickerAdapterListener: FolderPickerAdapterListener
) : BaseRecyclerViewAdapter<FolderTag, FolderPickerAdapter.ViewHolderFolder>() {

    /*--- FolderPickerAdapterListener ---*/
    interface FolderPickerAdapterListener {

        // onItemClick
        fun onItemClick(folderTag: FolderTag)

        // onNextClick
        fun onNextClick(folderTag: FolderTag)

    }

    /* parameters */
    private var selectedTag: FolderTag? = null

    // updateFolderTags
    fun updateFolderTags(folderTags: List<FolderTag>) {
        items.clear()
        items.addAll(folderTags)
        // Replicate current selection into new folderTags
        val _selectedTag = selectedTag
        if (_selectedTag != null) {
            folderTags.forEach { folderTag ->
                if (folderTag == _selectedTag) {
                    folderTag.isSelected = _selectedTag.isSelected
                }
            }
        }
        selectedTag = null
        notifyDataSetChanged()
    }

    // getSelectedTag
    fun getSelectedTag(): FolderTag? {
        return selectedTag
    }

    // setSelectedTag
    fun setSelectedTag(selectedTag: FolderTag?) {
        this.selectedTag = selectedTag
    }

    // updateSelection
    fun updateSelection(folderTag: FolderTag) {
        existingViewHolders.forEach { viewHolder ->
            val existingFolderTag = viewHolder.itemView.tag as FolderTag
            if (existingFolderTag == folderTag) {
                viewHolder.setIsSelected(folderTag.isSelected)
                return
            }
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
    override fun onCreateViewHolderAction(parent: ViewGroup, viewType: Int): ViewHolderFolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.layout_item_folder_picker, parent, false)
        return ViewHolderFolder(itemView, folderPickerAdapterListener)
    }


    /* ViewHolderFolder */
    class ViewHolderFolder(
        view: View,
        private val folderPickerAdapterListener: FolderPickerAdapterListener
    ) : BaseRecyclerViewHolder<FolderTag>(view) {

        /* init */
        init {
            view as FolderPickerItemLayout
            view.setOnClickListener {
                val folderTag = view.tag as FolderTag
                folderPickerAdapterListener.onItemClick(folderTag)
            }
            val next = view.getNextImage()
            next.setOnClickListener {
                val folderTag = view.tag as FolderTag
                folderPickerAdapterListener.onNextClick(folderTag)
            }
        }

        // getType
        override fun getType(): Int {
            return 0
        }

        // getUid
        override fun getUid(): String {
            return ""
        }

        /* parameters */
        private val fileListLayout by lazy { view as FolderPickerItemLayout }

        // bindView
        override fun bindView(item: FolderTag, payloads: MutableList<Any>?) {
            fileListLayout.setName(item.fileName)
            fileListLayout.setCount(item.folderCount)
            fileListLayout.setThumbnail(item.thumbnail)
            fileListLayout.setIsSelected(item.isSelected)
        }

        // setIsSelected
        fun setIsSelected(isSelected: Boolean) {
            fileListLayout.setIsSelected(isSelected)
        }

    }

}