# Claude Code 開発ガイド

## 言語設定
- 常に日本語で会話する
- コメントも日本語で記述する
- エラーメッセージの説明も日本語で行う
- ドキュメントも日本語で生成する

## 基本方針
- **保守性優先**: 複雑な実装より、非エンジニアが理解・保守しやすいコードを重視
- **段階的実装**: 大きな変更は小分けにして、1ファイルずつ確実に進める
- **MVP思考**: 完璧を目指さず、動くものを早く作ってフィードバックで改善

## 開発の進め方

### ステップバイステップ方式
1. **UI確認**: 実装前に画面イメージを確認
2. **実装順序確認**: 何から作るか、依存関係を整理
3. **小分け作業**: 1つずつ確実に進める
4. **完了確認**: 各ステップで「OK」確認後、次へ

### ファイル編集のルール
- ファイル全体を提示せず「この部分を修正」と具体的に指示
- エラー時は該当ファイル全体を共有
- 複数ファイル変更は段階的に（1ファイル→確認→次）

### コミュニケーション
- 簡潔な説明（コンテキスト節約）
- 不要な確認は削減
- 非エンジニア向けに図解・具体例で説明

## 決断パターン

### 複雑 vs 保守性
→ **常に保守性を優先**
例: ファイル統合より分離（ClipboardとTemplateを別ファイル）

### 実装順序
→ **簡単なもの or 基盤となるもの優先**
例: 選択削除実装後、個別削除を追加

### 不明点の説明
→ **非エンジニア向けに具体例で**
例: コールバック方式 vs 直接呼び出し方式の比較図

## アーキテクチャ原則

### Fragment vs Activity
- Fragment: タブ内での画面切り替え（自然な遷移）
- Activity: 独立した画面（編集、設定）

### Adapter設計
- 用途ごとに分離（ClipboardAdapter, TemplateAdapter, FolderContentAdapter）
- 選択モードは各Adapterで実装

### 設定管理
- SharedPreferences使用
- 機能ごとに分離（app_settings, template_settings）
- デバッグしやすさ優先

## 実装時の注意点

### データベース変更
- version番号を上げる
- fallbackToDestructiveMigration()使用
- テスト時はアプリ再インストール

### 選択モード実装
- 長押しで開始 + 自動選択
- タップで選択/解除
- ゴミ箱ボタンで削除
- 全画面で統一されたUX

### タブ切り替え
- 選択モード解除
- ゴミ箱非表示
- Fragment状態リセット

## コード規約

### 命名
- Activity: 機能名 + Activity（例: ClipboardSettingsActivity）
- Fragment: 機能名 + Fragment
- Adapter: 用途 + Adapter

### ファイル構成
```
app/src/main/java/com/example/myclipboardapp/
├── MainActivity.kt
├── ClipboardFragment.kt
├── TemplateFragment.kt
├── ClipboardAdapter.kt
├── TemplateAdapter.kt
├── FolderContentAdapter.kt
├── EditMemoActivity.kt
├── ClipboardSettingsActivity.kt
├── TemplateSettingsActivity.kt
├── MemoEntity.kt
├── MemoDao.kt
├── AppDatabase.kt
├── TemplateItem.kt (sealed class)
└── ViewPagerAdapter.kt
```

## よくある問題と解決

### Fragment取得
```kotlin
// ViewPager2のFragment取得
supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment
supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
```

### 選択モード判定
```kotlin
// フォルダ内 vs フォルダ外
if (currentFolder != null) {
    // フォルダ内処理
} else {
    // フォルダ外処理
}
```

### 設定参照
```kotlin
// 履歴
val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
// 定型文
val prefs = getSharedPreferences("template_settings", Context.MODE_PRIVATE)
```

## 次回実装予定
1. 画面間移動（履歴⇔定型文⇔フォルダ内）
2. 並び替え・検索