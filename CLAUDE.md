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

### 目的
1. **推論を結果に変えて品質を上げる**
   - メインの「こうすればいいはず」という推論だけで終わらせない
   - サブエージェントが実際にビルド・テスト・検証を実行し、事実として結果を返す
   - 実行結果に基づいて判断することで、精度と品質が向上する

2. **コンテキスト肥大化による精度低下を防ぐ**
   - 計画書 → セッションリセット後も意図を引き継ぐための「橋渡し」
   - サブエージェント → メインセッションのトークンを消費せず処理を委譲

### 原則
- メインセッションは「判断・対話」に集中
- 重い処理（ビルド、探索、レビュー等）はサブエージェントへ
- 計画書は次セッションの「唯一のコンテキスト」になる前提で書く
- コードを書いたら、自分で確認してから完了とする

### フロー
```
Plan: 設計・計画書作成 → code-architect
  ↓
セッションリセット（コンテキストクリア）
  ↓
Do: 計画書を基に実装（メイン）
  ↓
Check: ビルド検証 → build-validator（実行結果を返す）
       テスト手順作成 → verify-app
       実機/エミュレータ検証 → app-tester（スクショで判断）
  ↓
Act: コード簡素化 → code-simplifier
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

### サブエージェント一覧

| エージェント | 役割 | PDCAフェーズ |
|-------------|------|-------------|
| `code-architect` | 設計・計画書作成 | Plan |
| `build-validator` | ビルド検証 | Check |
| `verify-app` | テスト手順書作成 | Check |
| `app-tester` | 実機/エミュレータで検証 | Check |
| `code-simplifier` | コード簡素化 | Act |
| `Explore`（組込） | ファイル探索 | 随時 |

**呼び出し方**: 自然言語で依頼（例：「ビルド確認して」「実機でテストして」）

---

## adb（実機/エミュレータ操作）

```bash
# パス
C:/Users/toriw/AppData/Local/Android/Sdk/platform-tools/adb.exe

# デバイス確認
adb devices

# アプリ起動
adb shell am start -n com.chickenyoung.moreclip/.MainActivity

# スクリーンショット撮影（exec-outを使う）
adb exec-out screencap -p > screenshot.png
```

app-testerはスクリーンショットを撮影し、画像を見て「正しく動作しているか」を判断できる。

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
