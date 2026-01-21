package com.chickenyoung.moreclip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FolderContentAdapter(
    private var memos: List<MemoEntity>,
    private val onItemClick: (MemoEntity) -> Unit,
    private val onEdit: (MemoEntity) -> Unit,
    private val onDelete: (MemoEntity) -> Unit,
    private val onMove: (MemoEntity) -> Unit,
    private val onSelectMode: () -> Unit,
    private val onSelectionChanged: (Set<Int>) -> Unit = {}
) : RecyclerView.Adapter<FolderContentAdapter.ViewHolder>() {

    private var isSelectMode = false
    private var isReorderMode = false
    private val selectedItems = mutableSetOf<Int>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contentText: TextView = view.findViewById(R.id.contentText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val menuButton: ImageView = view.findViewById(R.id.menuButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val memo = memos[position]
        holder.contentText.text = memo.content

        // 表示行数設定を反映
        val prefs = holder.itemView.context.getSharedPreferences("template_settings", android.content.Context.MODE_PRIVATE)
        val maxLines = prefs.getInt("max_lines", 1)
        holder.contentText.maxLines = maxLines

        // 日時は空文字（行間を揃えるためViewは残す）
        holder.dateText.text = ""

        when {
            isReorderMode -> {
                // 並び替えモード：メニューボタン非表示
                holder.menuButton.visibility = View.GONE
                holder.itemView.setOnClickListener(null)
                holder.itemView.setOnLongClickListener(null)
            }
            isSelectMode -> {
                // 選択モード：チェックボックス表示
                holder.menuButton.visibility = View.VISIBLE
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
                holder.itemView.setOnLongClickListener(null)
            }
            else -> {
                // 通常モード：三点メニュー
                holder.menuButton.visibility = View.VISIBLE
                holder.menuButton.setImageResource(R.drawable.ic_more_vert)
                holder.menuButton.setOnClickListener { view ->
                    showPopupMenu(view, memo)
                }
                holder.itemView.setOnClickListener {
                    onItemClick(memo)
                }
                holder.itemView.setOnLongClickListener {
                    onSelectMode()
                    toggleSelection(memo.id)
                    true
                }
            }
        }
    }

    override fun getItemCount() = memos.size

    private fun showPopupMenu(view: View, memo: MemoEntity) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.template_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    onEdit(memo)
                    true
                }
                R.id.action_move -> {
                    onMove(memo)
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

        if (selectedItems.isEmpty()) {
            exitSelectMode()
        } else {
            // 選択状態が変わったアイテムのみ更新
            val position = memos.indexOfFirst { it.id == memoId }
            if (position >= 0) {
                notifyItemChanged(position)
            }
        }
        onSelectionChanged(selectedItems)
    }

    fun enterSelectMode() {
        isSelectMode = true
        selectedItems.clear()
        notifyItemRangeChanged(0, memos.size)
    }

    fun exitSelectMode() {
        isSelectMode = false
        selectedItems.clear()
        notifyItemRangeChanged(0, memos.size)
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(memos.map { it.id })
        notifyItemRangeChanged(0, memos.size)
        onSelectionChanged(selectedItems)
    }

    fun deselectAll() {
        selectedItems.clear()
        exitSelectMode()
        onSelectionChanged(selectedItems)
    }

    fun getSelectedItems(): Set<Int> = selectedItems.toSet()

    fun isAllSelected(): Boolean = selectedItems.size == memos.size && memos.isNotEmpty()

    fun updateData(newMemos: List<MemoEntity>) {
        val diffCallback = MemoDiffCallback(memos, newMemos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        memos = newMemos
        diffResult.dispatchUpdatesTo(this)
    }

    // 並び替えモード
    fun enterReorderMode() {
        isReorderMode = true
        notifyItemRangeChanged(0, memos.size)
    }

    fun exitReorderMode() {
        isReorderMode = false
        notifyItemRangeChanged(0, memos.size)
    }

    // DiffUtilコールバック
    private class MemoDiffCallback(
        private val oldList: List<MemoEntity>,
        private val newList: List<MemoEntity>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old.content == new.content && old.createdAt == new.createdAt
        }
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val mutableList = memos.toMutableList()
        val item = mutableList.removeAt(fromPosition)
        mutableList.add(toPosition, item)
        memos = mutableList
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getCurrentList(): List<MemoEntity> = memos
}