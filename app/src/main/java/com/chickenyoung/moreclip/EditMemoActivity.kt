package com.chickenyoung.moreclip

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class EditMemoActivity : AppCompatActivity() {

    private lateinit var editText: EditText
    private lateinit var saveButton: TextView
    private var memoId: Int = 0
    private var originalContent: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_memo)

        // データ取得
        memoId = intent.getIntExtra("MEMO_ID", 0)
        originalContent = intent.getStringExtra("MEMO_CONTENT") ?: ""

        // ビュー初期化
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        editText = findViewById(R.id.editText)
        saveButton = findViewById(R.id.saveButton)

        // ツールバー設定
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)  // ← 追加

// タイトル設定
        val title = intent.getStringExtra("TITLE") ?: "編集"
        val titleText = findViewById<TextView>(R.id.toolbarTitle)
        titleText.text = title

        toolbar.setNavigationOnClickListener {
            finish()
        }

        // 本文セット＆フォーカス
        editText.setText(originalContent)
        editText.setSelection(editText.text.length)
        editText.requestFocus()

        // 保存ボタン
        saveButton.setOnClickListener {
            saveMemo()
        }
    }

    private fun saveMemo() {
        val newContent = editText.text.toString().trim()

        if (newContent.isEmpty()) {
            Toast.makeText(this, "本文が空です", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val memo = db.memoDao().getAllMemos().find { it.id == memoId }

            if (memo != null) {
                // 重複チェック（内容が変わった場合のみ）
                if (newContent != memo.content) {
                    val prefsKey = if (memo.isTemplate) "template_settings" else "app_settings"
                    val prefs = getSharedPreferences(prefsKey, MODE_PRIVATE)
                    val allowDuplicate = prefs.getBoolean("allow_duplicate", false)

                    if (!allowDuplicate) {
                        val allMemos = db.memoDao().getAllMemos()
                        val isDuplicate = if (memo.isTemplate) {
                            allMemos.any { it.isTemplate && it.id != memoId && it.content == newContent && it.folder == memo.folder }
                        } else {
                            allMemos.any { !it.isTemplate && it.id != memoId && it.content == newContent }
                        }
                        if (isDuplicate) {
                            Toast.makeText(this@EditMemoActivity, "同じ内容が既に存在します", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                    }
                }

                val updatedMemo = memo.copy(content = newContent)
                db.memoDao().update(updatedMemo)
                Toast.makeText(this@EditMemoActivity, "更新しました", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}