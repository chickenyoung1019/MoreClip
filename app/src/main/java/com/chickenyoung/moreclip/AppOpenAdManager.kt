package com.chickenyoung.moreclip

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.lang.ref.WeakReference
import java.util.Date

class AppOpenAdManager(private val application: Application) {

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var isLoadingAd = false  // 読み込み中フラグ
    private var loadTime: Long = 0

    // 広告読み込み完了後に表示を待っているActivity（WeakReferenceでメモリリーク防止）
    private var pendingActivity: WeakReference<Activity>? = null

    // 広告が閉じられた時のコールバック
    private var onAdDismissedCallback: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())

    private fun fetchAd() {
        if (isAdAvailable() || isLoadingAd) return

        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()
        val adUnitId = "ca-app-pub-5377681981369299/6075663533"

        AppOpenAd.load(
            application,
            adUnitId,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTime = Date().time
                    isLoadingAd = false
                    Log.d("AppOpenAd", "広告読み込み成功")

                    // 待機中のActivityがあれば広告を表示
                    pendingActivity?.get()?.let { activity ->
                        showAdIfAvailable(activity)
                    }
                    pendingActivity = null
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isLoadingAd = false
                    Log.e("AppOpenAd", "広告読み込み失敗: ${error.message}")

                    // 読み込み失敗時はコールバック実行（ダイアログ表示のため）
                    if (pendingActivity?.get() != null) {
                        invokeAndClearCallback()
                    }
                    pendingActivity = null
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        val wasLoadTimeLessThanNHoursAgo = Date().time - loadTime < 3600000 * 4
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo
    }

    /**
     * 広告を表示
     * @param activity 広告を表示するActivity
     * @param onDismissed 広告が閉じられた時のコールバック
     */
    fun showAdIfAvailable(
        activity: Activity,
        onDismissed: (() -> Unit)? = null
    ) {
        // コールバックを保存
        if (onDismissed != null) {
            onAdDismissedCallback = onDismissed
        }

        if (!isShowingAd && isAdAvailable()) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AppOpenAd", "広告を閉じた")
                    clearAdAndInvokeCallback()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e("AppOpenAd", "広告表示失敗: ${error.message}")
                    clearAdAndInvokeCallback()
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                    Log.d("AppOpenAd", "広告を表示")
                }
            }

            appOpenAd?.show(activity)
        } else if (appOpenAd == null) {
            // 広告がなければ読み込みを開始（または読み込み中なら待機のみ）
            if (!isLoadingAd) {
                Log.d("AppOpenAd", "広告読み込み開始 - 完了を待機")
                fetchAd()
            } else {
                Log.d("AppOpenAd", "広告読み込み中 - 完了を待機")
            }
            pendingActivity = WeakReference(activity)
            startTimeoutForPendingActivity(activity)
        } else {
            // 広告が期限切れ（4時間経過）
            Log.d("AppOpenAd", "広告が利用不可（期限切れ）")
            invokeAndClearCallback()
        }
    }

    /** タイムアウト処理（5秒） */
    private fun startTimeoutForPendingActivity(activity: Activity) {
        handler.postDelayed({
            if (pendingActivity?.get() == activity) {
                Log.d("AppOpenAd", "広告読み込みタイムアウト")
                pendingActivity = null
                invokeAndClearCallback()
            }
        }, 5000)
    }

    /** 広告終了時の共通処理（広告クリア + コールバック実行） */
    private fun clearAdAndInvokeCallback() {
        appOpenAd = null
        isShowingAd = false
        // fetchAd() は呼ばない（Frequency cap対策）
        invokeAndClearCallback()
    }

    /** コールバック実行とクリア */
    private fun invokeAndClearCallback() {
        onAdDismissedCallback?.invoke()
        onAdDismissedCallback = null
    }

    /**
     * Activity破棄時にpendingActivityをクリア（メモリリーク対策）
     */
    fun clearPendingActivityIfMatch(activity: Activity) {
        if (pendingActivity?.get() == activity) {
            pendingActivity = null
        }
    }
}
