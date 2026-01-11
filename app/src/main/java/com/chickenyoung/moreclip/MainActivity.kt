package com.chickenyoung.moreclip

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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdSize
import android.widget.FrameLayout

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
    private lateinit var reorderCancelButton: TextView
    private lateinit var reorderDoneButton: TextView
    private lateinit var closeSelectModeButton: ImageView
    private lateinit var selectAllButton: ImageView
    private lateinit var addToTemplateButton: ImageView
    private lateinit var addButton: ImageView
    private lateinit var newFolderButton: ImageView
    private var isSearchMode = false
    private var isReorderMode = false
    private var isSelectMode = false

    // 広告関連
    private var bannerAdView: AdView? = null

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
        reorderCancelButton = findViewById(R.id.reorderCancelButton)
        reorderDoneButton = findViewById(R.id.reorderDoneButton)
        closeSelectModeButton = findViewById(R.id.closeSelectModeButton)
        selectAllButton = findViewById(R.id.selectAllButton)
        addToTemplateButton = findViewById(R.id.addToTemplateButton)
        addButton = findViewById(R.id.addButton)
        newFolderButton = findViewById(R.id.newFolderButton)

        backButton.setOnClickListener {
            if (isSearchMode) {
                hideSearchBar()
            } else {
                onBackButtonClicked()
            }
        }

        // 選択モード解除ボタン
        closeSelectModeButton.setOnClickListener {
            exitSelectMode()
        }

        // 全選択ボタン（トグル動作）
        selectAllButton.setOnClickListener {
            when (viewPager.currentItem) {
                0 -> {
                    val fragment = supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment
                    if (fragment?.isAllSelected() == true) {
                        fragment.deselectAll()
                    } else {
                        fragment?.selectAll()
                    }
                }
                1 -> {
                    val fragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
                    if (fragment?.isAllSelected() == true) {
                        fragment.deselectAll()
                    } else {
                        fragment?.selectAll()
                    }
                }
            }
        }

        // フォルダ移動ボタン（履歴: 定型文に追加、定型文: 移動）
        addToTemplateButton.setOnClickListener {
            when (viewPager.currentItem) {
                0 -> addSelectedHistoryToTemplate()
                1 -> moveSelectedTemplates()
            }
        }

        // 新しいフォルダボタン（定型文タブのみ）
        newFolderButton.setOnClickListener {
            showCreateFolderDialog()
        }

        // 追加ボタン（定型文タブのみ）
        addButton.setOnClickListener {
            showAddTemplateDialog()
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

        // 並び替えモード：キャンセル
        reorderCancelButton.setOnClickListener {
            exitReorderMode(save = false)
        }

        // 並び替えモード：完了
        reorderDoneButton.setOnClickListener {
            exitReorderMode(save = true)
        }

        // Fragment切り替え時の処理
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // 検索モード解除
                if (isSearchMode) {
                    hideSearchBar()
                }

                // +ボタン、フォルダ作成ボタンは定型文タブのみ表示
                addButton.visibility = if (position == 1) View.VISIBLE else View.GONE
                newFolderButton.visibility = if (position == 1) View.VISIBLE else View.GONE

                // 定型文タブ以外に移動した時、フォルダモードを終了
                if (position != 1) {
                    val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
                    if (templateFragment?.isInFolder() == true) {
                        templateFragment.exitFolder()
                    }
                }
            }
        })

        // displayOrderの初期値設定（マイグレーション対応）
        initializeDisplayOrder()

        // バナー広告読み込み
        loadBannerAd()
    }

    private fun initializeDisplayOrder() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val allMemos = db.memoDao().getAllMemos()

            // displayOrderが全て0のメモがあるかチェック
            val memosWithoutOrder = allMemos.filter { it.displayOrder == 0 }
            if (memosWithoutOrder.isNotEmpty()) {
                // 履歴と定型文を分けて処理
                val historyMemos = memosWithoutOrder.filter { !it.isTemplate }
                    .sortedByDescending { it.createdAt }
                val templateMemos = memosWithoutOrder.filter { it.isTemplate }
                    .sortedByDescending { it.createdAt }

                // 履歴に順序を設定
                historyMemos.forEachIndexed { index, memo ->
                    val updated = memo.copy(displayOrder = index)
                    db.memoDao().update(updated)
                }

                // 定型文に順序を設定
                templateMemos.forEachIndexed { index, memo ->
                    val updated = memo.copy(displayOrder = index)
                    db.memoDao().update(updated)
                }
            }
        }
    }

    override fun onBackPressed() {
        // 並び替えモード中ならキャンセル
        if (isReorderMode) {
            exitReorderMode(save = false)
            return
        }

        // 選択モード中なら解除
        if (isSelectMode) {
            exitSelectMode()
            return
        }

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

        super.onBackPressed()
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

        // 選択モード時は「並び替え」「設定」「定型文を追加」「フォルダ名を変更」を非表示
        popup.menu.findItem(R.id.action_sort)?.isVisible = !isSelectMode
        popup.menu.findItem(R.id.action_settings)?.isVisible = !isSelectMode
        popup.menu.findItem(R.id.action_add_template)?.isVisible = !isSelectMode

        // 履歴タブの場合、選択モード時のみ「定型文に追加」を表示
        if (viewPager.currentItem == 0) {
            val hasSelection = (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.getSelectedItems()?.isNotEmpty() == true
            popup.menu.findItem(R.id.action_add_selected_to_template)?.isVisible = hasSelection
        }

        // フォルダ内でのみ「フォルダ名を変更」を表示（ただし選択モード時は非表示）
        val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
        val isInFolder = templateFragment?.isInFolder() ?: false
        popup.menu.findItem(R.id.action_rename_folder)?.isVisible = isInFolder && !isSelectMode

        // 定型文タブの場合、選択内容に応じて「移動」を表示
        if (viewPager.currentItem == 1) {
            val selectedItems = templateFragment?.getSelectedItems()
            val moveItem = popup.menu.findItem(R.id.action_move_selected)

            if (selectedItems != null && selectedItems.isNotEmpty()) {
                moveItem?.isVisible = true
                if (isInFolder) {
                    // フォルダ内：全てテンプレートなので常に有効
                    moveItem?.isEnabled = true
                } else {
                    // フォルダ一覧：フォルダが含まれている場合は無効化（グレーアウト）
                    val hasFolder = (selectedItems as? Set<String>)?.any { it.startsWith("folder:") } ?: false
                    moveItem?.isEnabled = !hasFolder
                }
            } else {
                // 未選択時は非表示
                moveItem?.isVisible = false
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
                    enterReorderMode()
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


    fun showAddTemplateDialog() {
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

    private fun showCreateFolderDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "フォルダ名"
            setPadding(50, 40, 50, 40)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("新しいフォルダを作成")
            .setView(editText)
            .setPositiveButton("次へ") { _, _ ->
                val folderName = editText.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    showAddTemplateToFolderDialog(folderName)
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

    private fun showAddTemplateToFolderDialog(folderName: String) {
        val editText = android.widget.EditText(this).apply {
            hint = "定型文を入力"
            setPadding(50, 40, 50, 40)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("「$folderName」に定型文を追加")
            .setView(editText)
            .setPositiveButton("追加") { _, _ ->
                val text = editText.text.toString().trim()
                if (text.isNotEmpty()) {
                    addTemplateWithFolder(text, folderName)
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
            val prefs = getSharedPreferences("template_settings", MODE_PRIVATE)
            val allowDuplicate = prefs.getBoolean("allow_duplicate", false)

            // 重複チェック（定型文のみ）
            if (!allowDuplicate) {
                val allTemplates = db.memoDao().getAllMemos().filter { it.isTemplate }
                val existing = allTemplates.find { it.content == text && it.folder == folder }
                if (existing != null) {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "既に同じ定型文が存在します",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
            }

            // 既存の定型文のdisplayOrderを1増やす（同じフォルダ内のみ）
            val templatesInFolder = db.memoDao().getAllMemos()
                .filter { it.isTemplate && it.folder == folder }
            templatesInFolder.forEach { memo ->
                val updated = memo.copy(displayOrder = memo.displayOrder + 1)
                db.memoDao().update(updated)
            }

            // 新規定型文をdisplayOrder=0で追加
            val newTemplate = MemoEntity(
                content = text,
                isTemplate = true,
                folder = folder,
                displayOrder = 0
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
            val prefs = getSharedPreferences("template_settings", MODE_PRIVATE)
            val allowDuplicate = prefs.getBoolean("allow_duplicate", false)

            // 重複チェック（定型文のみ）
            if (!allowDuplicate) {
                val allTemplates = db.memoDao().getAllMemos().filter { it.isTemplate }
                val existing = allTemplates.find { it.content == historyMemo.content && it.folder == folder }
                if (existing != null) {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "既に同じ定型文が存在します",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
            }

            // 既存の定型文のdisplayOrderを1増やす（同じフォルダ内のみ）
            val templatesInFolder = db.memoDao().getAllMemos()
                .filter { it.isTemplate && it.folder == folder }
            templatesInFolder.forEach { memo ->
                val updated = memo.copy(displayOrder = memo.displayOrder + 1)
                db.memoDao().update(updated)
            }

            // 新規定型文をdisplayOrder=0で追加
            val newTemplate = MemoEntity(
                content = historyMemo.content,
                isTemplate = true,
                folder = folder,
                createdAt = System.currentTimeMillis(),
                displayOrder = 0,
                id = 0
            )
            db.memoDao().insert(newTemplate)

            // 定型文タブを更新
            val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
            templateFragment?.loadMemos()

            // 選択モード解除
            val clipboardFragment = supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment
            clipboardFragment?.exitSelectMode()
            resetSelectModeUI()

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
            val prefs = getSharedPreferences("template_settings", MODE_PRIVATE)
            val allowDuplicate = prefs.getBoolean("allow_duplicate", false)

            // 重複チェック（自分自身は除外）
            if (!allowDuplicate) {
                val allTemplates = db.memoDao().getAllMemos().filter { it.isTemplate && it.id != templateMemo.id }
                val existing = allTemplates.find { it.content == templateMemo.content && it.folder == folder }
                if (existing != null) {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "移動先に同じ定型文が存在します",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
            }

            val updatedMemo = templateMemo.copy(folder = folder)
            db.memoDao().update(updatedMemo)

            // 定型文タブを更新
            val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
            templateFragment?.loadMemos()

            // 選択モード解除
            templateFragment?.exitSelectMode()
            resetSelectModeUI()

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
            val prefs = getSharedPreferences("template_settings", MODE_PRIVATE)
            val allowDuplicate = prefs.getBoolean("allow_duplicate", false)

            val allTemplates = db.memoDao().getAllMemos().filter { it.isTemplate }
            var addedCount = 0
            var skippedCount = 0

            // 既存の定型文のdisplayOrderを取得（最大値）
            val templatesInFolder = db.memoDao().getAllMemos()
                .filter { it.isTemplate && it.folder == folder }

            // 全ての既存定型文のdisplayOrderを履歴件数分増やす
            templatesInFolder.forEach { memo ->
                val updated = memo.copy(displayOrder = memo.displayOrder + historyMemos.size)
                db.memoDao().update(updated)
            }

            historyMemos.forEachIndexed { index, historyMemo ->
                // 重複チェック
                if (!allowDuplicate) {
                    val existing = allTemplates.find { it.content == historyMemo.content && it.folder == folder }
                    if (existing != null) {
                        skippedCount++
                        return@forEachIndexed
                    }
                }

                // 新規定型文を追加（displayOrderは逆順で設定）
                val newTemplate = MemoEntity(
                    content = historyMemo.content,
                    isTemplate = true,
                    folder = folder,
                    createdAt = System.currentTimeMillis(),
                    displayOrder = historyMemos.size - 1 - index,
                    id = 0
                )
                db.memoDao().insert(newTemplate)
                addedCount++
            }

            // 定型文タブを更新
            val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
            templateFragment?.loadMemos()

            // 選択モード解除
            val clipboardFragment = supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment
            clipboardFragment?.exitSelectMode()
            resetSelectModeUI()

            val message = if (skippedCount > 0) {
                "${addedCount}件を定型文に追加しました（${skippedCount}件は重複のためスキップ）"
            } else {
                "${addedCount}件を定型文に追加しました"
            }

            android.widget.Toast.makeText(
                this@MainActivity,
                message,
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
            val prefs = getSharedPreferences("template_settings", MODE_PRIVATE)
            val allowDuplicate = prefs.getBoolean("allow_duplicate", false)

            val templateIds = templates.map { it.id }
            val allTemplates = db.memoDao().getAllMemos().filter { it.isTemplate && !templateIds.contains(it.id) }

            var movedCount = 0
            var skippedCount = 0

            templates.forEach { template ->
                // 重複チェック
                if (!allowDuplicate) {
                    val existing = allTemplates.find { it.content == template.content && it.folder == folder }
                    if (existing != null) {
                        skippedCount++
                        return@forEach
                    }
                }

                val updated = template.copy(folder = folder)
                db.memoDao().update(updated)
                movedCount++
            }

            // 定型文タブを更新
            val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
            templateFragment?.loadMemos()

            // 選択モード解除
            templateFragment?.exitSelectMode()
            resetSelectModeUI()

            val message = if (skippedCount > 0) {
                "${movedCount}件を移動しました（${skippedCount}件は重複のためスキップ）"
            } else {
                "${movedCount}件を移動しました"
            }

            android.widget.Toast.makeText(
                this@MainActivity,
                message,
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun deleteSelectedItems() {
        when (viewPager.currentItem) {
            0 -> {
                (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.deleteSelectedItems {
                    resetSelectModeUI()
                }
            }
            1 -> {
                (supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment)?.deleteSelectedItems {
                    resetSelectModeUI()
                }
            }
        }
    }

    // 選択モードUIをリセット（Fragmentは既にexitしている場合用）
    private fun resetSelectModeUI() {
        isSelectMode = false

        // タブ切り替えを許可
        viewPager.isUserInputEnabled = true
        tabLayout.getTabAt(0)?.view?.isEnabled = true
        tabLayout.getTabAt(1)?.view?.isEnabled = true

        // フォルダ内なら←を再表示、タイトルもフォルダ名に
        val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
        val isInFolder = templateFragment?.isInFolder() == true
        val folderName = templateFragment?.getCurrentFolderName()

        // ヘッダーUIを通常モードに戻す
        closeSelectModeButton.visibility = View.GONE
        backButton.visibility = if (isInFolder) View.VISIBLE else View.GONE
        headerTitle.text = if (isInFolder && folderName != null) folderName else getString(R.string.app_name)
        searchButton.visibility = View.VISIBLE
        addButton.visibility = if (viewPager.currentItem == 1) View.VISIBLE else View.GONE
        newFolderButton.visibility = if (viewPager.currentItem == 1) View.VISIBLE else View.GONE
        selectAllButton.visibility = View.GONE
        addToTemplateButton.visibility = View.GONE
        headerMenuButton.visibility = View.VISIBLE
        deleteButton.visibility = View.GONE
    }

    // Fragmentから呼ばれる公開メソッド
    fun updateDeleteButtonVisibility(visible: Boolean) {
        deleteButton.visibility = if (visible) View.VISIBLE else View.GONE
    }

    // 選択モード開始（Fragmentから呼ばれる）
    fun enterSelectMode(selectedCount: Int) {
        isSelectMode = true

        // タブ切り替えを禁止
        viewPager.isUserInputEnabled = false
        tabLayout.getTabAt(0)?.view?.isEnabled = false
        tabLayout.getTabAt(1)?.view?.isEnabled = false

        // ヘッダーUIを選択モード用に変更
        backButton.visibility = View.GONE  // フォルダ内でも←を非表示（×と競合するため）
        closeSelectModeButton.visibility = View.VISIBLE
        headerTitle.text = "${selectedCount}件選択中"
        searchButton.visibility = View.GONE
        addButton.visibility = View.GONE
        newFolderButton.visibility = View.GONE
        selectAllButton.visibility = View.VISIBLE
        // フォルダ移動ボタン（履歴: 定型文に追加、定型文: 移動）
        addToTemplateButton.visibility = View.VISIBLE
        addToTemplateButton.isEnabled = true
        addToTemplateButton.alpha = 1.0f
        headerMenuButton.visibility = View.VISIBLE
        deleteButton.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
    }

    // 選択件数更新（Fragmentから呼ばれる）
    fun updateSelectCount(selectedCount: Int) {
        if (isSelectMode) {
            if (selectedCount == 0) {
                // 選択が0件になったら選択モード終了
                resetSelectModeUI()
            } else {
                headerTitle.text = "${selectedCount}件選択中"
                deleteButton.visibility = View.VISIBLE

                // 定型文タブでフォルダが選択されている場合、移動ボタンを非活性に
                if (viewPager.currentItem == 1) {
                    val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
                    val isInFolder = templateFragment?.isInFolder() == true

                    if (isInFolder) {
                        // フォルダ内：全て定型文なので常に移動可能
                        addToTemplateButton.isEnabled = true
                        addToTemplateButton.alpha = 1.0f
                    } else {
                        // フォルダ一覧：フォルダが含まれている場合は非活性に
                        val selectedItems = templateFragment?.getSelectedItems()
                        val hasFolder = (selectedItems as? Set<String>)?.any { it.startsWith("folder:") } == true
                        addToTemplateButton.isEnabled = !hasFolder
                        addToTemplateButton.alpha = if (hasFolder) 0.3f else 1.0f
                    }
                }
            }
        }
    }

    // 選択モード終了
    fun exitSelectMode() {
        if (!isSelectMode) return

        isSelectMode = false

        // 現在のタブのFragmentの選択モードを解除
        when (viewPager.currentItem) {
            0 -> (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.exitSelectMode()
            1 -> (supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment)?.exitSelectMode()
        }

        // タブ切り替えを許可
        viewPager.isUserInputEnabled = true
        tabLayout.getTabAt(0)?.view?.isEnabled = true
        tabLayout.getTabAt(1)?.view?.isEnabled = true

        // フォルダ内なら←を再表示、タイトルもフォルダ名に
        val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
        val isInFolder = templateFragment?.isInFolder() == true
        val folderName = templateFragment?.getCurrentFolderName()

        // ヘッダーUIを通常モードに戻す
        closeSelectModeButton.visibility = View.GONE
        backButton.visibility = if (isInFolder) View.VISIBLE else View.GONE
        headerTitle.text = if (isInFolder && folderName != null) folderName else getString(R.string.app_name)
        searchButton.visibility = View.VISIBLE
        addButton.visibility = if (viewPager.currentItem == 1) View.VISIBLE else View.GONE
        newFolderButton.visibility = if (viewPager.currentItem == 1) View.VISIBLE else View.GONE
        selectAllButton.visibility = View.GONE
        addToTemplateButton.visibility = View.GONE
        headerMenuButton.visibility = View.VISIBLE
        deleteButton.visibility = View.GONE
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
        headerTitle.text = getString(R.string.app_name)
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

        // 戻るボタン表示、検索・メニュー・追加・フォルダボタン非表示
        backButton.visibility = View.VISIBLE
        searchButton.visibility = View.GONE
        addButton.visibility = View.GONE
        newFolderButton.visibility = View.GONE
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

        // 検索・メニュー・追加・フォルダボタン表示
        searchButton.visibility = View.VISIBLE
        addButton.visibility = if (viewPager.currentItem == 1) View.VISIBLE else View.GONE
        newFolderButton.visibility = if (viewPager.currentItem == 1) View.VISIBLE else View.GONE
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

    private fun enterReorderMode() {
        isReorderMode = true

        // ViewPagerのスワイプを無効化（タブ切り替え禁止）
        viewPager.isUserInputEnabled = false

        // タブを無効化（タップで切り替え禁止）
        tabLayout.getTabAt(0)?.view?.isEnabled = false
        tabLayout.getTabAt(1)?.view?.isEnabled = false

        // ヘッダーUIを並び替えモード用に変更
        searchButton.visibility = View.GONE
        addButton.visibility = View.GONE
        newFolderButton.visibility = View.GONE
        headerMenuButton.visibility = View.GONE
        deleteButton.visibility = View.GONE
        reorderCancelButton.visibility = View.VISIBLE
        reorderDoneButton.visibility = View.VISIBLE

        // Fragmentに並び替えモード開始を通知
        when (viewPager.currentItem) {
            0 -> (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.enterReorderMode()
            1 -> (supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment)?.enterReorderMode()
        }
    }

    private fun exitReorderMode(save: Boolean) {
        if (!isReorderMode) return

        // Fragmentに並び替えモード終了を通知
        when (viewPager.currentItem) {
            0 -> (supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment)?.exitReorderMode(save)
            1 -> (supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment)?.exitReorderMode(save)
        }

        isReorderMode = false

        // ViewPagerのスワイプを有効化
        viewPager.isUserInputEnabled = true

        // タブを有効化
        tabLayout.getTabAt(0)?.view?.isEnabled = true
        tabLayout.getTabAt(1)?.view?.isEnabled = true

        // ヘッダーUIを通常モードに戻す
        reorderCancelButton.visibility = View.GONE
        reorderDoneButton.visibility = View.GONE
        searchButton.visibility = View.VISIBLE
        addButton.visibility = if (viewPager.currentItem == 1) View.VISIBLE else View.GONE
        newFolderButton.visibility = if (viewPager.currentItem == 1) View.VISIBLE else View.GONE
        headerMenuButton.visibility = View.VISIBLE
    }

    // バナー広告を読み込む
    private fun loadBannerAd() {
        val adContainer = findViewById<FrameLayout>(R.id.adBannerContainer)

        // AdViewを作成
        bannerAdView = AdView(this).apply {
            adUnitId = "ca-app-pub-5377681981369299/6584173262" // 本番バナー広告ID
            setAdSize(getAdaptiveBannerAdSize())
        }

        // AdViewをコンテナに追加
        adContainer.removeAllViews()
        adContainer.addView(bannerAdView)

        // 広告を読み込む
        val adRequest = AdRequest.Builder().build()
        bannerAdView?.loadAd(adRequest)
    }

    // アダプティブバナーのサイズを取得
    private fun getAdaptiveBannerAdSize(): AdSize {
        val displayMetrics = resources.displayMetrics
        val adWidthPixels = displayMetrics.widthPixels.toFloat()
        val density = displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

}