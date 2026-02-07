package com.chickenyoung.moreclip

import android.app.Activity
import android.widget.FrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * バナー広告の共通ヘルパー
 */
object AdHelper {

    private const val BANNER_AD_UNIT_ID = "ca-app-pub-5377681981369299/6584173262"

    /**
     * バナー広告を読み込んで表示する
     * @param activity 表示するActivity
     * @param container 広告を配置するFrameLayout
     * @return 作成したAdView（onDestroy時にdestroy()を呼ぶこと）
     */
    fun loadBannerAd(activity: Activity, container: FrameLayout): AdView {
        val adView = AdView(activity).apply {
            adUnitId = BANNER_AD_UNIT_ID
            setAdSize(getAdaptiveBannerAdSize(activity))
        }

        container.removeAllViews()
        container.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        return adView
    }

    /**
     * アダプティブバナーのサイズを取得
     */
    private fun getAdaptiveBannerAdSize(activity: Activity): AdSize {
        val displayMetrics = activity.resources.displayMetrics
        val adWidthPixels = displayMetrics.widthPixels.toFloat()
        val density = displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }
}
