package com.dokidevs.pholder.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/*--- BaseRecyclerViewAdapter ---*/
abstract class BaseRecyclerViewAdapter<T : BaseRecyclerViewItem, VH : BaseRecyclerViewHolder<T>> :
    RecyclerView.Adapter<VH>() {

    /* views */
    protected lateinit var recyclerView: RecyclerView
    protected lateinit var layoutManager: RecyclerView.LayoutManager
    protected var viewRenderer: BaseRecyclerViewRenderer<T, VH>? = null

    /* parameters */
    protected val items = mutableListOf<T>()
    // Use HashSet as sequence doesn't matter, ArrayList will cause scroll to lag due to resizing and reordering
    protected val existingViewHolders = HashSet<VH>()

    // onAttachedToRecyclerView
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        setRecyclerViewProperties(recyclerView)
        layoutManager = setLayoutManager(recyclerView)
        recyclerView.layoutManager = layoutManager
        viewRenderer = setViewRenderer(recyclerView)
    }

    // setRecyclerViewProperties
    protected abstract fun setRecyclerViewProperties(recyclerView: RecyclerView)

    // setLayoutManager
    protected abstract fun setLayoutManager(recyclerView: RecyclerView): RecyclerView.LayoutManager

    // setViewRenderer
    protected open fun setViewRenderer(recyclerView: RecyclerView): BaseRecyclerViewRenderer<T, VH>? {
        // Either use viewRenderer or onCreateViewHolderAction
        return null
    }

    // getItemViewType
    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return item.getType()
    }

    // getItemCount
    override fun getItemCount(): Int {
        return items.size
    }

    // getAllItems
    fun getAllItems(): List<T> {
        return items.toList()
    }

    // onCreateViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return if (viewRenderer != null) {
            viewRenderer!!.createViewHolder(parent, viewType)
        } else {
            onCreateViewHolderAction(parent, viewType)
                ?: throw RuntimeException("ViewRenderer and onCreateViewHolderAction are not implemented.")
        }
    }

    // onCreateViewHolderAction
    protected open fun onCreateViewHolderAction(parent: ViewGroup, viewType: Int): VH? {
        // Either use viewRenderer or onCreateViewHolderAction
        return null
    }

    // onBindViewHolder
    override fun onBindViewHolder(holder: VH, position: Int) {
        addExistingViewHolder(holder)
        onBindViewHolderAction(holder, position, null)
    }

    // onBindViewHolder
    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        addExistingViewHolder(holder)
        onBindViewHolderAction(holder, position, payloads)
    }

    // onBindViewHolderAction
    protected open fun onBindViewHolderAction(holder: VH, position: Int, payloads: MutableList<Any>?) {
        val item = items[position]
        holder.setItem(item, payloads)
    }

    // onViewRecycled
    override fun onViewRecycled(holder: VH) {
        removeExistingViewHolder(holder)
        onViewRecycledAction(holder)
    }

    // onViewRecycledAction
    protected open fun onViewRecycledAction(holder: VH) {}

    // addExistingViewHolder
    private fun addExistingViewHolder(holder: VH) {
        existingViewHolders.add(holder)
    }

    // removeExistingViewHolder
    private fun removeExistingViewHolder(holder: VH) {
        existingViewHolders.remove(holder)
    }

    // getViewHolder
    fun getViewHolder(uid: String): VH? {
        val viewHolders = existingViewHolders.toList()
        for (viewHolder in viewHolders) {
            if (viewHolder.getUid() == uid) {
                return viewHolder
            }
        }
        return null
    }

}


/*--- BaseRecyclerViewRenderer ---*/
abstract class BaseRecyclerViewRenderer<T : BaseRecyclerViewItem, VH : BaseRecyclerViewHolder<T>> {

    // createViewHolder
    open fun createViewHolder(parent: ViewGroup, viewType: Int): VH {
        val itemView = LayoutInflater.from(parent.context).inflate(getLayoutResId(viewType), parent, false)
        return getViewHolder(itemView, viewType)
    }

    // getLayoutResId
    abstract fun getLayoutResId(viewType: Int): Int

    // getViewHolder
    abstract fun getViewHolder(itemView: View, viewType: Int): VH

}


/*--- BaseRecyclerViewHolder ---*/
abstract class BaseRecyclerViewHolder<T : BaseRecyclerViewItem>(view: View) :
    RecyclerView.ViewHolder(view),
    BaseRecyclerViewItem,
    BaseAdapterView<T> {

    // setItem
    fun setItem(item: T, payloads: MutableList<Any>?) {
        itemView.tag = item
        bindView(item, payloads)
    }

    // getTag
    @Suppress("UNCHECKED_CAST")
    open fun getTag(): T? {
        return itemView.tag as? T
    }

}

/*--- BaseAdapterView ---*/
interface BaseAdapterView<T : BaseRecyclerViewItem> {

    fun bindView(item: T, payloads: MutableList<Any>?)

}


/*--- BaseRecyclerViewItem ---*/
interface BaseRecyclerViewItem {

    // getType
    fun getType(): Int

    // getUid
    fun getUid(): String

}