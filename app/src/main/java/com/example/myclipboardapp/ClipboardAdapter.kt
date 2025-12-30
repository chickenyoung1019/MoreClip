package com.example.myclipboardapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClipboardAdapter(
    private var memos: List<MemoEntity>,
    private val onItemClick: (MemoEntity) -> Unit,
    private val onEdit: (MemoEntity) -> Unit,
    private val onDelete: (MemoEntity) -> Unit,
    private val onSelectMode: () -> Unit,
    private val onAddToTemplate: (MemoEntity) -> Unit,
    private val onSelectionChanged: (Set<Int>) -> Unit = {}
) : RecyclerView.Adapter<ClipboardAdapter.MemoViewHolder>() {

    private var isSelectMode = false
    private val selectedItems = mutableSetOf<Int>()

    class MemoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contentText: TextView = view.findViewById(R.id.contentText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val menuButton: ImageView = view.findViewById(R.id.menuButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memo, parent, false)
        return MemoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        val memo = memos[position]
        holder.contentText.text = memo.content

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        holder.dateText.text = dateFormat.format(Date(memo.createdAt))

        // 選択モードの表示切り替え
        if (isSelectMode) {
            // 選択モード：○または●✓を表示
            val isSelected = selectedItems.contains(memo.id)
            holder.menuButton.setImageResource(
                if (isSelected) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked
            )
            holder.menuButton.setOnClickListener {
                toggleSelection(memo.id)
            }
            holder.itemView.setOnClickListener {
                toggleSelection(memo.id)
            }
            // 長押しは無効化
            holder.itemView.setOnLongClickListener(null)
        } else {
            // 通常モード：︙を表示
            holder.menuButton.setImageResource(R.drawable.ic_more_vert)
            holder.menuButton.setOnClickListener { view ->
                showPopupMenu(view, memo)
            }
            holder.itemView.setOnClickListener {
                onItemClick(memo)
            }
            // 長押しで選択モード開始
            holder.itemView.setOnLongClickListener {
                onSelectMode()
                toggleSelection(memo.id)
                true
            }
        }
    }

    override fun getItemCount() = memos.size

    private fun showPopupMenu(view: View, memo: MemoEntity) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.clipboard_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    onEdit(memo)
                    true
                }
                R.id.action_add_to_template -> {
                    onAddToTemplate(memo)
                    true
                }
                R.id.action_delete -> {
                    onDelete(memo)
                    true
                }
                R.id.action_select_mode -> {
                    onSelectMode()
                    toggleSelection(memo.id)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun toggleSelection(memoId: Int) {
        if (selectedItems.contains(memoId)) {
            selectedItems.remove(memoId)
        } else {
            selectedItems.add(memoId)
        }

        // 全て未選択になったら選択モード解除
        if (selectedItems.isEmpty()) {
            exitSelectMode()
        }

        notifyDataSetChanged()
        onSelectionChanged(selectedItems)
    }

    fun enterSelectMode() {
        isSelectMode = true
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun exitSelectMode() {
        isSelectMode = false
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(memos.map { it.id })
        notifyDataSetChanged()
        onSelectionChanged(selectedItems)
    }

    fun deselectAll() {
        selectedItems.clear()
        exitSelectMode()
        notifyDataSetChanged()
        onSelectionChanged(selectedItems)
    }

    fun getSelectedItems(): Set<Int> = selectedItems.toSet()

    fun isAllSelected(): Boolean = selectedItems.size == memos.size && memos.isNotEmpty()

    fun updateData(newMemos: List<MemoEntity>) {
        memos = newMemos
        notifyDataSetChanged()
    }
}