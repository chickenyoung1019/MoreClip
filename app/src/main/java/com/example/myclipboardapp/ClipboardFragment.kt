package com.example.myclipboardapp

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
            val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val sortOrder = prefs.getString("sort_order", "newest") ?: "newest"

            val memos = db.memoDao().getHistoryMemos()

            // 並び替え適用
            val sortedMemos = when (sortOrder) {
                "oldest" -> memos.sortedBy { it.createdAt }
                "name" -> memos.sortedBy { it.content }
                else -> memos.sortedByDescending { it.createdAt } // "newest"
            }

            if (sortedMemos.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyText.visibility = View.GONE
                adapter.updateData(sortedMemos)
            }
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
                    val updated = it.copy(createdAt = System.currentTimeMillis())
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
}