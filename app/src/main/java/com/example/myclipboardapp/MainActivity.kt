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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        headerMenuButton = findViewById(R.id.headerMenuButton)
        deleteButton = findViewById(R.id.deleteButton)
        backButton = findViewById(R.id.backButton)
        headerTitle = findViewById(R.id.headerTitle)

        backButton.setOnClickListener {
            onBackButtonClicked()
        }

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

        // フォルダ内でのみ「フォルダ名を変更」を表示
        val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
        val isInFolder = templateFragment?.isInFolder() ?: false
        popup.menu.findItem(R.id.action_rename_folder)?.isVisible = isInFolder

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

    private fun showAddTemplateDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "定型文を入力"
            setPadding(50, 40, 50, 40)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
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

        androidx.appcompat.app.AlertDialog.Builder(this)
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

        androidx.appcompat.app.AlertDialog.Builder(this)
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
    }

}