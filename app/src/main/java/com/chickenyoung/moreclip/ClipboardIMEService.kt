package com.chickenyoung.moreclip

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
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

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)

        // RecyclerView設定
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyText = view.findViewById(R.id.emptyText)

        adapter = IMETemplateAdapter(emptyList()) { memo ->
            // タップで入力
            currentInputConnection?.commitText(memo.content, 1)
        }
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = adapter

        // キーボード切り替えボタン
        view.findViewById<Button>(R.id.btnSwitchKeyboard)?.setOnClickListener {
            switchToPreviousInputMethod()
        }

        // データ読み込み
        loadTemplates()

        return view
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // キーボード表示のたびにデータ更新
        loadTemplates()
    }

    private fun loadTemplates() {
        serviceScope.launch {
            val templates = withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                db.memoDao().getAllTemplates()
            }

            if (templates.isEmpty()) {
                recyclerView?.visibility = View.GONE
                emptyText?.visibility = View.VISIBLE
            } else {
                recyclerView?.visibility = View.VISIBLE
                emptyText?.visibility = View.GONE
                adapter?.updateData(templates)
            }
        }
    }
}
