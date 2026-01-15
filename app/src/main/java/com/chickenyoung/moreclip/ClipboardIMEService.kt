package com.chickenyoung.moreclip

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
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
    private var textTitle: TextView? = null
    private var btnBack: ImageView? = null

    private var currentFolder: String? = null

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)

        recyclerView = view.findViewById(R.id.recyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        textTitle = view.findViewById(R.id.textTitle)
        btnBack = view.findViewById(R.id.btnBack)

        adapter = IMETemplateAdapter(
            emptyList(),
            onFolderClick = { folderName -> openFolder(folderName) },
            onTemplateClick = { memo -> currentInputConnection?.commitText(memo.content, 1) }
        )
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = adapter

        btnBack?.setOnClickListener { exitFolder() }

        view.findViewById<Button>(R.id.btnSwitchKeyboard)?.setOnClickListener {
            switchToPreviousInputMethod()
        }

        loadTemplates()

        return view
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        loadTemplates()
    }

    private fun openFolder(folderName: String) {
        currentFolder = folderName
        textTitle?.text = folderName
        btnBack?.visibility = View.VISIBLE
        loadTemplates()
    }

    private fun exitFolder() {
        currentFolder = null
        textTitle?.text = "定型文"
        btnBack?.visibility = View.GONE
        loadTemplates()
    }

    private fun loadTemplates() {
        serviceScope.launch {
            val items = withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                val templateItems = mutableListOf<TemplateItem>()

                if (currentFolder == null) {
                    // フォルダ一覧 + フォルダなし定型文
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
                } else {
                    // フォルダ内の定型文
                    val templates = db.memoDao().getTemplatesByFolder(currentFolder!!)
                        .sortedBy { it.displayOrder }
                    for (memo in templates) {
                        templateItems.add(TemplateItem.Template(memo))
                    }
                }

                templateItems
            }

            if (items.isEmpty()) {
                recyclerView?.visibility = View.GONE
                emptyText?.visibility = View.VISIBLE
            } else {
                recyclerView?.visibility = View.VISIBLE
                emptyText?.visibility = View.GONE
                adapter?.updateData(items)
            }
        }
    }
}
