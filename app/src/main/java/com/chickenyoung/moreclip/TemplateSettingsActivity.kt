package com.chickenyoung.moreclip

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

class TemplateSettingsActivity : AppCompatActivity() {

    private lateinit var duplicateSwitch: SwitchCompat
    private lateinit var autoCloseSwitch: SwitchCompat
    private lateinit var maxLinesSpinner: Spinner
    private var bannerAdView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_settings)

        // バナー広告読み込み
        loadBannerAd()

        // ステータスバーの文字色を黒にする
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        // ツールバー設定
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // スイッチの初期化
        duplicateSwitch = findViewById(R.id.duplicateSwitch)
        autoCloseSwitch = findViewById(R.id.autoCloseSwitch)
        maxLinesSpinner = findViewById(R.id.maxLinesSpinner)

        // SharedPreferencesから設定読み込み
        val prefs = getSharedPreferences("template_settings", Context.MODE_PRIVATE)
        duplicateSwitch.isChecked = prefs.getBoolean("allow_duplicate", false)
        autoCloseSwitch.isChecked = prefs.getBoolean("auto_close", true)

        // 表示行数の初期値（デフォルトは3行=インデックス2）
        val maxLines = prefs.getInt("max_lines", 3)
        maxLinesSpinner.setSelection(maxLines - 1)

        // スイッチの変更を保存
        duplicateSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("allow_duplicate", isChecked).apply()
        }

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

    // バナー広告を読み込む
    private fun loadBannerAd() {
        val adContainer = findViewById<FrameLayout>(R.id.adBannerContainer)

        bannerAdView = AdView(this).apply {
            adUnitId = "ca-app-pub-5377681981369299/6584173262"
            setAdSize(getAdaptiveBannerAdSize())
        }

        adContainer.removeAllViews()
        adContainer.addView(bannerAdView)

        val adRequest = AdRequest.Builder().build()
        bannerAdView?.loadAd(adRequest)
    }

    // アダプティブバナーのサイズを取得
    private fun getAdaptiveBannerAdSize(): AdSize {
        val displayMetrics = resources.displayMetrics
        val adWidthPixels = displayMetrics.widthPixels.toFloat()
        val density = displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }
}