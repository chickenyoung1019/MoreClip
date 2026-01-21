# 既知の問題と対応方針

最終レビュー日: 2026-01-21

このファイルは、コードレビューで発見された問題と対応状況を管理します。
対応したら `[ ]` を `[x]` に変更してください。

---

## 緊急度：高（すぐ直すべき）

### [x] ID-05: IMEのCoroutineScopeリーク ✅ 対応済み
- **ファイル**: `ClipboardIMEService.kt:21`
- **問題**: サービス終了時にCoroutineScopeがキャンセルされていない
- **対応日**: 2026-01-21
- **対応内容**: `onDestroy()`を追加し、`serviceScope.cancel()`を実行

### [x] ID-06: AdViewのメモリリーク ✅ 対応済み
- **ファイル**:
  - `MainActivity.kt`
  - `ClipboardSettingsActivity.kt`
  - `TemplateSettingsActivity.kt`
- **問題**: Activity終了時にAdViewが破棄されていない
- **対応日**: 2026-01-21
- **対応内容**: 各Activityに`onDestroy()`を追加し、`bannerAdView?.destroy()`を実行

### [x] ID-14: データベースマイグレーション ✅ 対応済み
- **ファイル**: `AppDatabase.kt`
- **問題**: `fallbackToDestructiveMigration()`使用でデータ消失リスク
- **対応日**: 2026-01-21
- **対応内容**:
  - `fallbackToDestructiveMigration()`を削除
  - Migrationの書き方をコメントで明記
  - CLAUDE.mdにスキーマ変更手順を追記

---

## 緊急度：中（近いうちに直すべき）

### [ ] ID-01: notifyDataSetChanged()の乱用
- **ファイル**:
  - `ClipboardAdapter.kt` (8箇所)
  - `TemplateAdapter.kt` (8箇所)
  - `FolderContentAdapter.kt` (8箇所)
  - `IMETemplateAdapter.kt` (1箇所)
- **問題**: リスト全体を再描画するため非効率
- **影響**: スクロールのカクつき、ちらつき
- **修正方法**: `DiffUtil`を導入するか、`notifyItemChanged()`等を使用
- **備考**: アイテム数が少ない現状では体感差なし。余裕があれば対応

### [x] ID-03: Deprecated API使用 ✅ 対応済み
- **ファイル**:
  - `MainActivity.kt`
  - `ClipboardSettingsActivity.kt`
  - `TemplateSettingsActivity.kt`
- **問題**:
  - `SYSTEM_UI_FLAG_LIGHT_STATUS_BAR` (API 30で非推奨)
  - `onBackPressed()` (API 33で非推奨)
- **対応日**: 2026-01-21
- **対応内容**:
  - ステータスバー: `WindowInsetsControllerCompat`に変更
  - バックボタン: `OnBackPressedCallback`に変更

### [x] ID-02: 未使用のプロパティ ✅ 対応済み
- **ファイル**: `TemplateFragment.kt`
- **問題**: `backButton: View`が宣言のみで未使用
- **対応日**: 2026-01-21
- **対応内容**: 未使用の`backButton`プロパティを削除

---

## 緊急度：低（余裕があれば）

### [ ] ID-10: バナー広告コードの重複
- **ファイル**:
  - `MainActivity.kt:1356-1383`
  - `ClipboardSettingsActivity.kt:87-111`
  - `TemplateSettingsActivity.kt:80-104`
- **問題**: 同じコードが3箇所にコピペされている
- **影響**: 修正時に3箇所変更が必要
- **修正方法**: `AdHelper.kt`等を作成して共通化

### [ ] ID-11: Adapter選択モードの重複
- **ファイル**:
  - `ClipboardAdapter.kt:127-197`
  - `FolderContentAdapter.kt:124-193`
- **問題**: 選択モードのロジックがほぼ同一
- **影響**: 同じ修正を2箇所に行う必要
- **修正方法**: 共通インターフェースまたは抽象クラスで共通化
- **備考**: CLAUDE.mdの「保守性優先」に反するが、現状動いているなら優先度低

### [ ] ID-12: ダイアログ表示パターンの重複
- **ファイル**: `MainActivity.kt` (多数箇所)
- **問題**: フォルダ選択・作成ダイアログが多数重複
- **影響**: 修正漏れが発生しやすい
- **修正方法**: `DialogHelper.kt`を作成

### [ ] ID-13: MainActivityの肥大化
- **ファイル**: `MainActivity.kt` (1385行)
- **問題**: 1ファイルに多くの責務が集中
- **影響**: 読みづらい、修正しにくい
- **対応方針**:
  - 急いで分割する必要はない
  - 新機能追加時に少しずつ分離していく
  - まずはダイアログ系を`DialogHelper.kt`に移動するのがおすすめ

### [ ] ID-04: Uncheckedキャスト
- **ファイル**:
  - `MainActivity.kt:783, 936, 1149`
  - `TemplateFragment.kt:341`
- **問題**: `as Set<Int>`等の安全性チェックなしキャスト
- **影響**: 稀に`ClassCastException`発生の可能性
- **修正方法**: `as?`で安全なキャストに変更

### [ ] ID-07: ActivityLifecycleCallbacksの二重登録
- **ファイル**:
  - `AppOpenAdManager.kt:23`
  - `MyApplication.kt:25`
- **問題**: 同じイベントに対して2つのコールバック
- **影響**: 予期しない動作の可能性
- **修正方法**: 片方に統一

### [ ] ID-08: DBアクセスの非効率（ループ内個別更新）
- **ファイル**:
  - `MainActivity.kt` (多数箇所)
  - `ProcessTextActivity.kt:48-79`
- **問題**: ループ内で1件ずつ`update()`を呼び出し
- **影響**: 大量データ時にパフォーマンス低下
- **修正方法**: DAOに`@Transaction`付きバッチ処理メソッドを追加

### [ ] ID-09: 広告IDのハードコーディング
- **ファイル**:
  - `MainActivity.kt:1362`
  - `ClipboardSettingsActivity.kt:92`
  - `TemplateSettingsActivity.kt:85`
  - `AppOpenAdManager.kt:34`
- **問題**: 広告ユニットIDがソースコードに直接記載
- **影響**: 管理しにくい（セキュリティリスクは低い）
- **修正方法**: `strings.xml`または`BuildConfig`に移動

### [x] ID-15: 例外のログ出力不足 ✅ 対応済み
- **ファイル**: `TemplateFragment.kt`
- **問題**: JSONパース例外でログ出力なし
- **対応日**: 2026-01-21
- **対応内容**: `Log.e()`を追加してエラー内容を出力

---

## 対応完了した問題

対応したらここに移動してください。

```
### [x] ID-XX: 問題名
- 対応日: YYYY-MM-DD
- 対応内容: 〇〇を修正
```

---

## 参考: CLAUDE.mdとの整合性

| 問題 | CLAUDE.mdとの整合性 |
|-----|-------------------|
| ID-14 (fallback) | CLAUDE.mdに「使用中」と明記済み。開発中は現状維持 |
| ID-10, 11, 12 (重複) | 「保守性優先」に合致。ただし動作中なら急がない |
| ID-13 (肥大化) | 分離は方針に合致。段階的に対応 |
