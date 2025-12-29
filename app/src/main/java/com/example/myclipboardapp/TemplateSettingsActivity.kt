package com.example.myclipboardapp

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar

class TemplateSettingsActivity : AppCompatActivity() {

    private lateinit var autoCloseSwitch: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_settings)

        // ツールバー設定
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // スイッチの初期化
        autoCloseSwitch = findViewById(R.id.autoCloseSwitch)

        // SharedPreferencesから設定読み込み
        val prefs = getSharedPreferences("template_settings", Context.MODE_PRIVATE)
        autoCloseSwitch.isChecked = prefs.getBoolean("auto_close", true)

        // スイッチの変更を保存
        autoCloseSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_close", isChecked).apply()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}