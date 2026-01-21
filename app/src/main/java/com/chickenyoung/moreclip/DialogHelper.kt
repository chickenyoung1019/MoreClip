package com.chickenyoung.moreclip

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

/**
 * ダイアログ表示のヘルパークラス
 * MainActivity内の重複コードを共通化
 */
object DialogHelper {

    /**
     * EditText入力ダイアログを表示（キーボード自動展開付き）
     * @param activity Activity
     * @param title ダイアログタイトル
     * @param hint EditTextのヒント
     * @param positiveButtonText 確定ボタンのテキスト
     * @param initialText 初期値（任意）
     * @param onPositiveClick 確定時のコールバック（入力テキストを渡す）
     */
    fun showEditTextDialog(
        activity: Activity,
        title: String,
        hint: String,
        positiveButtonText: String,
        initialText: String? = null,
        onPositiveClick: (String) -> Unit
    ) {
        val editText = EditText(activity).apply {
            this.hint = hint
            setPadding(50, 40, 50, 40)
            initialText?.let { setText(it) }
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(editText)
            .setPositiveButton(positiveButtonText) { _, _ ->
                val text = editText.text.toString().trim()
                if (text.isNotEmpty()) {
                    onPositiveClick(text)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()

        // キーボード自動展開
        showKeyboard(activity, dialog, editText)
    }

    /**
     * フォルダ選択ダイアログを表示
     * @param activity Activity
     * @param title ダイアログタイトル
     * @param existingFolders 既存フォルダのリスト
     * @param onNewFolderClick 「新しいフォルダを作成」選択時のコールバック
     * @param onFolderSelected フォルダ選択時のコールバック（nullの場合は「フォルダを選択しない」）
     */
    fun showFolderSelectionDialog(
        activity: Activity,
        title: String,
        existingFolders: List<String>,
        onNewFolderClick: () -> Unit,
        onFolderSelected: (String?) -> Unit
    ) {
        val folders = existingFolders.toMutableList()
        folders.add(0, "新しいフォルダを作成")
        folders.add("フォルダを選択しない")

        val folderArray = folders.toTypedArray()

        AlertDialog.Builder(activity)
            .setTitle(title)
            .setItems(folderArray) { _, which ->
                when {
                    which == 0 -> {
                        // 新しいフォルダを作成
                        onNewFolderClick()
                    }
                    which == folderArray.size - 1 -> {
                        // フォルダを選択しない
                        onFolderSelected(null)
                    }
                    else -> {
                        // 既存フォルダを選択
                        onFolderSelected(folderArray[which])
                    }
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    /**
     * キーボードを自動展開
     */
    private fun showKeyboard(activity: Activity, dialog: AlertDialog, editText: EditText) {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        editText.requestFocus()
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
    }
}
