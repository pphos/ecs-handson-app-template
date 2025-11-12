# データベースマイグレーションを実行

このコマンドは、Flywayを使用してデータベースマイグレーションを実行します。

## 実行方法

### ローカル開発環境

```bash
make migrate
```

または直接Docker Composeで実行:

```bash
docker compose --profile flyway up flyway
```

### マイグレーションの確認

```bash
# マイグレーション履歴確認
docker exec -it handson-db psql -U handson_migration -d handson -c \
  "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"
```

## トラブルシューティング

### マイグレーション失敗時

**エラーログ確認**:
```bash
docker compose logs flyway
```

**よくあるエラー**:

1. **"Validate failed: Detected applied migration not resolved locally"**
   - 原因: マイグレーションファイルが変更または削除された
   - 対処（開発環境のみ）:
     ```bash
     docker compose down -v
     make setup
     ```
   - 対処（本番環境）: `flyway repair`を検討（慎重に）

2. **"Unable to obtain connection from database"**
   - 原因: データベースが起動していない
   - 対処:
     ```bash
     docker compose up -d db
     # dbの起動を待つ
     make migrate
     ```

3. **"Permission denied for table ..."**
   - 原因: `handson_migration`ユーザーに権限がない
   - 対処: `scripts/init-postgres.sh`を確認、DBを再作成

### マイグレーション履歴のリセット（開発環境のみ）

**警告**: 本番環境では絶対に実行しないでください

```bash
docker compose down -v  # ボリューム削除
make setup              # 環境再構築
```

## 重要な注意事項

- **マイグレーションは`handson_migration`ユーザーで実行されます**（DDL権限あり）
- **アプリケーションは`handson_app`ユーザーで実行されます**（DML権限のみ）
- **マイグレーションファイルは一度適用したら変更・削除しない**
- **本番環境では慎重に実行する**（事前にバックアップ取得）

## マイグレーションファイルの配置場所

- **ローカル/Docker環境**: `migration/sql/`
- **Flywayイメージ**: `/flyway/sql`（イメージに焼き込み済み）
- **命名規則**: `V{version}__{description}.sql`

例: `V1__create_users_table.sql`

## 詳細ドキュメント

- [docs/ARCHITECTURE.md - データベース設計](../../docs/ARCHITECTURE.md#データベース設計)
