# 実装計画書: App Open Ad とダイアログ表示の修正

## 概要

ハイブリッド方式で、ローディングUIなしで広告読み込み完了後に自動表示する。広告を閉じたらIMEダイアログを表示する。

## 現状の問題

| 問題 | 原因 | 深刻度 |
|------|------|--------|
| コールドスタート時に広告が表示されない | `isFirstLoad`による自動表示ロジックが削除された（コミット`d6fbd5a`） | 高 |
| IMEダイアログが表示されない | 原因未確定（広告との競合の可能性） | 高 |
| アプリ内遷移（設定画面）で広告が表示される | `onActivityStopped`で`currentActivity = null`にするため誤検出 | 高 |

## 期待する動作

| シナリオ | 動作 |
|---------|------|
| コールドスタート | 広告読み込み完了後に表示 → 閉じたらダイアログ表示 |
| バックグラウンド復帰 | 広告を表示（現状OK） |
| アプリ内遷移（設定画面） | 広告を表示しない |
| ProcessTextActivity/共有 | 広告を表示しない |
| IMEダイアログ | 「今後表示しない」にチェックしない限り常に表示（広告の有無に関係なく） |
| 広告の表示間隔 | AdMobコンソールで設定 |

## 修正方針

**ハイブリッド方式**:
- ローディングUIなし
- MainActivity は即座に表示
- 広告読み込み完了後に自動表示
- 広告を閉じたらダイアログ表示

**アプリ内遷移の判定**:
- `isInAppTransition`フラグを使用
- `onActivityPaused`でフラグを立て、`onActivityStarted`でリセット

---

## 変更ファイルと修正内容

### 1. AppOpenAdManager.kt

```kotlin
package com.chickenyoung.moreclip

import android.app.Activity
import android.app.Application
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
    private var loadTime: Long = 0

    // 初回起動フラグ
    private var isFirstLaunch = true

    // 広告読み込み完了後に表示を待っているActivity（WeakReferenceでメモリリーク防止）
    private var pendingActivity: WeakReference<Activity>? = null

    // 広告が閉じられた時のコールバック
    private var onAdDismissedCallback: (() -> Unit)? = null

    init {
        fetchAd()
    }

    private fun fetchAd() {
        if (isAdAvailable()) return

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
                    Log.d("AppOpenAd", "広告読み込み成功")

                    // 初回起動時で待機中のActivityがあれば広告を表示
                    if (isFirstLaunch) {
                        pendingActivity?.get()?.let { activity ->
                            showAdIfAvailable(activity)
                        }
                        pendingActivity = null
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    Log.e("AppOpenAd", "広告読み込み失敗: ${error.message}")

                    // 読み込み失敗時もコールバック実行（ダイアログ表示のため）
                    if (isFirstLaunch) {
                        onAdDismissedCallback?.invoke()
                        onAdDismissedCallback = null
                        isFirstLaunch = false
                        pendingActivity = null
                    }
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
     * @param waitIfLoading 読み込み中なら待機するか（初回起動時true）
     * @param onDismissed 広告が閉じられた時のコールバック
     */
    fun showAdIfAvailable(
        activity: Activity,
        waitIfLoading: Boolean = false,
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
                    isFirstLaunch = false
                    fetchAd()
                    Log.d("AppOpenAd", "広告を閉じた")

                    onAdDismissedCallback?.invoke()
                    onAdDismissedCallback = null
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    isFirstLaunch = false
                    fetchAd()
                    Log.e("AppOpenAd", "広告表示失敗: ${error.message}")

                    onAdDismissedCallback?.invoke()
                    onAdDismissedCallback = null
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                    Log.d("AppOpenAd", "広告を表示")
                }
            }

            appOpenAd?.show(activity)
        } else if (waitIfLoading && isFirstLaunch && appOpenAd == null) {
            // 初回起動で広告読み込み中なら、読み込み完了を待つ
            Log.d("AppOpenAd", "初回起動 - 広告読み込み完了を待機")
            pendingActivity = WeakReference(activity)
        } else {
            Log.d("AppOpenAd", "広告が利用不可")
            fetchAd()

            // 広告なしの場合もコールバック実行
            onAdDismissedCallback?.invoke()
            onAdDismissedCallback = null
            if (isFirstLaunch) {
                isFirstLaunch = false
            }
        }
    }

    /**
     * Activity破棄時にpendingActivityをクリア（メモリリーク対策）
     */
    fun clearPendingActivityIfMatch(activity: Activity) {
        if (pendingActivity?.get() == activity) {
            pendingActivity = null
            if (isFirstLaunch) {
                isFirstLaunch = false
            }
        }
    }
}
```

### 2. MyApplication.kt

```kotlin
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
                    waitIfLoading = true,
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
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        // pendingActivityのクリア（メモリリーク対策）
        appOpenAdManager.clearPendingActivityIfMatch(activity)
    }
}
```

### 3. MainActivity.kt

**変更箇所のみ記載**

```kotlin
// onCreate() から showFirstLaunchDialog() の呼び出しを削除

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // ... 既存の初期化処理 ...

    // バナー広告読み込み
    loadBannerAd()

    // showFirstLaunchDialog()  ← この行を削除

    // 戻るボタンの処理
    // ...
}

/**
 * App Open Ad が閉じられた（または表示されなかった）時に呼ばれる
 * MyApplication から呼び出される
 */
fun onAppOpenAdDismissed() {
    Log.d("MainActivity", "onAppOpenAdDismissed called")
    showFirstLaunchDialog()
}

// showFirstLaunchDialog() はそのまま維持（変更なし）
```

---

## データフロー

### コールドスタート時

```
1. MyApplication.onCreate()
   → AppOpenAdManager.init() → fetchAd()開始（非同期）
   → registerActivityLifecycleCallbacks()

2. MainActivity.onCreate() → UI初期化

3. MyApplication.onActivityStarted(MainActivity)
   → isInAppTransition = false, currentActivity = null
   → shouldShowAd = true
   → showAdIfAvailable(waitIfLoading=true, onDismissed=ダイアログ表示)
   → 広告未読み込み → pendingActivity = WeakReference(MainActivity)

4. AppOpenAdManager.onAdLoaded()
   → isFirstLaunch && pendingActivity != null → true
   → showAdIfAvailable(pendingActivity.get()) → 広告表示

5. ユーザーが広告を閉じる
   → onAdDismissedFullScreenContent()
   → onDismissed() → MainActivity.onAppOpenAdDismissed()
   → showFirstLaunchDialog() → ダイアログ表示
```

### アプリ内遷移時（MainActivity → 設定 → MainActivity）

```
1. MainActivity → SettingsActivity
   → onActivityPaused(MainActivity) → isInAppTransition = true
   → onActivityStarted(SettingsActivity) → isInAppTransition = true → 広告表示しない
   → isInAppTransition = false（リセット）
   → currentActivity = SettingsActivity

2. SettingsActivity → MainActivity（戻る）
   → onActivityPaused(SettingsActivity) → isInAppTransition = true
   → onActivityStarted(MainActivity) → isInAppTransition = true → 広告表示しない
   → isInAppTransition = false（リセット）
```

### バックグラウンド復帰時

```
1. ホームボタン押下
   → onActivityPaused(MainActivity) → isInAppTransition = true
   → onActivityStopped(MainActivity) → currentActivity = null

2. アプリに戻る
   → onActivityStarted(MainActivity)
   → isInAppTransition = true だが、onPausedからonStoppedを経由しているので
     実際にはonStoppedでcurrentActivityがnullになり、
     新しいonStartedが呼ばれる時にはisInAppTransitionはリセットされている？
```

**注意**: バックグラウンド復帰時の`isInAppTransition`の挙動を要確認。

**追加検討**: `onActivityStopped`で`isInAppTransition = false`にリセットする必要があるかもしれない。

---

## テスト項目

### 基本動作

| # | テスト内容 | 期待結果 |
|---|-----------|----------|
| 1 | コールドスタート | 広告が表示される |
| 2 | 広告を閉じる | IMEダイアログが表示される |
| 3 | ダイアログで「今後表示しない」にチェック → 再起動 | ダイアログが表示されない |
| 4 | バックグラウンド復帰（30秒以上後） | 広告が表示される |

### 除外ケース

| # | テスト内容 | 期待結果 |
|---|-----------|----------|
| 5 | MainActivity → 設定 → 戻る | 広告が表示されない |
| 6 | ProcessTextActivity（テキスト選択→共有） | 広告が表示されない |

### エッジケース

| # | テスト内容 | 期待結果 |
|---|-----------|----------|
| 7 | 広告読み込み失敗時 | ダイアログのみ表示される |
| 8 | 高速な画面切り替え | クラッシュしない |

---

## 懸念点と対策

| 懸念点 | 対策 |
|--------|------|
| pendingActivityのメモリリーク | WeakReference使用 + onActivityDestroyedでクリア |
| バックグラウンド復帰時のisInAppTransitionの挙動 | onActivityStoppedでリセットを検討 |
| 複数Activityの同時起動 | 現状の実装で基本ケースはカバー、問題発生時に再検討 |

---

## 実装順序

1. **Phase 1**: AppOpenAdManager.kt の修正
   - `isFirstLaunch`フラグ追加
   - `pendingActivity`（WeakReference）追加
   - `onAdDismissedCallback`追加
   - `clearPendingActivityIfMatch()`追加

2. **Phase 2**: MyApplication.kt の修正
   - `isInAppTransition`フラグ追加
   - `onActivityPaused`でフラグ設定
   - `onActivityStarted`の判定ロジック変更
   - `onActivityDestroyed`でクリア処理追加

3. **Phase 3**: MainActivity.kt の修正
   - `onAppOpenAdDismissed()`メソッド追加
   - `onCreate()`から`showFirstLaunchDialog()`呼び出しを削除

4. **Phase 4**: ビルド検証
   - build-validatorでビルド確認

5. **Phase 5**: 動作テスト
   - app-testerで全テスト項目を確認

---

## 参考情報

### 関連コミット

- `ecf8a34`: App Open Ads初回実装（isFirstLoadあり、動作していた）
- `eb94794`: ProcessTextActivity除外を追加
- `d6fbd5a`: ActivityLifecycleCallbacks二重登録解消（isFirstLoad削除、問題発生）

### 関連ファイル

- `app/src/main/java/com/chickenyoung/moreclip/AppOpenAdManager.kt`
- `app/src/main/java/com/chickenyoung/moreclip/MyApplication.kt`
- `app/src/main/java/com/chickenyoung/moreclip/MainActivity.kt`
