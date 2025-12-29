package com.example.myclipboardapp

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar

class ClipboardSettingsActivity : AppCompatActivity() {

    private lateinit var duplicateSwitch: SwitchCompat
    private lateinit var autoCloseSwitch: SwitchCompat
    private lateinit var moveToTopSwitch: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clipboard_settings)

        // ツールバー設定
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // スイッチの初期化
        duplicateSwitch = findViewById(R.id.duplicateSwitch)
        autoCloseSwitch = findViewById(R.id.autoCloseSwitch)
        moveToTopSwitch = findViewById(R.id.moveToTopSwitch)

        // SharedPreferencesから設定読み込み
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        duplicateSwitch.isChecked = prefs.getBoolean("allow_duplicate", false)
        autoCloseSwitch.isChecked = prefs.getBoolean("auto_close", true)
        moveToTopSwitch.isChecked = prefs.getBoolean("move_to_top", true)

        // スイッチの変更を保存
        duplicateSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("allow_duplicate", isChecked).apply()
        }

        autoCloseSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_close", isChecked).apply()
        }

        moveToTopSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("move_to_top", isChecked).apply()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}