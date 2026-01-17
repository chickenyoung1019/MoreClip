package com.chickenyoung.moreclip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TemplateAdapter(
    private var items: List<TemplateItem>,
    private val onFolderClick: (String) -> Unit,
    private val onTemplateClick: (MemoEntity) -> Unit,
    private val onFolderEdit: (String) -> Unit,
    private val onFolderDelete: (String) -> Unit,
    private val onTemplateEdit: (MemoEntity) -> Unit,
    private val onTemplateDelete: (MemoEntity) -> Unit,
    private val onTemplateMove: (MemoEntity) -> Unit,
    private val onSelectMode: () -> Unit,
    private val onSelectionChanged: (Set<String>) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_FOLDER = 0
        private const val TYPE_TEMPLATE = 1
    }

    private var isSelectMode = false
    private var isReorderMode = false
    private val selectedItems = mutableSetOf<String>()  // "folder:名前" or "template:id"

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TemplateItem.Folder -> TYPE_FOLDER
            is TemplateItem.Template -> TYPE_TEMPLATE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FOLDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_folder, parent, false)
                FolderViewHolder(view)
            }
            TYPE_TEMPLATE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_template, parent, false)
                TemplateViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TemplateItem.Folder -> {
                (holder as FolderViewHolder).bind(item)
            }
            is TemplateItem.Template -> {
                (holder as TemplateViewHolder).bind(item)
            }
        }
    }

    override fun getItemCount() = items.size

    inner class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val folderNameText: TextView = view.findViewById(R.id.folderNameText)
        private val folderIcon: ImageView = view.findViewById(R.id.folderIcon)
        private val folderMenuButton: ImageView = view.findViewById(R.id.folderMenuButton)

        fun bind(folder: TemplateItem.Folder) {
            folderNameText.text = folder.name

            val itemKey = "folder:${folder.name}"

            when {
                isReorderMode -> {
                    // 並び替えモード：メニューボタン非表示
                    folderMenuButton.visibility = View.GONE
                    itemView.setOnClickListener(null)
                    itemView.setOnLongClickListener(null)
                }
                isSelectMode -> {
                    // 選択モード：三点→チェックボックス
                    folderMenuButton.visibility = View.VISIBLE
                    val isSelected = selectedItems.contains(itemKey)
                    folderMenuButton.setImageResource(
                        if (isSelected) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked
                    )
                    folderMenuButton.setOnClickListener {
                        toggleSelection(itemKey)
                    }
                    itemView.setOnClickListener {
                        toggleSelection(itemKey)
                    }
                    itemView.setOnLongClickListener(null)
                }
                else -> {
                    // 通常モード：三点メニュー
                    folderMenuButton.visibility = View.VISIBLE
                    folderMenuButton.setImageResource(R.drawable.ic_more_vert)
                    folderMenuButton.setOnClickListener { view ->
                        showFolderPopupMenu(view, folder.name)
                    }
                    itemView.setOnClickListener {
                        onFolderClick(folder.name)
                    }
                    itemView.setOnLongClickListener {
                        onSelectMode()
                        toggleSelection(itemKey)
                        true
                    }
                }
            }
        }

        private fun showFolderPopupMenu(view: View, folderName: String) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.folder_menu, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit_folder -> {
                        onFolderEdit(folderName)
                        true
                    }
                    R.id.action_delete_folder -> {
                        onFolderDelete(folderName)
                        true
                    }
                    R.id.action_select_mode -> {
                        onSelectMode()
                        toggleSelection("folder:$folderName")
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    inner class TemplateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val contentText: TextView = view.findViewById(R.id.contentText)
        private val dateText: TextView = view.findViewById(R.id.dateText)
        private val menuButton: ImageView = view.findViewById(R.id.menuButton)

        fun bind(template: TemplateItem.Template) {
            contentText.text = template.memo.content
            dateText.text = ""

            // 表示行数設定を反映
            val prefs = itemView.context.getSharedPreferences("template_settings", android.content.Context.MODE_PRIVATE)
            val maxLines = prefs.getInt("max_lines", 1)
            contentText.maxLines = maxLines

            val itemKey = "template:${template.memo.id}"

            when {
                isReorderMode -> {
                    // 並び替えモード：メニューボタン非表示
                    menuButton.visibility = View.GONE
                    itemView.setOnClickListener(null)
                    itemView.setOnLongClickListener(null)
                }
                isSelectMode -> {
                    // 選択モード：チェックボックス表示
                    menuButton.visibility = View.VISIBLE
                    val isSelected = selectedItems.contains(itemKey)
                    menuButton.setImageResource(
                        if (isSelected) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked
                    )
                    menuButton.setOnClickListener {
                        toggleSelection(itemKey)
                    }
                    itemView.setOnClickListener {
                        toggleSelection(itemKey)
                    }
                    itemView.setOnLongClickListener(null)
                }
                else -> {
                    // 通常モード：三点メニュー
                    menuButton.visibility = View.VISIBLE
                    menuButton.setImageResource(R.drawable.ic_more_vert)
                    menuButton.setOnClickListener { view ->
                        showPopupMenu(view, template.memo)
                    }
                    itemView.setOnClickListener {
                        onTemplateClick(template.memo)
                    }
                    itemView.setOnLongClickListener {
                        onSelectMode()
                        toggleSelection(itemKey)
                        true
                    }
                }
            }
        }

        private fun showPopupMenu(view: View, memo: MemoEntity) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.template_menu, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        onTemplateEdit(memo)
                        true
                    }
                    R.id.action_move -> {
                        onTemplateMove(memo)
                        true
                    }
                    R.id.action_delete -> {
                        onTemplateDelete(memo)
                        true
                    }
                    R.id.action_select_mode -> {
                        onSelectMode()
                        toggleSelection("template:${memo.id}")
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun toggleSelection(itemKey: String) {
        if (selectedItems.contains(itemKey)) {
            selectedItems.remove(itemKey)
        } else {
            selectedItems.add(itemKey)
        }

        if (selectedItems.isEmpty()) {
            exitSelectMode()
        }

        notifyDataSetChanged()
        onSelectionChanged(selectedItems)
    }

    fun enterSelectMode() {
        isSelectMode = true
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun exitSelectMode() {
        isSelectMode = false
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedItems.clear()
        items.forEach { item ->
            when (item) {
                is TemplateItem.Folder -> selectedItems.add("folder:${item.name}")
                is TemplateItem.Template -> selectedItems.add("template:${item.memo.id}")
            }
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedItems)
    }

    fun deselectAll() {
        selectedItems.clear()
        exitSelectMode()
        notifyDataSetChanged()
        onSelectionChanged(selectedItems)
    }

    fun getSelectedItems(): Set<String> = selectedItems.toSet()

    fun isAllSelected(): Boolean = selectedItems.size == items.size && items.isNotEmpty()

    fun updateData(newItems: List<TemplateItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    // 並び替えモード
    fun enterReorderMode() {
        isReorderMode = true
        notifyDataSetChanged()
    }

    fun exitReorderMode() {
        isReorderMode = false
        notifyDataSetChanged()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val mutableList = items.toMutableList()
        val item = mutableList.removeAt(fromPosition)
        mutableList.add(toPosition, item)
        items = mutableList
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getCurrentList(): List<TemplateItem> = items
}