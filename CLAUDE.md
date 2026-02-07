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

## リファクタリング時の注意

**過去の失敗**: コードレビュー指摘でActivityLifecycleCallbacksを整理した際、`isFirstLoad`フラグと初回広告表示ロジックを一緒に削除してしまい、起動時広告が動作しなくなった。

### 必須チェック項目
1. **削除するコードの「呼び出し元」を確認** - そのコードを使っている箇所が他にないか
2. **フラグ・状態管理は特に注意** - `isFirst〇〇`, `has〇〇`, `should〇〇` などのフラグは重要なロジックを制御していることが多い
3. **「責務の分離」で機能を壊さない** - コードを移動する場合、移動先で同等の処理が実行されるか確認
4. **リファクタ後は該当機能を必ずテスト** - ビルド成功≠機能正常

### やってはいけないこと
- 「使われていなさそう」という推測だけでコードを削除
- 複数の変更を1コミットにまとめてリファクタ（問題発生時に切り分けが困難）

### 過去のバグ事例
- `getAllMemos()`で履歴を検索した際、定型文が先にヒットして誤動作した。**履歴の操作には`getHistoryMemos()`を使うこと**。テーブルが共通（memos）なので、isTemplateフィルタの意識が必要。

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

### クイックタイルの制約
- コールドスタート時、TileServiceバインドまで1〜2秒「使用不可」が表示される（システム制約、修正不可）

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
| `ime_settings` | IMEの設定 | `ime_close_after_input`, `ime_switch_after_input` |

---

## 広告（Google AdMob）

| 種類 | ID |
|-----|-----|
| App ID | `ca-app-pub-5377681981369299~3584613013` |
| バナー広告 | `ca-app-pub-5377681981369299/6584173262` |
| App Open広告 | `ca-app-pub-5377681981369299/6075663533` |

### App Open Ad 実装ルール

- **広告終了直後に`fetchAd()`を呼ばない** - Frequency capに引っかかる
- **`init`で広告を読み込まない** - 必要な時（Activity表示時）に読み込む
- **読み込み待機にはタイムアウトを設ける** - 現在5秒
- 詳細は `docs/report_aoa_dialog_fix.md` 参照

---

## RecyclerView

- DiffUtilは不使用（全アダプターで`notifyDataSetChanged()`を使用）
- 設定変更後はFragmentの`onResume()`で`adapter.notifyDataSetChanged()`を呼ぶ

---

## Play Console リリース対応

### プライバシーポリシー
- GitHub上の `privacy-policy.md` がPlay Consoleに登録済み
- IME搭載の場合：キーストローク非収集を明記必須
- AdMob使用時：SDK経由のデータ収集を正直に記載（「外部送信なし」と矛盾しないよう注意）

### データセーフティ
- AdMob使用時は「データ収集あり」で申告必須
- 申告データ：おおよその位置情報、デバイスID、アプリのインタラクション、診断情報

---

## リリースフロー

### ブランチ運用と製品版リリース
1. `feature/xxx` で開発・ビルド
2. クローズドテストに提出して動作確認
3. 問題なければ `feature/xxx` を `main` にマージ（プライバシーポリシーURL反映のため）
4. Play Console「リリースを昇格」でクローズドテストのビルドを製品版に昇格（再ビルド不要）

### 注意
- プライバシーポリシー（GitHub上）は `main` ブランチを参照 → マージは製品版昇格前に行う
- クローズドテストで問題が見つかった場合のみ、修正→再ビルド→再提出

---

## 今後の拡張ポイント

- バックアップ/リストア機能
