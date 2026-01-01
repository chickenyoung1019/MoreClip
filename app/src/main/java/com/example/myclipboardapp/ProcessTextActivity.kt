package com.example.myclipboardapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProcessTextActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                // Process Text経由
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            }
            Intent.ACTION_SEND -> {
                // 共有経由
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                } else null
            }
            else -> null
        }

        if (!text.isNullOrBlank()) {
            saveText(text)
        } else {
            finish()
        }
    }

    private fun saveText(text: String) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                val allowDuplicate = prefs.getBoolean("allow_duplicate", false)

                // 重複チェック（履歴のみ）
                if (!allowDuplicate) {
                    val existing = db.memoDao().getHistoryMemos().find { it.content == text }
                    if (existing != null) {
                        // 重複している場合は日時とdisplayOrderを更新（最新に移動）
                        val historyMemos = db.memoDao().getHistoryMemos()

                        // 他の履歴のdisplayOrderを1増やす
                        historyMemos.forEach { memo ->
                            if (memo.id != existing.id) {
                                val updated = memo.copy(displayOrder = memo.displayOrder + 1)
                                db.memoDao().update(updated)
                            }
                        }

                        // 既存メモを最新に移動
                        val updated = existing.copy(
                            createdAt = System.currentTimeMillis(),
                            displayOrder = 0
                        )
                        db.memoDao().update(updated)
                        Toast.makeText(
                            applicationContext,
                            "既に保存済みです",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                        return@launch
                    }
                }

                // 新規保存
                // 既存の履歴のdisplayOrderを1増やす
                val historyMemos = db.memoDao().getHistoryMemos()
                historyMemos.forEach { memo ->
                    val updated = memo.copy(displayOrder = memo.displayOrder + 1)
                    db.memoDao().update(updated)
                }

                // 新規メモをdisplayOrder=0で保存（一番上）
                db.memoDao().insert(MemoEntity(content = text, displayOrder = 0))
                Toast.makeText(
                    applicationContext,
                    "保存しました: ${text.take(10)}...",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext,
                    "保存に失敗しました",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                finish()
            }
        }
    }
}