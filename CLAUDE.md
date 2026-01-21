# Claude Code 開発ガイド

## プロジェクト概要

**アプリ名**: MoreClipボード
**パッケージ名**: `com.chickenyoung.moreclip`
**バージョン**: 1.3 (versionCode: 4)
**SDK**: minSdk 24 / targetSdk 36 / compileSdk 36

クリップボードの履歴管理と定型文の保存ができるAndroidアプリ。
専用のIME（入力メソッド）も搭載しており、どのアプリからでも定型文を貼り付け可能。

## 言語設定
- 常に日本語で会話する
- コメントも日本語で記述する
- エラーメッセージの説明も日本語で行う
- ドキュメントも日本語で生成する

## 基本方針
- **保守性優先**: 複雑な実装より、非エンジニアが理解・保守しやすいコードを重視
- **段階的実装**: 大きな変更は小分けにして、1ファイルずつ確実に進める
- **MVP思考**: 完璧を目指さず、動くものを早く作ってフィードバックで改善

---

## アーキテクチャ

### 全体構成図
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

### Fragment vs Activity
- **Fragment**: タブ内での画面切り替え（自然な遷移）
- **Activity**: 独立した画面（編集、設定）

### Adapter設計
- 用途ごとに分離（ClipboardAdapter, TemplateAdapter, FolderContentAdapter, IMETemplateAdapter）
- 選択モード・並び替えモードは各Adapterで実装

---

## ファイル構成

```
app/src/main/java/com/chickenyoung/moreclip/
│
├── [Activity]
│   ├── MainActivity.kt           # メイン画面（タブ、ヘッダー操作）
│   ├── EditMemoActivity.kt       # メモ編集画面
│   ├── ClipboardSettingsActivity.kt  # 履歴の設定
│   ├── TemplateSettingsActivity.kt   # 定型文の設定
│   └── ProcessTextActivity.kt    # 共有/PROCESS_TEXT受信
│
├── [Fragment]
│   ├── ClipboardFragment.kt      # 履歴タブ
│   └── TemplateFragment.kt       # 定型文タブ
│
├── [Adapter]
│   ├── ClipboardAdapter.kt       # 履歴リスト用
│   ├── TemplateAdapter.kt        # 定型文+フォルダ一覧用
│   ├── FolderContentAdapter.kt   # フォルダ内コンテンツ用
│   ├── IMETemplateAdapter.kt     # IME用（シンプル）
│   └── ViewPagerAdapter.kt       # ViewPager2用
│
├── [データベース]
│   ├── AppDatabase.kt            # Room Database (version: 5)
│   ├── MemoDao.kt                # データアクセス
│   └── MemoEntity.kt             # エンティティ
│
├── [モデル]
│   └── TemplateItem.kt           # sealed class（Folder/Template）
│
├── [サービス]
│   ├── ClipboardIMEService.kt    # 専用IME（入力メソッド）
│   └── ClipboardTileService.kt   # クイックタイル
│
├── [ヘルパー]
│   ├── AdHelper.kt               # バナー広告ヘルパー
│   └── DialogHelper.kt           # ダイアログ表示ヘルパー
│
└── [広告]
    ├── MyApplication.kt          # Application初期化
    └── AppOpenAdManager.kt       # App Open Ads管理
```

---

## データベース

### MemoEntity
```kotlin
@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isTemplate: Boolean = false,  // false:履歴, true:定型文
    val folder: String? = null,       // フォルダ名（nullはフォルダなし）
    val displayOrder: Int = 0         // 並び替え用順序
)
```

### 主要なDAOメソッド
| メソッド | 用途 |
|---------|------|
| `getHistoryMemos()` | 履歴のみ取得 |
| `getAllTemplates()` | 全定型文をdisplayOrder順で取得 |
| `getTemplatesByFolder(name)` | 特定フォルダ内の定型文取得 |
| `getTemplatesWithoutFolder()` | フォルダなしの定型文取得 |
| `getFolders()` | フォルダ一覧取得（重複なし） |

### データベース変更時の注意（重要）
スキーマ変更時は**必ずMigrationを書くこと**。書かないとユーザーデータが消失する。

1. `MemoEntity.kt`に列を追加/変更
2. `AppDatabase.kt`のversion番号を上げる（例: 5 → 6）
3. Migrationを追加:
```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE memos ADD COLUMN newColumn TEXT DEFAULT ''")
    }
}
```
4. `.addMigrations(MIGRATION_5_6)`を追加
5. テスト時は**アプリを再インストールせず**、更新で動作確認

---

## SharedPreferences

### 設定ファイル一覧
| ファイル名 | 用途 | 主なキー |
|-----------|------|---------|
| `app_settings` | 履歴の設定 | `allow_duplicate`, `auto_close`, `move_to_top`, `max_lines` |
| `template_settings` | 定型文の設定 | `allow_duplicate`, `auto_close`, `max_lines`, `folder_order` |
| `ime_settings` | IMEの設定 | `ime_after_input_action` |

### 参照方法
```kotlin
// 履歴設定
val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

// 定型文設定
val prefs = getSharedPreferences("template_settings", Context.MODE_PRIVATE)

// IME設定
val prefs = getSharedPreferences("ime_settings", Context.MODE_PRIVATE)
```

### フォルダ順序の保存（JSON形式）
```kotlin
// 保存
val jsonArray = JSONArray(folderOrder)
prefs.edit().putString("folder_order", jsonArray.toString()).apply()

// 読み込み
val json = prefs.getString("folder_order", null)
val jsonArray = JSONArray(json)
```

---

## displayOrder管理（並び替え機能）

### 基本ルール
- 新しいアイテムは`displayOrder = 0`で追加
- 他のアイテムは`displayOrder + 1`でずらす
- 並び替え保存時は位置に応じてdisplayOrderを再設定

### フォルダ順序
- SharedPreferencesの`folder_order`にJSONArrayで保存
- 新しいフォルダは末尾に追加される

---

## モード切り替え

### 通常モード → 選択モード
```
長押し → enterSelectMode() → 自動で該当アイテム選択
├── タブ切り替え禁止
├── ヘッダーUIを選択用に変更
└── 0件になったら自動解除
```

### 通常モード → 並び替えモード
```
メニュー「並び替え」→ enterReorderMode()
├── タブ切り替え禁止
├── ItemTouchHelperでドラッグ有効化
└── 完了/キャンセルで終了
```

### フォルダモード
```
定型文タブ → フォルダをタップ → フォルダ内表示
├── TemplateAdapter → FolderContentAdapter に切り替え
├── ヘッダーに←ボタン表示
└── タイトルをフォルダ名に変更
```

---

## IME（入力メソッド）

### ClipboardIMEService
- 専用キーボードとして動作
- 定型文タブと履歴タブを切り替え可能
- フォルダ階層表示対応

### 入力後のアクション（ime_after_input_action）
| 値 | 動作 |
|---|-----|
| `switch` | 前のキーボードに切り替え |
| `close` | キーボードを閉じる |
| `stay` | そのまま（何もしない） |

---

## 広告（Google AdMob）

### 広告ID
| 種類 | ID |
|-----|-----|
| App ID | `ca-app-pub-5377681981369299~3584613013` |
| バナー広告 | `ca-app-pub-5377681981369299/6584173262` |
| App Open広告 | `ca-app-pub-5377681981369299/6075663533` |

### 表示場所
- バナー広告: MainActivity、設定画面
- App Open広告: アプリ起動時（ProcessTextActivity除外）

---

## よくある実装パターン

### Fragment取得
```kotlin
// ViewPager2のFragment取得
val clipboardFragment = supportFragmentManager.findFragmentByTag("f0") as? ClipboardFragment
val templateFragment = supportFragmentManager.findFragmentByTag("f1") as? TemplateFragment
```

### 選択モード判定（TemplateFragment）
```kotlin
if (currentFolder != null) {
    // フォルダ内: folderContentAdapterを使用
} else {
    // フォルダ一覧: adapterを使用
}
```

### Adapterでのモード切り替え
```kotlin
when {
    isReorderMode -> { /* 並び替えモード */ }
    isSelectMode -> { /* 選択モード */ }
    else -> { /* 通常モード */ }
}
```

---

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

### 決断パターン
| 状況 | 方針 |
|-----|------|
| 複雑 vs 保守性 | 常に保守性を優先 |
| 実装順序 | 簡単 or 基盤となるもの優先 |
| 不明点 | 非エンジニア向けに具体例で説明 |

---

## 命名規約

| 種類 | 規約 | 例 |
|-----|------|-----|
| Activity | 機能名 + Activity | `ClipboardSettingsActivity` |
| Fragment | 機能名 + Fragment | `TemplateFragment` |
| Adapter | 用途 + Adapter | `FolderContentAdapter` |
| Service | 機能名 + Service | `ClipboardIMEService` |

---

## 現在の機能一覧

### 履歴タブ
- クリップボードへのコピー
- 編集・削除
- 選択モード（複数選択・削除）
- 並び替えモード（ドラッグ&ドロップ）
- 定型文への追加
- 検索

### 定型文タブ
- フォルダ管理（作成・名前変更・削除）
- フォルダ内表示
- 選択モード（フォルダ+定型文）
- 並び替えモード（フォルダ順序も保存）
- 移動機能
- 検索

### IME
- 定型文/履歴タブ切り替え
- フォルダ階層表示
- バックスペース機能
- 入力後アクション設定

### その他
- クイックタイル
- PROCESS_TEXT / SEND intent対応
- App Open広告

---

## 今後の拡張ポイント

- IME設定画面（現在は未実装）
- バックアップ/リストア機能
- タグ機能
- 統計/使用頻度表示
