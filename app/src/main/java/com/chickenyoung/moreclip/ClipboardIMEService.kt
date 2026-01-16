package com.chickenyoung.moreclip

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 貼り付け専用IME
 */
class ClipboardIMEService : InputMethodService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var adapter: IMETemplateAdapter? = null
    private var recyclerView: RecyclerView? = null
    private var emptyText: TextView? = null
    private var btnBack: ImageView? = null
    private var tabTemplate: TextView? = null
    private var tabHistory: TextView? = null

    private var currentFolder: String? = null
    private var isHistoryMode = false

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)

        recyclerView = view.findViewById(R.id.recyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        btnBack = view.findViewById(R.id.btnBack)
        tabTemplate = view.findViewById(R.id.tabTemplate)
        tabHistory = view.findViewById(R.id.tabHistory)

        adapter = IMETemplateAdapter(
            emptyList(),
            onFolderClick = { folderName -> openFolder(folderName) },
            onTemplateClick = { memo -> commitTextAndHandleAfterAction(memo.content) }
        )
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = adapter

        btnBack?.setOnClickListener { exitFolder() }

        tabTemplate?.setOnClickListener { switchToTemplate() }
        tabHistory?.setOnClickListener { switchToHistory() }

        view.findViewById<ImageView>(R.id.btnSwitchKeyboard)?.setOnClickListener {
            switchToPreviousInputMethod()
        }

        view.findViewById<ImageView>(R.id.btnBackspace)?.setOnClickListener {
            // バックスペースキーイベントを送信（選択テキストも削除可能）
            val keyEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL)
            currentInputConnection?.sendKeyEvent(keyEvent)
            val keyEventUp = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL)
            currentInputConnection?.sendKeyEvent(keyEventUp)
        }

        loadData()

        return view
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        loadData()
    }

    private fun switchToTemplate() {
        isHistoryMode = false
        currentFolder = null
        updateTabUI()
        loadData()
    }

    private fun switchToHistory() {
        isHistoryMode = true
        currentFolder = null
        btnBack?.visibility = View.INVISIBLE
        updateTabUI()
        loadData()
    }

    private fun updateTabUI() {
        if (isHistoryMode) {
            tabTemplate?.setTextColor(0xFF888888.toInt())
            tabTemplate?.setTypeface(null, android.graphics.Typeface.NORMAL)
            tabHistory?.setTextColor(0xFF1976D2.toInt())
            tabHistory?.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            tabTemplate?.setTextColor(0xFF1976D2.toInt())
            tabTemplate?.setTypeface(null, android.graphics.Typeface.BOLD)
            tabHistory?.setTextColor(0xFF888888.toInt())
            tabHistory?.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    private fun openFolder(folderName: String) {
        currentFolder = folderName
        btnBack?.visibility = View.VISIBLE
        loadData()
    }

    private fun exitFolder() {
        currentFolder = null
        btnBack?.visibility = View.INVISIBLE
        loadData()
    }

    private fun loadData() {
        serviceScope.launch {
            val items = withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)

                if (isHistoryMode) {
                    // 履歴モード
                    val history = db.memoDao().getHistoryMemos()
                        .sortedByDescending { it.createdAt }
                    history.map { TemplateItem.Template(it) }
                } else if (currentFolder == null) {
                    // 定型文モード（フォルダ一覧）
                    val templateItems = mutableListOf<TemplateItem>()
                    val folders = db.memoDao().getFolders()
                    for (folderName in folders) {
                        val count = db.memoDao().getTemplatesByFolder(folderName).size
                        templateItems.add(TemplateItem.Folder(folderName, count))
                    }
                    val templates = db.memoDao().getTemplatesWithoutFolder()
                        .sortedBy { it.displayOrder }
                    for (memo in templates) {
                        templateItems.add(TemplateItem.Template(memo))
                    }
                    templateItems
                } else {
                    // フォルダ内
                    val templates = db.memoDao().getTemplatesByFolder(currentFolder!!)
                        .sortedBy { it.displayOrder }
                    templates.map { TemplateItem.Template(it) }
                }
            }

            if (items.isEmpty()) {
                recyclerView?.visibility = View.INVISIBLE
                emptyText?.visibility = View.VISIBLE
                emptyText?.text = if (isHistoryMode) "履歴がありません" else "定型文がありません"
            } else {
                recyclerView?.visibility = View.VISIBLE
                emptyText?.visibility = View.INVISIBLE
                adapter?.updateData(items)
            }
        }
    }

    /**
     * テキストを入力し、設定に応じた後処理を実行
     * 設定値: "switch" = 前のキーボードに切り替え, "close" = 閉じる, "stay" = そのまま
     */
    private fun commitTextAndHandleAfterAction(text: String) {
        // テキストを入力
        currentInputConnection?.commitText(text, 1)

        // 設定を読み取り
        val prefs = getSharedPreferences("ime_settings", Context.MODE_PRIVATE)
        val action = prefs.getString("ime_after_input_action", "switch") ?: "switch"

        when (action) {
            "switch" -> {
                // キーボードを閉じてから直前のキーボードに切り替え
                requestHideSelf(0)
                switchToPreviousInputMethod()
            }
            "close" -> {
                // キーボードを閉じる（IMEは変わらない）
                requestHideSelf(0)
            }
            "stay" -> {
                // 何もしない（そのまま）
            }
        }
    }
}
