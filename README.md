# ECS デプロイ用 Spring Boot サンプルアプリケーション

## このアプリケーションについて
下記の Zenn Book で利用するサンプルアプリケーションです。
画像とメモを組み合わせて保存・管理するものになっています。

https://zenn.dev/y___u/books/aws-ecs-hands-on


**主な機能:**
- ユーザー登録・ログイン認証
- 画像アップロード（タイトル・説明付き）
- 画像メモ一覧表示（ページング機能付き）
- 画像メモ詳細表示
- 画像メモ削除

## 前提条件

- **Docker**
- **Git**

> **JavaやGradleのインストールは不要です**
> すべてDockerコンテナ内で実行されます。

## クイックスタート

### 1. リポジトリのクローン

```bash
git clone <repository-url>
cd ecs-handson-app-template
```

### 2. アプリケーションの起動（3ステップ）

```bash
# ステップ1: インフラ起動（PostgreSQL + LocalStack）
docker compose up -d

# ステップ2: データベースマイグレーション実行
docker compose --profile flyway up flyway

# ステップ3: アプリケーション起動
docker compose --profile app up -d
```

> **初回起動時:**
> - Dockerイメージのダウンロードとビルドが行われます（3-5分かかります）
> - LocalStackの初期化スクリプトが自動的にS3バケットを作成します
> - `docker compose logs -f app` でアプリケーションログを確認できます
> - `docker compose logs -f localstack` でLocalStackの初期化ログを確認できます

### 3. 動作確認

1. ブラウザで http://localhost:8080 にアクセス
2. 「新規登録」からユーザーを作成（任意のユーザー名・パスワード）
3. ログイン後、「新規作成」から画像メモを作成してみましょう
   - タイトル、説明、画像ファイルを入力
   - 「作成」ボタンで保存
4. 一覧画面で作成した画像メモが表示されることを確認

### 4. 停止方法

```bash
docker compose down
```

完全にリセットしたい場合:
```bash
docker compose down -v  # ボリューム削除
```

## 技術スタック

**フロントエンド:**
- Thymeleaf（テンプレートエンジン）

**バックエンド:**
- Java 25 + Spring Boot 3.5.7
- Spring Security（ログイン認証）
- Spring Data JPA / Hibernate（データベースアクセス）
- PostgreSQL 17（データベース）

**インフラ:**
- Docker / Docker Compose（コンテナ化とローカル開発環境）
- Spring Cloud AWS（S3連携）
- LocalStack（ローカルAWS環境）
- Flyway（データベースマイグレーション - 公式Dockerイメージで分離実行）
- Testcontainers（統合テスト）
- Gradle (Kotlin DSL)（ビルドツール）

**本番環境（ハンズオンで構築）:**
- AWS ECS/Fargate
- Amazon RDS（PostgreSQL）
- Amazon S3

## トラブルシューティング

### Q. アプリケーションが起動しない

```bash
# ログを確認
docker compose logs app

# コンテナの状態確認
docker compose ps
```

### Q. ポート8080が使用中と表示される

```bash
# ポートを使用しているプロセスを確認
lsof -ti:8080

# 停止（Macの場合）
lsof -ti:8080 | xargs kill -9
```

### Q. 画像アップロードでエラーが出る

LocalStackのS3バケットが作成されているか確認:
```bash
docker exec localstack awslocal s3 ls
# handson-app-bucket が表示されればOK
```

表示されない場合、LocalStackの初期化ログを確認:
```bash
docker compose logs localstack
# "LocalStackの初期化が完了しました" が表示されているか確認
```

### Q. 環境を完全にリセットしたい

```bash
docker compose down -v
docker compose up -d
docker compose --profile flyway up flyway
docker compose --profile app up -d
```

### Q. データベースマイグレーションエラー

Flywayマイグレーション履歴を確認:
```bash
docker exec -it postgres psql -U app -d handson -c "SELECT * FROM flyway_schema_history;"
```

開発環境の場合、DB再作成で解決:
```bash
docker compose down -v
docker compose up -d
docker compose --profile flyway up flyway
docker compose --profile app up -d
```

## 開発者向け情報

<details>
<summary><strong>Makefileコマンド（開発効率化）</strong></summary>

Java開発環境がある場合、Makefileで効率化できます:

```bash
make help             # ヘルプ表示
make setup            # 初回セットアップ
make build            # 全Dockerイメージビルド（アプリ + Flyway）
make start-local      # ローカル開発起動（インフラコンテナ + アプリローカル実行）
make start-container  # 完全Docker環境起動（全コンテナ起動）
make stop             # 停止
make clean            # 環境完全リセット（ボリューム削除）
make test             # テスト実行
make migrate          # データベースマイグレーション実行
```

**起動パターン**:

**パターン1: ローカル開発（推奨）**
```bash
make setup           # 初回のみ
make start-local     # インフラ起動 + アプリローカル実行
# Ctrl+Cでアプリ停止、インフラは起動したまま
```

**パターン2: 完全Docker環境**
```bash
make setup           # 初回のみ
make build           # イメージビルド
make start-container # 全コンテナ起動
```

</details>

<details>
<summary><strong>テスト実行方法</strong></summary>

### 全テスト実行

```bash
# コンテナ内でテスト実行
docker compose run --rm app ./gradlew test

# または、Java環境がある場合
./gradlew test
```

### 特定のテストクラスを実行

```bash
./gradlew test --tests UserRepositoryTest
./gradlew test --tests S3ServiceTest
```

### テストカバレッジ

- **リポジトリ層**: UserRepository, ImageMemoRepository
- **サービス層**: UserService, S3Service, ImageMemoService
- **インフラ層**: データベース接続, S3接続

詳細は `CLAUDE.md` を参照してください。

</details>

<details>
<summary><strong>プロジェクト構成</strong></summary>

```
src/
├── main/
│   ├── java/
│   │   └── com/example/handson/
│   │       ├── config/           # 設定クラス
│   │       │   ├── SecurityConfig.java
│   │       │   └── S3Properties.java
│   │       ├── controller/       # コントローラー
│   │       │   ├── HomeController.java
│   │       │   ├── UserController.java
│   │       │   └── ImageMemoController.java
│   │       ├── domain/           # ドメインモデル
│   │       │   ├── user/
│   │       │   │   ├── User.java
│   │       │   │   └── UserRepository.java
│   │       │   └── imagememo/
│   │       │       ├── ImageMemo.java
│   │       │       └── ImageMemoRepository.java
│   │       ├── dto/              # データ転送オブジェクト（Java Record）
│   │       │   ├── UserRegistrationDto.java
│   │       │   └── ImageMemoCreateDto.java
│   │       ├── exception/        # 例外処理
│   │       │   ├── GlobalExceptionHandler.java
│   │       │   └── StorageException.java
│   │       ├── security/         # セキュリティ
│   │       │   └── CustomUserDetailsService.java
│   │       └── service/          # サービス層
│   │           ├── UserService.java
│   │           ├── S3Service.java
│   │           ├── ImageMemoService.java
│   │           └── ImageDownloadResult.java
│   └── resources/
│       ├── templates/            # Thymeleafテンプレート
│       │   ├── layout/
│       │   │   └── base.html
│       │   ├── fragments/
│       │   │   └── common.html
│       │   ├── index.html
│       │   ├── login.html
│       │   ├── register.html
│       │   ├── memos/
│       │   │   ├── list.html
│       │   │   ├── create.html
│       │   │   ├── detail.html
│       │   │   └── delete-confirm.html
│       │   └── error/
│       │       ├── 400.html
│       │       └── 500.html
│       ├── static/
│       │   └── css/
│       │       └── style.css     # 896行のカスタムCSS
│       ├── application.yml       # メイン設定（本番環境）
│       └── application-local.yml # ローカル環境設定
├── migration/
│   ├── Dockerfile                # Flyway用カスタムイメージ
│   └── sql/                      # Flywayマイグレーション
│       └── V1__create_initial_schema.sql
├── scripts/
│   ├── init-postgres.sh          # PostgreSQL初期化
│   ├── init-db-users.sql         # DBユーザー権限設定
│   └── init-localstack.sh        # LocalStack初期化
└── test/
    └── java/
        └── com/example/handson/
            ├── domain/
            │   ├── user/UserRepositoryTest.java
            │   └── imagememo/ImageMemoRepositoryTest.java
            ├── infrastructure/
            │   ├── DatabaseConnectionTest.java
            │   └── S3ConnectionTest.java
            ├── service/
            │   ├── UserServiceTest.java
            │   ├── ImageMemoServiceTest.java
            │   └── S3ServiceTest.java
            └── controller/
                └── ImageMemoControllerTest.java
```

詳細なアーキテクチャは `CLAUDE.md` を参照してください。

</details>

<details>
<summary><strong>設定情報</strong></summary>

### データベース設定

**本番環境** (`application.yml`):
- 環境変数で設定（デフォルト値なし）: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- DB権限分離: `handson_migration`（マイグレーション用）と `handson_app`（アプリ用）

**ローカル環境** (`application-local.yml`):
- ホスト: localhost:5432
- データベース: handson
- ユーザー: handson_app
- パスワード: app_password

### S3設定

**ローカル環境** (`application-local.yml`):
- LocalStackのS3エンドポイント: http://localhost:4566
- バケット名: handson-app-bucket
- path-style-access: 有効

### 許可される画像ファイル

- 拡張子: jpg, jpeg, png, gif
- 最大サイズ: 10MB（設定可能）
- S3パス形式: `uploads/{userId}/{UUID}.{拡張子}`
</details>