package com.example.myclipboardapp

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar

class TemplateSettingsActivity : AppCompatActivity() {

    private lateinit var autoCloseSwitch: SwitchCompat
    private lateinit var maxLinesSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_settings)

        // ステータスバーの文字色を黒にする
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        // ツールバー設定
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // スイッチの初期化
        autoCloseSwitch = findViewById(R.id.autoCloseSwitch)
        maxLinesSpinner = findViewById(R.id.maxLinesSpinner)

        // SharedPreferencesから設定読み込み
        val prefs = getSharedPreferences("template_settings", Context.MODE_PRIVATE)
        autoCloseSwitch.isChecked = prefs.getBoolean("auto_close", true)

        // 表示行数の初期値（デフォルトは3行=インデックス2）
        val maxLines = prefs.getInt("max_lines", 3)
        maxLinesSpinner.setSelection(maxLines - 1)

        // スイッチの変更を保存
        autoCloseSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_close", isChecked).apply()
        }

        // Spinnerの変更を保存
        maxLinesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMaxLines = position + 1 // 0-indexed なので +1
                prefs.edit().putInt("max_lines", selectedMaxLines).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 何もしない
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}