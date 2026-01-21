# git add, commit, push を一括実行

変更内容を確認し、適切なコミットメッセージを自動生成してコミット＆プッシュしてください。

## 手順
1. `git status` と `git diff` で変更内容を確認
2. 変更内容に基づいて日本語のコミットメッセージを作成
3. `git add .` でステージング
4. `git commit -m "メッセージ"` でコミット（Co-Authored-By を含める）
5. `git push` でプッシュ（upstream未設定なら `--set-upstream origin ブランチ名`）

## コミットメッセージのルール
- 1行目: 変更の要約（50文字以内）
- 空行
- 本文: 変更内容を箇条書き
- 最後に `Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>`
