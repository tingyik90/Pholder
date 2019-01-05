package com.dokidevs.pholder.gallery.layout

import android.view.View
import com.dokidevs.pholder.R
import com.dokidevs.pholder.base.BaseAdapterView
import com.dokidevs.pholder.base.BaseRecyclerViewHolder
import com.dokidevs.pholder.base.BaseRecyclerViewRenderer
import com.dokidevs.pholder.data.PholderTag
import com.dokidevs.pholder.data.PholderTag.Companion.TYPE_FILE
import com.dokidevs.pholder.data.PholderTag.Companion.TYPE_FOLDER

/*--- GridViewRenderer ---*/
class GridViewRenderer(
    private val galleryAdapterListener: GalleryAdapter.GalleryAdapterListener
) : BaseRecyclerViewRenderer<PholderTag, GalleryBaseViewHolder>() {

    // getLayoutResId
    override fun getLayoutResId(viewType: Int): Int {
        return when (viewType) {
            TYPE_FILE -> R.layout.layout_file
            TYPE_FOLDER -> R.layout.layout_folder
            PholderTag.TYPE_TITLE -> R.layout.layout_title
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
    }

    // getViewHolder
    override fun getViewHolder(itemView: View, viewType: Int): GalleryBaseViewHolder {
        return when (viewType) {
            TYPE_FILE -> FileViewHolder(itemView, galleryAdapterListener)
            TYPE_FOLDER -> FolderViewHolder(itemView, galleryAdapterListener)
            PholderTag.TYPE_TITLE -> TitleViewHolder(itemView, galleryAdapterListener)
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
    }

}

/*--- ListViewRenderer ---*/
class ListViewRenderer(
    private val galleryAdapterListener: GalleryAdapter.GalleryAdapterListener
) : BaseRecyclerViewRenderer<PholderTag, GalleryBaseViewHolder>() {

    // getLayoutResId
    override fun getLayoutResId(viewType: Int): Int {
        return when (viewType) {
            TYPE_FILE -> R.layout.layout_file_list
            TYPE_FOLDER -> R.layout.layout_folder_list
            PholderTag.TYPE_TITLE -> R.layout.layout_title
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
    }

    // getViewHolder
    override fun getViewHolder(itemView: View, viewType: Int): GalleryBaseViewHolder {
        return when (viewType) {
            TYPE_FILE -> FileListViewHolder(itemView, galleryAdapterListener)
            TYPE_FOLDER -> FolderListViewHolder(itemView, galleryAdapterListener)
            PholderTag.TYPE_TITLE -> TitleViewHolder(itemView, galleryAdapterListener)
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
    }

}

/*--- GalleryBaseViewHolder ---*/
abstract class GalleryBaseViewHolder(
    view: View,
    protected val galleryAdapterListener: GalleryAdapter.GalleryAdapterListener
) : BaseRecyclerViewHolder<PholderTag>(view) {

    // getUid
    override fun getUid(): String {
        return getTag()?.getUid() ?: ""
    }

    // bindView
    override fun bindView(item: PholderTag, payloads: MutableList<Any>?) {
        if (itemView is GalleryAdapterView) {
            itemView.bindView(item, payloads)
        } else {
            throw RuntimeException("This view must implement GalleryAdapterView")
        }
    }

    // setSelected
    fun setSelected(isSelected: Boolean, withAnimation: Boolean) {
        getTag()!!.isSelected = isSelected
        (itemView as GalleryAdapterView).setSelected(isSelected, withAnimation)
    }

}

/*--- FileViewHolder ---*/
class FileViewHolder(
    view: View,
    galleryAdapterListener: GalleryAdapter.GalleryAdapterListener
) : GalleryBaseViewHolder(view, galleryAdapterListener), FileLayout.FileLayoutListener {

    /* init */
    init {
        view as FileLayout
        view.setFileLayoutListener(this)
        view.setOnClickListener {
            galleryAdapterListener.onFileClick(view, adapterPosition)
        }
        view.setOnLongClickListener {
            galleryAdapterListener.onItemLongClick(view, adapterPosition)
        }
    }

    // getType
    override fun getType(): Int {
        return TYPE_FILE
    }

    // onThumbnailLoadCompleted
    override fun onThumbnailLoadComplete() {
        galleryAdapterListener.onThumbnailLoadCompleted(itemView, getUid())
    }

}

/*--- FileListViewHolder ---*/
class FileListViewHolder(
    view: View,
    galleryAdapterListener: GalleryAdapter.GalleryAdapterListener
) : GalleryBaseViewHolder(view, galleryAdapterListener), FileListLayout.FileListLayoutListener {

    /* init */
    init {
        view as FileListLayout
        view.setFileListLayoutListener(this)
        view.setOnClickListener {
            galleryAdapterListener.onFileClick(view, adapterPosition)
        }
        view.setOnLongClickListener {
            galleryAdapterListener.onItemLongClick(view, adapterPosition)
        }
    }

    // getType
    override fun getType(): Int {
        return TYPE_FILE
    }

    // onThumbnailLoadCompleted
    override fun onThumbnailLoadComplete() {
        galleryAdapterListener.onThumbnailLoadCompleted(itemView, getUid())
    }

}


/*--- FolderViewHolder ---*/
class FolderViewHolder(
    view: View,
    galleryAdapterListener: GalleryAdapter.GalleryAdapterListener
) : GalleryBaseViewHolder(view, galleryAdapterListener) {

    /* init */
    init {
        view as FolderLayout
        view.setOnClickListener {
            galleryAdapterListener.onFolderClick(view, adapterPosition)
        }
        view.setOnLongClickListener {
            galleryAdapterListener.onItemLongClick(view, adapterPosition)
        }
    }

    // getType
    override fun getType(): Int {
        return TYPE_FOLDER
    }

}


/*--- FolderListViewHolder ---*/
class FolderListViewHolder(
    view: View,
    galleryAdapterListener: GalleryAdapter.GalleryAdapterListener
) : GalleryBaseViewHolder(view, galleryAdapterListener) {

    /* init */
    init {
        view as FolderListLayout
        view.setOnClickListener {
            galleryAdapterListener.onFolderClick(view, adapterPosition)
        }
        view.setOnLongClickListener {
            galleryAdapterListener.onItemLongClick(view, adapterPosition)
        }
    }

    // getType
    override fun getType(): Int {
        return TYPE_FOLDER
    }

}


/*--- TitleViewHolder ---*/
class TitleViewHolder(
    view: View,
    galleryAdapterListener: GalleryAdapter.GalleryAdapterListener
) : GalleryBaseViewHolder(view, galleryAdapterListener) {

    /* init */
    init {
        view.setOnClickListener {
            galleryAdapterListener.onTitleClick(view, adapterPosition)
        }
    }

    // getType
    override fun getType(): Int {
        return PholderTag.TYPE_TITLE
    }

    // showTickBox
    fun showTickBox(show: Boolean) {
        (itemView as TitleLayout).showTickBox(show)
    }

}


/*--- GalleryAdapterView ---*/
interface GalleryAdapterView : BaseAdapterView<PholderTag> {

    // setSelected
    fun setSelected(isSelected: Boolean, withAnimation: Boolean)

}