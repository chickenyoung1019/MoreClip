package com.chickenyoung.moreclip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * IME用Adapter（フォルダ/定型文両対応）
 */
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
        return if (viewType == TYPE_FOLDER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ime_folder, parent, false)
            FolderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ime_template, parent, false)
            TemplateViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TemplateItem.Folder -> {
                val h = holder as FolderViewHolder
                h.textName.text = item.name
                h.textCount.text = "${item.count}件"
                h.itemView.setOnClickListener {
                    onFolderClick(item.name)
                }
            }
            is TemplateItem.Template -> {
                val h = holder as TemplateViewHolder
                h.textContent.text = item.memo.content.replace("\n", " ")
                if (item.memo.folder != null) {
                    h.textFolder.text = item.memo.folder
                    h.textFolder.visibility = View.VISIBLE
                } else {
                    h.textFolder.visibility = View.GONE
                }
                h.itemView.setOnClickListener {
                    onTemplateClick(item.memo)
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<TemplateItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
