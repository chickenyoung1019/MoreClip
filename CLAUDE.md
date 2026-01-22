# Claude Code 開発ガイド

## プロジェクト概要

**アプリ名**: MoreClipボード
**パッケージ名**: `com.chickenyoung.moreclip`
**SDK**: minSdk 24 / targetSdk 36 / compileSdk 36

クリップボードの履歴管理と定型文の保存ができるAndroidアプリ。
専用のIME（入力メソッド）も搭載しており、どのアプリからでも定型文を貼り付け可能。
他アプリからPROCESS_TEXT（テキスト選択メニュー）や共有機能で直接保存もできる。

## 言語設定
- 常に日本語で会話する
- コメントも日本語で記述する
- エラーメッセージの説明も日本語で行う
- ドキュメントも日本語で生成する

## 基本方針
- 非エンジニアによる個人開発プロジェクト
- 複雑な実装より、理解・保守しやすいシンプルなコードを優先

## 開発サイクル（PDCA）

```
Plan: 計画書作成（対話）
  ↓
セッションリセット（コンテキストクリア）
  ↓
Do: 計画書を基に実装（クリーンな状態）
  ↓
Check: ビルド・テスト・レビュー（サブエージェント）
  ↓
Act: 改善・簡略化（サブエージェント）
  ↓
次のPlanへ or 完了
```

### 計画書フォーマット

```markdown
# 実装計画書: [機能名]

## 概要
[1-2文で何をするか]

## 変更ファイル
- ファイル名: 変更内容

## 影響範囲
[他に影響する箇所]

## 懸念点
[あれば]
```

### サブエージェント活用
トークン削減のため以下はサブエージェントで実行：
- `build`: ビルド実行・エラー確認
- `review`: コード設計レビュー
- `simplify`: コード簡略化提案
- `test`: テスト実行

---

## アーキテクチャ

```
MainActivity (タブ切り替え、ヘッダー操作)
├── ViewPager2 + TabLayout
│   ├── [タブ0] ClipboardFragment (履歴)
│   │   └── ClipboardAdapter
│   └── [タブ1] TemplateFragment (定型文)
│       ├── TemplateAdapter (フォルダ一覧)
│       └── FolderContentAdapter (フォルダ内)
├── ClipboardSettingsActivity (履歴の設定)
└── TemplateSettingsActivity (定型文の設定)

ClipboardIMEService (専用キーボード)
└── IMETemplateAdapter

ProcessTextActivity (共有・PROCESS_TEXT受信)
ClipboardTileService (クイックタイル)
```

---

## データベース

スキーマ変更時は**必ずMigrationを書くこと**。書かないとユーザーデータが消失する。

1. `MemoEntity.kt`に列を追加/変更
2. `AppDatabase.kt`のversion番号を上げる
3. Migrationを追加して`.addMigrations()`に登録
4. テスト時は**アプリを再インストールせず**、更新で動作確認

---

## SharedPreferences

| ファイル名 | 用途 | 主なキー |
|-----------|------|---------|
| `app_settings` | 履歴の設定 | `allow_duplicate`, `auto_close`, `move_to_top`, `max_lines` |
| `template_settings` | 定型文の設定 | `allow_duplicate`, `auto_close`, `max_lines`, `folder_order` |
| `ime_settings` | IMEの設定 | `ime_after_input_action` |

---

## 広告（Google AdMob）

| 種類 | ID |
|-----|-----|
| App ID | `ca-app-pub-5377681981369299~3584613013` |
| バナー広告 | `ca-app-pub-5377681981369299/6584173262` |
| App Open広告 | `ca-app-pub-5377681981369299/6075663533` |

---

## 今後の拡張ポイント

- バックアップ/リストア機能
