# 手動リリース手順 (GitHub Actions)

このリポジトリは GitHub Actions の `Manual Build & Release` ワークフローで手動リリースできます。

## 使い方

1. GitHub 上で **Actions** タブを開きます。
2. **Manual Build & Release** を選択します。
3. **Run workflow** をクリックし、以下の入力を設定します。
   - `tag`: 作成するタグ名 (例: `v1.2.3`)
   - `release_name`: リリース名 (未入力の場合はタグ名が使われます)
   - `release_body`: リリース本文 (任意)
   - `prerelease`: プレリリースにする場合は `true`
   - `generate_release_notes`: GitHub の自動リリースノートを使う場合は `true`
4. 実行後、`build/libs/*.jar` がリリースに添付されます。

## 仕様

- ビルドは `./gradlew clean build` で実行されます。
- リリース作成には `contents: write` 権限が必要です。
