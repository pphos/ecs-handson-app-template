#!/bin/bash
set -e

# PostgreSQL初期化スクリプト
# このスクリプトはPostgreSQLコンテナ初回起動時に自動実行されます
# /docker-entrypoint-initdb.d/ にマウントされたスクリプトはpostgresユーザーで実行されます

echo "=== PostgreSQL初期化スクリプト開始 ==="

# データベース名
DB_NAME="${POSTGRES_DB:-handson}"

# マイグレーション用ユーザー
MIGRATION_PASSWORD="${DB_MIGRATION_PASSWORD:-migration_password}"

# アプリケーション用ユーザー
APP_PASSWORD="${DB_APP_PASSWORD:-app_password}"

echo "データベース: $DB_NAME"
echo "マイグレーションユーザー: handson_migration"
echo "アプリケーションユーザー: handson_app"

# SQLファイルを実行（psql変数として環境変数を渡す）
psql -v ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname "$DB_NAME" \
    -v MIGRATION_PASSWORD="$MIGRATION_PASSWORD" \
    -v APP_PASSWORD="$APP_PASSWORD" \
    -v DB_NAME="$DB_NAME" \
    -f /docker-entrypoint-initdb.d/scripts/init-db-users.sql

echo "=== PostgreSQL初期化スクリプト完了 ==="
