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

class TemplateFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: TemplateAdapter
    private var currentFolder: String? = null  // null = フォルダ一覧表示中
    private lateinit var backButton: View  // 戻るボタン（後で追加）
    private var folderContentAdapter: FolderContentAdapter? = null
    private var allTemplateItems: List<TemplateItem> = emptyList()
    private var allFolderMemos: List<MemoEntity> = emptyList()
    private var currentSearchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_template, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        emptyText = view.findViewById(R.id.emptyText)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TemplateAdapter(
            items = emptyList(),
            onFolderClick = { folderName -> openFolder(folderName) },
            onTemplateClick = { memo -> copyToClipboard(memo.content) },
            onFolderEdit = { folderName -> editFolder(folderName) },
            onFolderDelete = { folderName -> deleteFolder(folderName) },
            onTemplateEdit = { memo -> editMemo(memo) },
            onTemplateDelete = { memo -> deleteMemo(memo) },
            onTemplateMove = { memo -> moveTemplate(memo) },
            onSelectMode = { enterSelectModeForList() },
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

            if (currentFolder == null) {
                // フォルダ一覧表示
                val folders = db.memoDao().getFolders()
                val templateItems = mutableListOf<TemplateItem>()

                folders.forEach { folderName ->
                    val templatesInFolder = db.memoDao().getTemplatesByFolder(folderName)
                    templateItems.add(TemplateItem.Folder(folderName, templatesInFolder.size))
                }

                val templatesWithoutFolder = db.memoDao().getTemplatesWithoutFolder()

                // displayOrder順に並び替え
                val sortedTemplates = templatesWithoutFolder.sortedBy { it.displayOrder }

                sortedTemplates.forEach { memo ->
                    templateItems.add(TemplateItem.Template(memo))
                }

                allTemplateItems = templateItems

                // 検索フィルターを適用
                filterMemos(currentSearchQuery)
            } else {
                // フォルダ内容表示
                val memos = db.memoDao().getTemplatesByFolder(currentFolder!!)

                // displayOrder順に並び替え
                allFolderMemos = memos.sortedBy { it.displayOrder }

                // 検索フィルターを適用
                filterMemos(currentSearchQuery)
            }
        }
    }

    fun filterMemos(query: String) {
        currentSearchQuery = query

        if (currentFolder == null) {
            // フォルダ一覧の検索
            val filteredItems = if (query.isEmpty()) {
                allTemplateItems
            } else {
                allTemplateItems.filter { item ->
                    when (item) {
                        is TemplateItem.Folder -> item.name.contains(query, ignoreCase = true)
                        is TemplateItem.Template -> item.memo.content.contains(query, ignoreCase = true)
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyText.visibility = View.GONE
                adapter.updateData(filteredItems)
            }
        } else {
            // フォルダ内の検索
            val filteredMemos = if (query.isEmpty()) {
                allFolderMemos
            } else {
                allFolderMemos.filter { it.content.contains(query, ignoreCase = true) }
            }

            // FolderContentAdapterに切り替え
            if (folderContentAdapter == null) {
                folderContentAdapter = FolderContentAdapter(
                    memos = filteredMemos,
                    onItemClick = { memo -> copyToClipboard(memo.content) },
                    onEdit = { memo -> editMemo(memo) },
                    onDelete = { memo -> deleteMemo(memo) },
                    onMove = { memo -> moveTemplate(memo) },
                    onSelectMode = { enterSelectMode() },
                    onSelectionChanged = { selectedIds ->
                        (activity as? MainActivity)?.updateDeleteButtonVisibility(selectedIds.isNotEmpty())
                    }
                )
                recyclerView.adapter = folderContentAdapter
            } else {
                folderContentAdapter?.updateData(filteredMemos)
            }

            if (filteredMemos.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyText.visibility = View.GONE
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("memo", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "クリップボードにコピーしました", Toast.LENGTH_SHORT).show()

        val prefs = requireContext().getSharedPreferences("template_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("auto_close", true)) {
            requireActivity().finish()
        }
    }

    private fun editMemo(memo: MemoEntity) {
        val intent = Intent(requireContext(), EditMemoActivity::class.java).apply {
            putExtra("MEMO_ID", memo.id)
            putExtra("MEMO_CONTENT", memo.content)
            putExtra("TITLE", "定型文の編集")
        }
        startActivity(intent)
    }

    private fun deleteMemo(memo: MemoEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("削除確認")
            .setMessage("この定型文を削除しますか？")
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

    private fun moveTemplate(memo: MemoEntity) {
        (activity as? MainActivity)?.showFolderSelectionDialogForMove(memo)
    }

    private fun openFolder(folderName: String) {
        currentFolder = folderName
        (activity as? MainActivity)?.showFolderMode(folderName)
        loadMemos()
    }

    fun exitFolder() {
        currentFolder = null
        (activity as? MainActivity)?.showNormalMode()
        (activity as? MainActivity)?.updateDeleteButtonVisibility(false)
        // アダプターをTemplateAdapterに戻す
        recyclerView.adapter = adapter
        folderContentAdapter = null
        loadMemos()
    }

    fun isInFolder(): Boolean {
        return currentFolder != null
    }

    fun getCurrentFolderName(): String? {
        return currentFolder
    }

    fun renameFolder(oldName: String, newName: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            // フォルダ内の定型文をすべて取得して、folderフィールドを更新
            val templates = db.memoDao().getTemplatesByFolder(oldName)
            templates.forEach { template ->
                val updated = template.copy(folder = newName)
                db.memoDao().update(updated)
            }

            // ヘッダータイトル更新
            (activity as? MainActivity)?.showFolderMode(newName)
            currentFolder = newName

            // リスト更新
            loadMemos()

            android.widget.Toast.makeText(
                requireContext(),
                "フォルダ名を変更しました",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun enterSelectMode() {
        folderContentAdapter?.enterSelectMode()
    }

    fun selectAll() {
        if (currentFolder != null) {
            folderContentAdapter?.enterSelectMode()
            folderContentAdapter?.selectAll()
        } else {
            adapter.enterSelectMode()
            adapter.selectAll()
        }
    }

    fun deselectAll() {
        if (currentFolder != null) {
            folderContentAdapter?.deselectAll()
        } else {
            adapter.deselectAll()
        }
    }

    fun getSelectedItems(): Set<*>? {
        return if (currentFolder != null) {
            folderContentAdapter?.getSelectedItems()
        } else {
            adapter.getSelectedItems()
        }
    }

    fun isAllSelected(): Boolean {
        return if (currentFolder != null) {
            folderContentAdapter?.isAllSelected() ?: false
        } else {
            adapter.isAllSelected()
        }
    }

    fun exitSelectMode() {
        if (currentFolder != null) {
            folderContentAdapter?.exitSelectMode()
        } else {
            adapter.exitSelectMode()
        }
    }

    fun deleteSelectedItems(onComplete: () -> Unit) {
        if (currentFolder != null) {
            // フォルダ内の削除（既存のまま）
            val selectedIds = folderContentAdapter?.getSelectedItems() ?: return
            if (selectedIds.isEmpty()) return

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("削除確認")
                .setMessage("選択した${selectedIds.size}件の定型文を削除しますか？")
                .setPositiveButton("削除") { _, _ ->
                    lifecycleScope.launch {
                        val db = AppDatabase.getDatabase(requireContext())
                        val allMemos = db.memoDao().getAllMemos()
                        val memosToDelete = allMemos.filter { (selectedIds as Set<Int>).contains(it.id) }

                        memosToDelete.forEach { memo ->
                            db.memoDao().delete(memo)
                        }

                        folderContentAdapter?.exitSelectMode()
                        loadMemos()
                        android.widget.Toast.makeText(requireContext(), "削除しました", android.widget.Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        } else {
            // フォルダ一覧の削除（フォルダと定型文混在）
            val selectedKeys = adapter.getSelectedItems()
            if (selectedKeys.isEmpty()) return

            val folderCount = selectedKeys.count { it.startsWith("folder:") }
            val templateCount = selectedKeys.count { it.startsWith("template:") }

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("削除確認")
                .setMessage("フォルダ${folderCount}件、定型文${templateCount}件を削除しますか？\n（フォルダ内の定型文も削除されます）")
                .setPositiveButton("削除") { _, _ ->
                    lifecycleScope.launch {
                        val db = AppDatabase.getDatabase(requireContext())

                        selectedKeys.forEach { key ->
                            when {
                                key.startsWith("folder:") -> {
                                    val folderName = key.removePrefix("folder:")
                                    // フォルダ内の定型文を全削除
                                    val templatesInFolder = db.memoDao().getTemplatesByFolder(folderName)
                                    templatesInFolder.forEach { db.memoDao().delete(it) }
                                }
                                key.startsWith("template:") -> {
                                    val templateId = key.removePrefix("template:").toInt()
                                    val allMemos = db.memoDao().getAllMemos()
                                    val memo = allMemos.find { it.id == templateId }
                                    memo?.let { db.memoDao().delete(it) }
                                }
                            }
                        }

                        adapter.exitSelectMode()
                        loadMemos()
                        android.widget.Toast.makeText(requireContext(), "削除しました", android.widget.Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    private fun enterSelectModeForList() {
        if (currentFolder == null) {
            // フォルダ一覧の選択モード
            adapter.enterSelectMode()
        } else {
            // フォルダ内の選択モード
            folderContentAdapter?.enterSelectMode()
        }
    }

    private fun editFolder(folderName: String) {
        val editText = android.widget.EditText(requireContext()).apply {
            hint = "新しいフォルダ名"
            setText(folderName)
            setPadding(50, 40, 50, 40)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("フォルダ名を変更")
            .setView(editText)
            .setPositiveButton("変更") { _, _ ->
                val newFolderName = editText.text.toString().trim()
                if (newFolderName.isNotEmpty() && newFolderName != folderName) {
                    renameFolder(folderName, newFolderName)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()

        // キーボード自動展開
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        editText.requestFocus()
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
    }

    private fun deleteFolder(folderName: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val templates = db.memoDao().getTemplatesByFolder(folderName)

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("削除確認")
                .setMessage("フォルダ「$folderName」と中の定型文${templates.size}件を削除しますか？")
                .setPositiveButton("削除") { _, _ ->
                    lifecycleScope.launch {
                        templates.forEach { template ->
                            db.memoDao().delete(template)
                        }
                        loadMemos()
                        android.widget.Toast.makeText(
                            requireContext(),
                            "削除しました",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    // 並び替えモード
    private var itemTouchHelper: androidx.recyclerview.widget.ItemTouchHelper? = null
    private var reorderBackupListItems: List<TemplateItem>? = null
    private var reorderBackupListMemos: List<MemoEntity>? = null

    fun enterReorderMode() {
        if (currentFolder == null) {
            // フォルダ一覧モードのバックアップ
            reorderBackupListItems = allTemplateItems.toList()
            adapter.enterReorderMode()
        } else {
            // フォルダ内モードのバックアップ
            reorderBackupListMemos = allFolderMemos.toList()
            folderContentAdapter?.enterReorderMode()
        }

        // ItemTouchHelperの設定
        if (itemTouchHelper == null) {
            val callback = object : androidx.recyclerview.widget.ItemTouchHelper.Callback() {
                override fun getMovementFlags(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ): Int {
                    val dragFlags = androidx.recyclerview.widget.ItemTouchHelper.UP or
                            androidx.recyclerview.widget.ItemTouchHelper.DOWN
                    return makeMovementFlags(dragFlags, 0)
                }

                override fun onMove(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ): Boolean {
                    val fromPosition = viewHolder.adapterPosition
                    val toPosition = target.adapterPosition
                    if (currentFolder == null) {
                        adapter.moveItem(fromPosition, toPosition)
                    } else {
                        folderContentAdapter?.moveItem(fromPosition, toPosition)
                    }
                    return true
                }

                override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
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
                if (currentFolder == null) {
                    // フォルダ一覧モード：定型文のみ保存（フォルダは順序なし）
                    val currentList = adapter.getCurrentList()
                    var order = 0
                    currentList.forEach { item ->
                        if (item is TemplateItem.Template) {
                            val updated = item.memo.copy(displayOrder = order)
                            db.memoDao().update(updated)
                            order++
                        }
                    }
                } else {
                    // フォルダ内モード
                    val currentList = folderContentAdapter?.getCurrentList() ?: emptyList()
                    currentList.forEachIndexed { index, memo ->
                        val updated = memo.copy(displayOrder = index)
                        db.memoDao().update(updated)
                    }
                }
                loadMemos()
                android.widget.Toast.makeText(requireContext(), "並び替えを保存しました", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            // 変更を破棄
            if (currentFolder == null) {
                reorderBackupListItems?.let {
                    adapter.updateData(it)
                }
            } else {
                reorderBackupListMemos?.let {
                    folderContentAdapter?.updateData(it)
                }
            }
        }

        // ItemTouchHelperを解除
        itemTouchHelper?.attachToRecyclerView(null)
        if (currentFolder == null) {
            adapter.exitReorderMode()
        } else {
            folderContentAdapter?.exitReorderMode()
        }
        reorderBackupListItems = null
        reorderBackupListMemos = null
    }

}