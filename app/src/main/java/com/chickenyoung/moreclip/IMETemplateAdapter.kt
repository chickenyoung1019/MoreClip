package com.chickenyoung.moreclip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * IME用の定型文リストAdapter（シンプル版）
 */
class IMETemplateAdapter(
    private var items: List<MemoEntity>,
    private val onItemClick: (MemoEntity) -> Unit
) : RecyclerView.Adapter<IMETemplateAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textContent: TextView = view.findViewById(R.id.textContent)
        val textFolder: TextView = view.findViewById(R.id.textFolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ime_template, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // 内容表示（1行に収める）
        holder.textContent.text = item.content.replace("\n", " ")

        // フォルダ名表示
        if (item.folder != null) {
            holder.textFolder.text = item.folder
            holder.textFolder.visibility = View.VISIBLE
        } else {
            holder.textFolder.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<MemoEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
