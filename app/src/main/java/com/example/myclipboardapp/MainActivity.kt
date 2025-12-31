package com.example.myclipboardapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var headerMenuButton: ImageView
    private lateinit var deleteButton: ImageView
    private lateinit var pagerAdapter: ViewPagerAdapter
    private lateinit var backButton: ImageView
    private lateinit var headerTitle: TextView
    private lateinit var searchButton: ImageView
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private var isSearchMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ステータスバーの文字色を黒にする
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        headerMenuButton = findViewById(R.id.headerMenuButton)
        deleteButton = findViewById(R.id.deleteButton)
        backButton = findViewById(R.id.backButton)
        headerTitle = findViewById(R.id.headerTitle)
        searchButton = findViewById(R.id.searchButton)
        searchView = findViewById(R.id.searchView)

        backButton.setOnClickListener {
            if (isSearchMode) {
                hideSearchBar()
            } else {
                onBackButtonClicked()
            }
        }

        // 検索ボタン
        searchButton.setOnClickListener {
            showSearchBar()
        }

        // 検索クエリリスナー
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // 決定ボタン押下時：キーボードだけ閉じる
                searchView.clearFocus()
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                performSearch(newText ?: "")
                return true
            }
        })

        // ViewPager設定
        pagerAdapter = ViewPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // タブとViewPagerを連携
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "履歴"
                1 -> "定型文"
                else -> ""
            }
        }.attach()

        // ヘッダーメニュー
        headerMenuButton.setOnClickListener { view ->
            showHeaderMenu(view)
        }

        // ゴミ箱ボタン
        deleteButton.setOnClickListener {
            deleteSelectedItems()
        }

        // Fragment切り替え時の処理
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // 検索モード解除
                if (isSearchMode) {
                    hideSearchBar()
                }

                // 定型文タブ以外に移動した時、フォルダモードを終了
                if (position != 1) {
                    val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
                    if (templateFragment?.isInFolder() == true) {
                        templateFragment.exitFolder()
                    }
                }

                when (position) {
                    0 -> {
                        val fragment = supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment
                        fragment?.exitSelectMode()
                    }
                    1 -> {  // ← この部分を追加
                        val fragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
                        fragment?.exitSelectMode()
                    }
                }
                deleteButton.visibility = View.GONE
            }
        })
    }

    override fun onBackPressed() {
        // 検索モード中なら検索解除
        if (isSearchMode) {
            hideSearchBar()
            return
        }

        // フォルダモード中なら戻る
        if (backButton.visibility == View.VISIBLE) {
            onBackButtonClicked()
            return
        }

        // 選択モード中なら解除
        val hasSelection = when (viewPager.currentItem) {
            0 -> (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.getSelectedItems()?.isNotEmpty() == true
            1 -> (supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment)?.getSelectedItems()?.isNotEmpty() == true  // ← 追加
            else -> false
        }

        if (hasSelection) {
            when (viewPager.currentItem) {
                0 -> (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.exitSelectMode()
                1 -> (supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment)?.exitSelectMode()  // ← 追加
            }
            deleteButton.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }

    private fun showHeaderMenu(view: View) {
        val popup = PopupMenu(this, view)

        // 現在のタブに応じてメニューを切り替え
        val menuRes = when (viewPager.currentItem) {
            0 -> R.menu.header_menu_clipboard
            1 -> R.menu.header_menu_template
            else -> R.menu.header_menu_clipboard
        }

        popup.menuInflater.inflate(menuRes, popup.menu)

        // 選択状態の確認
        val isAllSelected = when (viewPager.currentItem) {
            0 -> (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.isAllSelected() ?: false
            1 -> (supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment)?.isAllSelected() ?: false  // ← 追加
            else -> false
        }

        popup.menu.findItem(R.id.action_select_all)?.isVisible = !isAllSelected
        popup.menu.findItem(R.id.action_deselect_all)?.isVisible = isAllSelected

        // 履歴タブの場合、選択モード時のみ「定型文に追加」を表示
        if (viewPager.currentItem == 0) {
            val hasSelection = (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.getSelectedItems()?.isNotEmpty() == true
            popup.menu.findItem(R.id.action_add_selected_to_template)?.isVisible = hasSelection
        }

        // フォルダ内でのみ「フォルダ名を変更」を表示
        val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
        val isInFolder = templateFragment?.isInFolder() ?: false
        popup.menu.findItem(R.id.action_rename_folder)?.isVisible = isInFolder

        // 定型文タブの場合、選択内容に応じて「移動」を表示
        if (viewPager.currentItem == 1) {
            val selectedItems = templateFragment?.getSelectedItems()
            if (selectedItems != null && selectedItems.isNotEmpty()) {
                if (isInFolder) {
                    // フォルダ内：全てテンプレートなので常に表示
                    popup.menu.findItem(R.id.action_move_selected)?.isVisible = true
                } else {
                    // フォルダ一覧：フォルダが含まれていなければ表示
                    val hasFolder = (selectedItems as? Set<String>)?.any { it.startsWith("folder:") } ?: false
                    popup.menu.findItem(R.id.action_move_selected)?.isVisible = !hasFolder
                }
            } else {
                // 未選択時は非表示
                popup.menu.findItem(R.id.action_move_selected)?.isVisible = false
            }
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_rename_folder -> {
                    showRenameFolderDialog()
                    true
                }
                R.id.action_add_template -> {
                    showAddTemplateDialog()
                    true
                }
                R.id.action_select_all -> {
                    when (viewPager.currentItem) {
                        0 -> (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.selectAll()
                        1 -> (supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment)?.selectAll()  // ← 追加
                    }
                    true
                }
                R.id.action_deselect_all -> {
                    when (viewPager.currentItem) {
                        0 -> (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.deselectAll()
                        1 -> (supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment)?.deselectAll()  // ← 追加
                    }
                    true
                }
                R.id.action_add_selected_to_template -> {
                    addSelectedHistoryToTemplate()
                    true
                }
                R.id.action_move_selected -> {
                    moveSelectedTemplates()
                    true
                }
                R.id.action_sort -> {
                    showSortDialog()
                    true
                }
                R.id.action_settings -> {
                    val settingsIntent = when (viewPager.currentItem) {
                        0 -> Intent(this, ClipboardSettingsActivity::class.java)
                        1 -> Intent(this, TemplateSettingsActivity::class.java)
                        else -> Intent(this, ClipboardSettingsActivity::class.java)
                    }
                    startActivity(settingsIntent)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showSortDialog() {
        val prefsName = when (viewPager.currentItem) {
            0 -> "app_settings"
            1 -> "template_settings"
            else -> "app_settings"
        }

        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val currentSort = prefs.getString("sort_order", "newest") ?: "newest"

        val sortOptions = arrayOf("新しい順", "古い順", "名前順")
        val sortValues = arrayOf("newest", "oldest", "name")
        val checkedItem = sortValues.indexOf(currentSort)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("並び替え")
            .setSingleChoiceItems(sortOptions, checkedItem) { dialog, which ->
                val selectedSort = sortValues[which]
                prefs.edit().putString("sort_order", selectedSort).apply()

                // リストを更新
                when (viewPager.currentItem) {
                    0 -> (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.loadMemos()
                    1 -> (supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment)?.loadMemos()
                }

                dialog.dismiss()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun showAddTemplateDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "定型文を入力"
            setPadding(50, 40, 50, 40)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("定型文を追加")
            .setView(editText)
            .setPositiveButton("次へ") { _, _ ->
                val text = editText.text.toString().trim()
                if (text.isNotEmpty()) {
                    showFolderSelectionDialog(text)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()

        // キーボード自動展開
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        editText.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
    }

    private fun showFolderSelectionDialog(templateText: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val folders = db.memoDao().getFolders().toMutableList()
            folders.add(0, "新しいフォルダを作成")
            folders.add("フォルダを選択しない")

            val folderArray = folders.toTypedArray()

            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("フォルダを選択")
                .setItems(folderArray) { _, which ->
                    when {
                        which == 0 -> {
                            // 新しいフォルダを作成
                            showNewFolderDialog(templateText)
                        }
                        which == folderArray.size - 1 -> {
                            // フォルダを選択しない
                            addTemplateWithFolder(templateText, null)
                        }
                        else -> {
                            // 既存フォルダを選択
                            addTemplateWithFolder(templateText, folderArray[which])
                        }
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    private fun showNewFolderDialog(templateText: String) {
        val editText = android.widget.EditText(this).apply {
            hint = "新しいフォルダ名"
            setPadding(50, 40, 50, 40)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("新しいフォルダを作成")
            .setView(editText)
            .setPositiveButton("作成") { _, _ ->
                val folderName = editText.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    addTemplateWithFolder(templateText, folderName)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()

        // キーボード自動展開
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        editText.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
    }

    private fun addTemplateWithFolder(text: String, folder: String?) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val newTemplate = MemoEntity(
                content = text,
                isTemplate = true,
                folder = folder
            )
            db.memoDao().insert(newTemplate)

            // 定型文タブを更新
            val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
            templateFragment?.loadMemos()

            android.widget.Toast.makeText(
                this@MainActivity,
                "定型文を追加しました",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun showFolderSelectionDialogForHistory(historyMemo: MemoEntity) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val folders = db.memoDao().getFolders().toMutableList()
            folders.add(0, "新しいフォルダを作成")
            folders.add("フォルダを選択しない")

            val folderArray = folders.toTypedArray()

            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("フォルダを選択")
                .setItems(folderArray) { _, which ->
                    when {
                        which == 0 -> {
                            // 新しいフォルダを作成
                            showNewFolderDialogForHistory(historyMemo)
                        }
                        which == folderArray.size - 1 -> {
                            // フォルダを選択しない
                            addHistoryItemAsTemplate(historyMemo, null)
                        }
                        else -> {
                            // 既存フォルダを選択
                            addHistoryItemAsTemplate(historyMemo, folderArray[which])
                        }
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    private fun showNewFolderDialogForHistory(historyMemo: MemoEntity) {
        val editText = android.widget.EditText(this).apply {
            hint = "新しいフォルダ名"
            setPadding(50, 40, 50, 40)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("新しいフォルダを作成")
            .setView(editText)
            .setPositiveButton("作成") { _, _ ->
                val folderName = editText.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    addHistoryItemAsTemplate(historyMemo, folderName)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()

        // キーボード自動展開
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        editText.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
    }

    private fun addHistoryItemAsTemplate(historyMemo: MemoEntity, folder: String?) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val newTemplate = MemoEntity(
                content = historyMemo.content,
                isTemplate = true,
                folder = folder,
                createdAt = System.currentTimeMillis(),
                id = 0
            )
            db.memoDao().insert(newTemplate)

            // 定型文タブを更新
            val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
            templateFragment?.loadMemos()

            android.widget.Toast.makeText(
                this@MainActivity,
                "定型文に追加しました",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun showFolderSelectionDialogForMove(templateMemo: MemoEntity) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val folders = db.memoDao().getFolders().toMutableList()
            folders.add(0, "新しいフォルダを作成")
            folders.add("フォルダを選択しない")

            val folderArray = folders.toTypedArray()

            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("移動先を選択")
                .setItems(folderArray) { _, which ->
                    when {
                        which == 0 -> {
                            // 新しいフォルダを作成
                            showNewFolderDialogForMove(templateMemo)
                        }
                        which == folderArray.size - 1 -> {
                            // フォルダを選択しない
                            moveTemplateToFolder(templateMemo, null)
                        }
                        else -> {
                            // 既存フォルダを選択
                            moveTemplateToFolder(templateMemo, folderArray[which])
                        }
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    private fun showNewFolderDialogForMove(templateMemo: MemoEntity) {
        val editText = android.widget.EditText(this).apply {
            hint = "新しいフォルダ名"
            setPadding(50, 40, 50, 40)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("新しいフォルダを作成")
            .setView(editText)
            .setPositiveButton("作成") { _, _ ->
                val folderName = editText.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    moveTemplateToFolder(templateMemo, folderName)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()

        // キーボード自動展開
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        editText.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
    }

    private fun moveTemplateToFolder(templateMemo: MemoEntity, folder: String?) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val updatedMemo = templateMemo.copy(folder = folder)
            db.memoDao().update(updatedMemo)

            // 定型文タブを更新
            val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
            templateFragment?.loadMemos()

            val message = if (folder != null) {
                "「$folder」に移動しました"
            } else {
                "フォルダから移動しました"
            }

            android.widget.Toast.makeText(
                this@MainActivity,
                message,
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addSelectedHistoryToTemplate() {
        val clipboardFragment = supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment
        val selectedIds = clipboardFragment?.getSelectedItems() ?: return
        if (selectedIds.isEmpty()) return

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val allMemos = db.memoDao().getAllMemos()
            val selectedMemos = allMemos.filter { (selectedIds as Set<Int>).contains(it.id) }

            if (selectedMemos.size == 1) {
                // 1件のみの場合は既存のダイアログを使用
                showFolderSelectionDialogForHistory(selectedMemos[0])
            } else {
                // 複数件の場合は一括追加用のダイアログ
                showFolderSelectionDialogForMultiple(selectedMemos)
            }
        }
    }

    private fun showFolderSelectionDialogForMultiple(historyMemos: List<MemoEntity>) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val folders = db.memoDao().getFolders().toMutableList()
            folders.add(0, "新しいフォルダを作成")
            folders.add("フォルダを選択しない")

            val folderArray = folders.toTypedArray()

            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("フォルダを選択（${historyMemos.size}件）")
                .setItems(folderArray) { _, which ->
                    when {
                        which == 0 -> {
                            // 新しいフォルダを作成
                            showNewFolderDialogForMultiple(historyMemos)
                        }
                        which == folderArray.size - 1 -> {
                            // フォルダを選択しない
                            addMultipleHistoryItemsAsTemplate(historyMemos, null)
                        }
                        else -> {
                            // 既存フォルダを選択
                            addMultipleHistoryItemsAsTemplate(historyMemos, folderArray[which])
                        }
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    private fun showNewFolderDialogForMultiple(historyMemos: List<MemoEntity>) {
        val editText = android.widget.EditText(this).apply {
            hint = "新しいフォルダ名"
            setPadding(50, 40, 50, 40)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("新しいフォルダを作成")
            .setView(editText)
            .setPositiveButton("作成") { _, _ ->
                val folderName = editText.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    addMultipleHistoryItemsAsTemplate(historyMemos, folderName)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()

        // キーボード自動展開
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        editText.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
    }

    private fun addMultipleHistoryItemsAsTemplate(historyMemos: List<MemoEntity>, folder: String?) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)

            historyMemos.forEach { historyMemo ->
                val newTemplate = MemoEntity(
                    content = historyMemo.content,
                    isTemplate = true,
                    folder = folder,
                    createdAt = System.currentTimeMillis(),
                    id = 0
                )
                db.memoDao().insert(newTemplate)
            }

            // 定型文タブを更新
            val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
            templateFragment?.loadMemos()

            // 選択モード解除
            val clipboardFragment = supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment
            clipboardFragment?.exitSelectMode()
            deleteButton.visibility = View.GONE

            android.widget.Toast.makeText(
                this@MainActivity,
                "${historyMemos.size}件を定型文に追加しました",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun moveSelectedTemplates() {
        val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
        val selectedItems = templateFragment?.getSelectedItems() ?: return
        if (selectedItems.isEmpty()) return

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val allMemos = db.memoDao().getAllMemos()

            // 選択内容からテンプレートIDを抽出
            val selectedMemos = if (selectedItems.first() is String) {
                // フォルダ一覧の場合：Set<String> ("template:id"形式)
                val templateIds = (selectedItems as Set<String>)
                    .filter { it.startsWith("template:") }
                    .map { it.removePrefix("template:").toInt() }
                allMemos.filter { templateIds.contains(it.id) }
            } else {
                // フォルダ内の場合：Set<Int>
                allMemos.filter { (selectedItems as Set<Int>).contains(it.id) }
            }

            if (selectedMemos.size == 1) {
                // 1件のみの場合は既存のダイアログを使用
                showFolderSelectionDialogForMove(selectedMemos[0])
            } else {
                // 複数件の場合は一括移動用のダイアログ
                showFolderSelectionDialogForMoveMultiple(selectedMemos)
            }
        }
    }

    private fun showFolderSelectionDialogForMoveMultiple(templates: List<MemoEntity>) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val folders = db.memoDao().getFolders().toMutableList()
            folders.add(0, "新しいフォルダを作成")
            folders.add("フォルダを選択しない")

            val folderArray = folders.toTypedArray()

            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("フォルダを選択（${templates.size}件）")
                .setItems(folderArray) { _, which ->
                    when {
                        which == 0 -> {
                            // 新しいフォルダを作成
                            showNewFolderDialogForMoveMultiple(templates)
                        }
                        which == folderArray.size - 1 -> {
                            // フォルダを選択しない
                            moveMultipleTemplates(templates, null)
                        }
                        else -> {
                            // 既存フォルダを選択
                            moveMultipleTemplates(templates, folderArray[which])
                        }
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    private fun showNewFolderDialogForMoveMultiple(templates: List<MemoEntity>) {
        val editText = android.widget.EditText(this).apply {
            hint = "新しいフォルダ名"
            setPadding(50, 40, 50, 40)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("新しいフォルダを作成")
            .setView(editText)
            .setPositiveButton("作成") { _, _ ->
                val folderName = editText.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    moveMultipleTemplates(templates, folderName)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()

        // キーボード自動展開
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        editText.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
    }

    private fun moveMultipleTemplates(templates: List<MemoEntity>, folder: String?) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)

            templates.forEach { template ->
                val updated = template.copy(folder = folder)
                db.memoDao().update(updated)
            }

            // 定型文タブを更新
            val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
            templateFragment?.loadMemos()

            // 選択モード解除
            templateFragment?.exitSelectMode()
            deleteButton.visibility = View.GONE

            android.widget.Toast.makeText(
                this@MainActivity,
                "${templates.size}件を移動しました",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun deleteSelectedItems() {
        when (viewPager.currentItem) {
            0 -> {
                (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.deleteSelectedItems {
                    deleteButton.visibility = View.GONE
                }
            }
            1 -> {  // ← 追加
                (supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment)?.deleteSelectedItems {
                    deleteButton.visibility = View.GONE
                }
            }
        }
    }

    // Fragmentから呼ばれる公開メソッド
    fun updateDeleteButtonVisibility(visible: Boolean) {
        deleteButton.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun onBackButtonClicked() {
        val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
        templateFragment?.exitFolder()
    }

    fun showFolderMode(folderName: String) {
        // 検索モード解除
        if (isSearchMode) {
            hideSearchBar()
        }

        backButton.visibility = View.VISIBLE
        headerTitle.text = folderName
    }

    fun showNormalMode() {
        backButton.visibility = View.GONE
        headerTitle.text = "MoreClip"
        headerMenuButton.visibility = View.VISIBLE
    }

    private fun showRenameFolderDialog() {
        val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
        val currentFolderName = templateFragment?.getCurrentFolderName() ?: return

        val editText = android.widget.EditText(this).apply {
            hint = "新しいフォルダ名"
            setText(currentFolderName)
            setPadding(50, 40, 50, 40)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("フォルダ名を変更")
            .setView(editText)
            .setPositiveButton("変更") { _, _ ->
                val newFolderName = editText.text.toString().trim()
                if (newFolderName.isNotEmpty() && newFolderName != currentFolderName) {
                    templateFragment.renameFolder(currentFolderName, newFolderName)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()

        // キーボード自動展開
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        editText.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
    }

    private fun showSearchBar() {
        isSearchMode = true

        // タイトル非表示、SearchView表示
        headerTitle.visibility = View.GONE
        searchView.visibility = View.VISIBLE

        // 戻るボタン表示、検索・メニューボタン非表示
        backButton.visibility = View.VISIBLE
        searchButton.visibility = View.GONE
        headerMenuButton.visibility = View.GONE

        // キーボード表示（少し遅延させる）
        searchView.post {
            searchView.isIconified = false
            searchView.requestFocus()
        }
    }

    private fun hideSearchBar() {
        isSearchMode = false

        // SearchView非表示、タイトル表示
        searchView.visibility = View.GONE
        headerTitle.visibility = View.VISIBLE

        // フォルダモード中かチェック
        val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
        val isInFolder = templateFragment?.isInFolder() ?: false

        // 戻るボタンはフォルダモード時のみ表示
        backButton.visibility = if (isInFolder) View.VISIBLE else View.GONE

        // 検索・メニューボタン表示
        searchButton.visibility = View.VISIBLE
        headerMenuButton.visibility = View.VISIBLE

        // 検索クリア
        searchView.setQuery("", false)
        searchView.clearFocus()

        // キーボード非表示
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(searchView.windowToken, 0)

        // 検索をクリア
        performSearch("")
    }

    private fun performSearch(query: String) {
        when (viewPager.currentItem) {
            0 -> (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.filterMemos(query)
            1 -> (supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment)?.filterMemos(query)
        }
    }

}