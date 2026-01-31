package com.chickenyoung.moreclip

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class IMETemplateAdapter(
    private var items: List<TemplateItem>,
    private val onFolderClick: (String) -> Unit,
    private val onTemplateClick: (MemoEntity) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_FOLDER = 0
        private const val TYPE_TEMPLATE = 1
    }

    class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textName)
        val textCount: TextView = view.findViewById(R.id.textCount)
    }

    class TemplateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textContent: TextView = view.findViewById(R.id.textContent)
        val textFolder: TextView = view.findViewById(R.id.textFolder)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TemplateItem.Folder -> TYPE_FOLDER
            is TemplateItem.Template -> TYPE_TEMPLATE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_FOLDER -> FolderViewHolder(inflater.inflate(R.layout.item_ime_folder, parent, false))
            else -> TemplateViewHolder(inflater.inflate(R.layout.item_ime_template, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TemplateItem.Folder -> (holder as FolderViewHolder).bind(item)
            is TemplateItem.Template -> (holder as TemplateViewHolder).bind(item)
        }
    }

    private fun FolderViewHolder.bind(item: TemplateItem.Folder) {
        textName.text = item.name
        textCount.text = "${item.count}件"
        itemView.setOnClickListener { onFolderClick(item.name) }
        itemView.setOnLongClickListener { showPreview(it, item.name); true }
    }

    private fun TemplateViewHolder.bind(item: TemplateItem.Template) {
        textContent.text = item.memo.content.replace("\n", " ")
        textFolder.text = item.memo.folder
        textFolder.visibility = if (item.memo.folder != null) View.VISIBLE else View.GONE
        itemView.setOnClickListener { onTemplateClick(item.memo) }
        itemView.setOnLongClickListener { showPreview(it, item.memo.content); true }
    }

    override fun getItemCount() = items.size

    private fun showPreview(anchorView: View, text: String) {
        val context = anchorView.context
        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_preview, null)
        val previewText = popupView.findViewById<TextView>(R.id.previewText)
        previewText.text = text

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = 8f
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0)
    }

    fun updateData(newItems: List<TemplateItem>) {
        val diffResult = DiffUtil.calculateDiff(TemplateItemDiffCallback(items, newItems))
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    private class TemplateItemDiffCallback(
        private val oldList: List<TemplateItem>,
        private val newList: List<TemplateItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return when {
                old is TemplateItem.Folder && new is TemplateItem.Folder -> old.name == new.name
                old is TemplateItem.Template && new is TemplateItem.Template -> old.memo.id == new.memo.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return when {
                old is TemplateItem.Folder && new is TemplateItem.Folder ->
                    old.name == new.name && old.count == new.count
                old is TemplateItem.Template && new is TemplateItem.Template ->
                    old.memo.content == new.memo.content && old.memo.folder == new.memo.folder
                else -> false
            }
        }
    }
}
