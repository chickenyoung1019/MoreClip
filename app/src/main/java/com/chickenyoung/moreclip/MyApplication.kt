package com.chickenyoung.moreclip

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.MobileAds

class MyApplication : Application(), Application.ActivityLifecycleCallbacks {

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    private var currentActivity: Activity? = null
    private var isInAppTransition = false  // アプリ内遷移フラグ

    override fun onCreate() {
        super.onCreate()
        Log.d("MyApplication", "onCreate called")
        MobileAds.initialize(this) {}
        appOpenAdManager = AppOpenAdManager(this)
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        Log.d("MyApplication", "onActivityStarted: ${activity::class.simpleName}, isInAppTransition: $isInAppTransition")

        // 広告を表示しないActivity（ProcessTextActivityのみ）
        val isProcessText = activity is ProcessTextActivity

        // 広告表示条件: アプリ内遷移でない && currentActivityがnull && ProcessTextActivityでない
        val shouldShowAd = !isInAppTransition
            && currentActivity == null
            && !isProcessText

        if (shouldShowAd) {
            Log.d("MyApplication", "フォアグラウンド復帰 - 広告表示チェック")

            if (activity is MainActivity) {
                // MainActivityの場合は広告閉じた後にダイアログ表示
                appOpenAdManager.showAdIfAvailable(
                    activity = activity,
                    onDismissed = { activity.onAppOpenAdDismissed() }
                )
            } else {
                appOpenAdManager.showAdIfAvailable(activity)
            }
        }

        currentActivity = activity
        isInAppTransition = false  // リセット
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {
        // 別のActivityが開始される前にフラグを立てる
        // (onPausedは新しいActivityのonStartedより先に呼ばれる)
        isInAppTransition = true
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity == currentActivity) {
            currentActivity = null
        }
        // バックグラウンドに移行した場合はフラグをリセット
        // （復帰時に広告を表示するため）
        isInAppTransition = false
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        // pendingActivityのクリア（メモリリーク対策）
        appOpenAdManager.clearPendingActivityIfMatch(activity)
    }
}
