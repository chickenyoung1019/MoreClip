package com.chickenyoung.moreclip

import android.app.Activity
import android.app.Application
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(private val application: Application) {

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var loadTime: Long = 0

    init {
        fetchAd()
    }

    // 広告を読み込む
    private fun fetchAd() {
        if (isAdAvailable()) {
            return
        }

        val adRequest = AdRequest.Builder().build()
        val adUnitId = "ca-app-pub-5377681981369299/6075663533" // App Open Ads 本番ID

        AppOpenAd.load(
            application,
            adUnitId,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTime = Date().time
                    Log.d("AppOpenAd", "広告読み込み成功")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    Log.e("AppOpenAd", "広告読み込み失敗: ${error.message}")
                }
            }
        )
    }

    // 広告が利用可能かチェック
    private fun isAdAvailable(): Boolean {
        // 広告が4時間以上前に読み込まれた場合は無効
        val wasLoadTimeLessThanNHoursAgo = Date().time - loadTime < 3600000 * 4
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo
    }

    /**
     * 広告を表示
     * @param activity 広告を表示するActivity
     */
    fun showAdIfAvailable(activity: Activity) {
        if (!isShowingAd && isAdAvailable()) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    fetchAd() // 次の広告を読み込む
                    Log.d("AppOpenAd", "広告を閉じた")
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    fetchAd()
                    Log.e("AppOpenAd", "広告表示失敗: ${error.message}")
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                    Log.d("AppOpenAd", "広告を表示")
                }
            }

            appOpenAd?.show(activity)
        } else {
            Log.d("AppOpenAd", "広告が利用不可")
            fetchAd()
        }
    }
}
