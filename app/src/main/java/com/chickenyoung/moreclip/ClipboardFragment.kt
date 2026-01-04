package com.chickenyoung.moreclip

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ClipboardFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: ClipboardAdapter
    private var allMemos: List<MemoEntity> = emptyList()
    private var currentSearchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_clipboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        emptyText = view.findViewById(R.id.emptyText)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ClipboardAdapter(
            memos = emptyList(),
            onItemClick = { memo -> copyToClipboard(memo.content) },
            onEdit = { memo -> editMemo(memo) },
            onDelete = { memo -> deleteMemo(memo) },
            onSelectMode = { enterSelectMode() },
            onAddToTemplate = { memo -> addToTemplate(memo) },
            onSelectionChanged = { selectedIds ->
                (activity as? MainActivity)?.updateDeleteButtonVisibility(selectedIds.isNotEmpty())
            }
        )
        recyclerView.adapter = adapter

        loadMemos()
    }

    override fun onResume() {
        super.onResume()
        loadMemos()
    }

    fun loadMemos() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val memos = db.memoDao().getHistoryMemos()

            // displayOrder順に並び替え
            allMemos = memos.sortedBy { it.displayOrder }

            // 検索フィルターを適用
            filterMemos(currentSearchQuery)
        }
    }

    fun filterMemos(query: String) {
        currentSearchQuery = query

        val filteredMemos = if (query.isEmpty()) {
            allMemos
        } else {
            allMemos.filter { it.content.contains(query, ignoreCase = true) }
        }

        if (filteredMemos.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
            adapter.updateData(filteredMemos)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("memo", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "クリップボードにコピーしました", Toast.LENGTH_SHORT).show()

        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val allMemos = db.memoDao().getAllMemos()
            val copiedMemo = allMemos.find { it.content == text }

            copiedMemo?.let {
                if (prefs.getBoolean("move_to_top", true)) {
                    // 履歴メモのみ取得
                    val historyMemos = db.memoDao().getHistoryMemos()

                    // 他の履歴のdisplayOrderを1増やす
                    historyMemos.forEach { memo ->
                        if (memo.id != it.id) {
                            val updated = memo.copy(displayOrder = memo.displayOrder + 1)
                            db.memoDao().update(updated)
                        }
                    }

                    // コピーしたメモを最新（displayOrder=0）に設定
                    val updated = it.copy(
                        createdAt = System.currentTimeMillis(),
                        displayOrder = 0
                    )
                    db.memoDao().update(updated)
                    loadMemos()
                }
            }

            if (prefs.getBoolean("auto_close", true)) {
                requireActivity().finish()
            }
        }
    }

    private fun editMemo(memo: MemoEntity) {
        val intent = Intent(requireContext(), EditMemoActivity::class.java).apply {
            putExtra("MEMO_ID", memo.id)
            putExtra("MEMO_CONTENT", memo.content)
            putExtra("TITLE", "履歴の編集")
        }
        startActivity(intent)
    }

    private fun deleteMemo(memo: MemoEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("削除確認")
            .setMessage("このメモを削除しますか？")
            .setPositiveButton("削除") { _, _ ->
                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(requireContext())
                    db.memoDao().delete(memo)
                    loadMemos()
                    Toast.makeText(requireContext(), "削除しました", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun addToTemplate(memo: MemoEntity) {
        (activity as? MainActivity)?.showFolderSelectionDialogForHistory(memo)
    }

    private fun enterSelectMode() {
        adapter.enterSelectMode()
    }

    // 外部から選択モード操作
    fun selectAll() {
        adapter.enterSelectMode()
        adapter.selectAll()
    }
    fun deselectAll() = adapter.deselectAll()
    fun getSelectedItems() = adapter.getSelectedItems()
    fun isAllSelected() = adapter.isAllSelected()
    fun exitSelectMode() = adapter.exitSelectMode()
    fun deleteSelectedItems(onComplete: () -> Unit) {
        val selectedIds = adapter.getSelectedItems()
        if (selectedIds.isEmpty()) return

        AlertDialog.Builder(requireContext())
            .setTitle("削除確認")
            .setMessage("選択した${selectedIds.size}件のメモを削除しますか？")
            .setPositiveButton("削除") { _, _ ->
                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(requireContext())
                    val allMemos = db.memoDao().getAllMemos()
                    val memosToDelete = allMemos.filter { selectedIds.contains(it.id) }

                    memosToDelete.forEach { memo ->
                        db.memoDao().delete(memo)
                    }

                    adapter.exitSelectMode()
                    loadMemos()
                    Toast.makeText(requireContext(), "削除しました", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    // 並び替えモード
    private var itemTouchHelper: androidx.recyclerview.widget.ItemTouchHelper? = null
    private var reorderBackupList: List<MemoEntity>? = null

    fun enterReorderMode() {
        // バックアップを保存
        reorderBackupList = allMemos.toList()

        // AdapterとItemTouchHelperを設定
        adapter.enterReorderMode()
        if (itemTouchHelper == null) {
            val callback = object : androidx.recyclerview.widget.ItemTouchHelper.Callback() {
                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    val dragFlags = androidx.recyclerview.widget.ItemTouchHelper.UP or
                            androidx.recyclerview.widget.ItemTouchHelper.DOWN
                    return makeMovementFlags(dragFlags, 0)
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val fromPosition = viewHolder.adapterPosition
                    val toPosition = target.adapterPosition
                    adapter.moveItem(fromPosition, toPosition)
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    // スワイプ無効
                }

                override fun isLongPressDragEnabled() = true
            }
            itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(callback)
        }
        itemTouchHelper?.attachToRecyclerView(recyclerView)
    }

    fun exitReorderMode(save: Boolean) {
        if (save) {
            // 並び替え結果を保存
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(requireContext())
                val currentList = adapter.getCurrentList()
                currentList.forEachIndexed { index, memo ->
                    val updated = memo.copy(displayOrder = index)
                    db.memoDao().update(updated)
                }
                loadMemos()
                Toast.makeText(requireContext(), "並び替えを保存しました", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 変更を破棄
            reorderBackupList?.let {
                adapter.updateData(it)
            }
        }

        // ItemTouchHelperを解除
        itemTouchHelper?.attachToRecyclerView(null)
        adapter.exitReorderMode()
        reorderBackupList = null
    }
}