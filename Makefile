.PHONY: help setup build start-local start-container stop clean test migrate

# デフォルトターゲット
.DEFAULT_GOAL := help

# ヘルプメッセージ
help:
	@echo "Available commands:"
	@echo "  make setup            - 初回セットアップ（Docker起動 + マイグレーション + LocalStack初期化）"
	@echo "  make build            - 全Dockerイメージビルド（アプリ + Flyway）"
	@echo "  make start-local      - ローカル開発起動（インフラコンテナ + アプリローカル実行）"
	@echo "  make start-container  - 完全Docker環境起動（全コンテナ起動）"
	@echo "  make stop             - 停止"
	@echo "  make clean            - 環境完全リセット（ボリューム削除）"
	@echo "  make test             - テスト実行"
	@echo "  make migrate          - データベースマイグレーション実行"

# 初回セットアップ
setup:
	@echo "========================================="
	@echo "ローカル環境セットアップを開始します"
	@echo "========================================="
	@chmod +x scripts/init-localstack.sh
	@docker compose up -d db localstack
	@echo "データベースが起動するまで待機しています..."
	@sleep 5
	@make migrate
	@./scripts/init-localstack.sh
	@echo "セットアップが完了しました"

# 全Dockerイメージビルド
build:
	@echo "全Dockerイメージをビルドしています..."
	@docker build -t handson-app:latest .
	@docker compose build flyway
	@echo "全Dockerイメージのビルドが完了しました"

# ローカル開発起動（インフラコンテナ + アプリローカル実行）
start-local:
	@echo "========================================="
	@echo "ローカル開発環境を起動しています"
	@echo "========================================="
	@docker compose up -d db localstack
	@echo "インフラコンテナ起動完了。アプリケーションをローカルで起動します..."
	@./gradlew bootRun --args='--spring.profiles.active=local'

# アプリケーションをコンテナ上で起動
start-container:
	@echo "========================================="
	@echo "アプリケーションコンテナを起動しています"
	@echo "========================================="
	@docker compose --profile app up -d
	@echo "Docker環境を起動しました"
	@echo "アプリケーション: http://localhost:8080"

# 停止
stop:
	@echo "Docker環境を停止しています..."
	@docker compose down
	@echo "Docker環境を停止しました"

# 環境完全リセット
clean:
	@echo "環境を完全にリセットします（ボリュームも削除）"
	@docker compose down -v
	@echo "環境のリセットが完了しました"

# テスト実行
test:
	@echo "テストを実行しています..."
	@./gradlew test

# マイグレーション実行
migrate:
	@echo "データベースマイグレーションを実行しています..."
	@docker compose --profile flyway up flyway
	@echo "マイグレーションが完了しました"
