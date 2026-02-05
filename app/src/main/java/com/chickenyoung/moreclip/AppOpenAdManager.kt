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
                    pendingActivity?.get()?.let {
                        onAdDismissedCallback?.invoke()
                        onAdDismissedCallback = null
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
                    appOpenAd = null
                    isShowingAd = false
                    Log.d("AppOpenAd", "広告を閉じた")
                    // fetchAd() は呼ばない（Frequency cap対策）

                    onAdDismissedCallback?.invoke()
                    onAdDismissedCallback = null
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    Log.e("AppOpenAd", "広告表示失敗: ${error.message}")
                    // fetchAd() は呼ばない（Frequency cap対策）

                    onAdDismissedCallback?.invoke()
                    onAdDismissedCallback = null
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                    Log.d("AppOpenAd", "広告を表示")
                }
            }

            appOpenAd?.show(activity)
        } else if (!isLoadingAd && appOpenAd == null) {
            // 広告がなく、読み込み中でもない場合は読み込みを開始して待機
            Log.d("AppOpenAd", "広告読み込み開始 - 完了を待機")
            pendingActivity = WeakReference(activity)
            fetchAd()

            // タイムアウト（5秒）- 読み込みが遅い場合はスキップ
            handler.postDelayed({
                if (pendingActivity?.get() == activity) {
                    Log.d("AppOpenAd", "広告読み込みタイムアウト")
                    pendingActivity = null
                    onAdDismissedCallback?.invoke()
                    onAdDismissedCallback = null
                }
            }, 5000)
        } else if (isLoadingAd) {
            // 読み込み中なら待機
            Log.d("AppOpenAd", "広告読み込み中 - 完了を待機")
            pendingActivity = WeakReference(activity)

            // タイムアウト（5秒）
            handler.postDelayed({
                if (pendingActivity?.get() == activity) {
                    Log.d("AppOpenAd", "広告読み込みタイムアウト")
                    pendingActivity = null
                    onAdDismissedCallback?.invoke()
                    onAdDismissedCallback = null
                }
            }, 5000)
        } else {
            Log.d("AppOpenAd", "広告が利用不可")
            onAdDismissedCallback?.invoke()
            onAdDismissedCallback = null
        }
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
