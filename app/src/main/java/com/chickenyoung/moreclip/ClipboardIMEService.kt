package com.chickenyoung.moreclip

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.Switch
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private var tabTemplate: ImageView? = null
    private var tabHistory: ImageView? = null
    private var switchClose: Switch? = null
    private var switchChange: Switch? = null
    private var toastMessage: TextView? = null
    private var btnUndo: ImageView? = null
    private val handler = Handler(Looper.getMainLooper())

    private var currentFolder: String? = null
    private var lastCommittedTextLength: Int = 0
    private var isHistoryMode = false

    // キャッシュ
    private var cachedHistoryItems: List<TemplateItem> = emptyList()
    private var cachedTemplateItems: List<TemplateItem> = emptyList()
    private var cachedFolderContents: MutableMap<String, List<TemplateItem>> = mutableMapOf()

    override fun onCreateInputView(): View {
        window?.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#F5F5F5")))

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
        recyclerView?.itemAnimator = null
        recyclerView?.adapter = adapter

        btnBack?.setOnClickListener { exitFolder() }

        tabTemplate?.setOnClickListener { switchToTemplate() }
        tabHistory?.setOnClickListener { switchToHistory() }

        view.findViewById<ImageView>(R.id.btnSwitchKeyboard)?.setOnClickListener {
            switchToPreviousInputMethod()
        }

        setupBackspaceButton(view.findViewById(R.id.btnBackspace))

        // Undoボタンの初期化
        btnUndo = view.findViewById(R.id.btnUndo)
        btnUndo?.setColorFilter(0xFF888888.toInt())
        btnUndo?.setOnClickListener { undoLastCommit() }

        // トグルスイッチの初期化
        val prefs = getSharedPreferences("ime_settings", Context.MODE_PRIVATE)
        switchClose = view.findViewById(R.id.switchClose)
        switchChange = view.findViewById(R.id.switchChange)
        toastMessage = view.findViewById(R.id.toastMessage)

        switchClose?.isChecked = prefs.getBoolean("ime_close_after_input", false)
        switchChange?.isChecked = prefs.getBoolean("ime_switch_after_input", true)

        switchClose?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("ime_close_after_input", isChecked).apply()
            val msg = if (isChecked) "入力後にキーボードを閉じます" else "入力後もキーボードを表示します"
            showMessage(msg)
        }

        switchChange?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("ime_switch_after_input", isChecked).apply()
            val msg = if (isChecked) "入力後に前のキーボードに切り替えます" else "入力後もこのキーボードのままです"
            showMessage(msg)
        }

        updateTabUI()
        refreshCache()

        return view
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        refreshCache()
    }

    private fun switchToTemplate() {
        isHistoryMode = false
        currentFolder = null
        disableUndo()
        updateTabUI()
        showCachedData()
    }

    private fun switchToHistory() {
        isHistoryMode = true
        currentFolder = null
        btnBack?.visibility = View.INVISIBLE
        disableUndo()
        updateTabUI()
        showCachedData()
    }

    private fun updateTabUI() {
        if (isHistoryMode) {
            tabTemplate?.setColorFilter(0xFF888888.toInt())
            tabHistory?.setColorFilter(0xFF1976D2.toInt())
        } else {
            tabTemplate?.setColorFilter(0xFF1976D2.toInt())
            tabHistory?.setColorFilter(0xFF888888.toInt())
        }
    }

    private fun openFolder(folderName: String) {
        currentFolder = folderName
        btnBack?.visibility = View.VISIBLE
        disableUndo()
        showCachedData()
    }

    private fun exitFolder() {
        currentFolder = null
        btnBack?.visibility = View.INVISIBLE
        disableUndo()
        showCachedData()
    }

    private fun refreshCache() {
        serviceScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)

                // 履歴キャッシュ
                cachedHistoryItems = db.memoDao().getHistoryMemos()
                    .sortedByDescending { it.createdAt }
                    .map { TemplateItem.Template(it) }

                // 定型文キャッシュ（フォルダ一覧）
                val templateItems = mutableListOf<TemplateItem>()
                val folders = db.memoDao().getFolders()
                cachedFolderContents.clear()
                for (folderName in folders) {
                    val folderTemplates = db.memoDao().getTemplatesByFolder(folderName)
                        .sortedBy { it.displayOrder }
                    templateItems.add(TemplateItem.Folder(folderName, folderTemplates.size))
                    cachedFolderContents[folderName] = folderTemplates.map { TemplateItem.Template(it) }
                }
                val templates = db.memoDao().getTemplatesWithoutFolder()
                    .sortedBy { it.displayOrder }
                for (memo in templates) {
                    templateItems.add(TemplateItem.Template(memo))
                }
                cachedTemplateItems = templateItems
            }
            showCachedData()
        }
    }

    private fun showCachedData() {
        val items = when {
            isHistoryMode -> cachedHistoryItems
            currentFolder != null -> cachedFolderContents[currentFolder] ?: emptyList()
            else -> cachedTemplateItems
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

    /**
     * テキストを入力し、設定に応じた後処理を実行
     */
    private fun commitTextAndHandleAfterAction(text: String) {
        lastCommittedTextLength = text.length
        currentInputConnection?.commitText(text, 1)
        btnUndo?.setColorFilter(0xFF1976D2.toInt())

        val shouldClose = switchClose?.isChecked ?: false
        val shouldSwitch = switchChange?.isChecked ?: true

        if (shouldClose) {
            requestHideSelf(0)
        }
        if (shouldSwitch) {
            switchToPreviousInputMethod()
        }
    }

    private fun undoLastCommit() {
        if (lastCommittedTextLength > 0) {
            currentInputConnection?.deleteSurroundingText(lastCommittedTextLength, 0)
            lastCommittedTextLength = 0
            btnUndo?.setColorFilter(0xFF888888.toInt())
        }
    }

    private fun disableUndo() {
        lastCommittedTextLength = 0
        btnUndo?.setColorFilter(0xFF888888.toInt())
    }

    private val backspaceRunnable = object : Runnable {
        override fun run() {
            sendBackspaceKey()
            handler.postDelayed(this, 50)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBackspaceButton(btn: ImageView?) {
        btn?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    sendBackspaceKey()
                    handler.postDelayed(backspaceRunnable, 400)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(backspaceRunnable)
                    true
                }
                else -> false
            }
        }
    }

    private fun sendBackspaceKey() {
        disableUndo()
        val keyDown = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL)
        currentInputConnection?.sendKeyEvent(keyDown)
        val keyUp = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL)
        currentInputConnection?.sendKeyEvent(keyUp)
    }

    private fun showMessage(msg: String) {
        handler.removeCallbacksAndMessages(null)
        toastMessage?.text = msg
        toastMessage?.visibility = View.VISIBLE
        handler.postDelayed({
            toastMessage?.visibility = View.GONE
        }, 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
    }
}
