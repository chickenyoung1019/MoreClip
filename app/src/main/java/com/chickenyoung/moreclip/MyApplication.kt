package com.chickenyoung.moreclip

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.MobileAds

class MyApplication : Application(), Application.ActivityLifecycleCallbacks {

    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("MyApplication", "onCreate called")

        // AdMob初期化
        MobileAds.initialize(this) {}

        // App Open Ad Manager初期化
        appOpenAdManager = AppOpenAdManager(this)

        // ActivityLifecycleCallbacks登録
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // バックグラウンドから復帰時に広告表示（ProcessTextActivityは除外）
        if (currentActivity == null && activity !is ProcessTextActivity) {
            Log.d("MyApplication", "App returned to foreground - showing ad")
            appOpenAdManager.showAdIfAvailable()
        }
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        currentActivity = null
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
