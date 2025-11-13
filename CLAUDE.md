# Handson Application - Claude Code クイックリファレンス

このドキュメントは、Claude Codeが効率的にコードベースを理解し、改修を支援するためのクイックリファレンスです。

> **詳細ドキュメント**: より詳しい情報は[docs/](./docs/)ディレクトリを参照してください

## 目次

- [プロジェクト概要](#プロジェクト概要)
- [クイックスタート](#クイックスタート)
- [アーキテクチャ概要](#アーキテクチャ概要)
- [よく使うコマンド](#よく使うコマンド)
- [よくある作業フロー](#よくある作業フロー)
- [トラブルシューティング](#トラブルシューティング)
- [作業ログ管理](#作業ログ管理)
- [詳細ドキュメント](#詳細ドキュメント)

## プロジェクト概要

画像とメモを組み合わせて保存・管理できるWebアプリケーション。TDD（Test-Driven Development）アプローチで開発されています。

### 技術スタック

- **言語**: Java 25
- **フレームワーク**: Spring Boot 3.5.7
- **データベース**: PostgreSQL 17（Flyway）
- **クラウド**: Spring Cloud AWS 3.4.0 (S3)
- **セキュリティ**: Spring Security（フォーム認証、BCrypt）
- **ビューテンプレート**: Thymeleaf + Material Icons
- **テスト**: JUnit 5, Testcontainers
- **コンテナ**: Docker（マルチステージビルド、レイヤードJAR）

### 主要機能

- ユーザー登録・ログイン（Spring Security + BCrypt）
- 画像メモのCRUD操作（S3 + PostgreSQL）
- 画像配信（アプリケーションProxy方式）
- ページング対応の一覧表示（N+1クエリ対策）
- ヘルスチェックエンドポイント（Spring Boot Actuator）

## クイックスタート

### 初回セットアップ（推奨）

```bash
make setup
```

このコマンド1つで以下がすべて自動実行されます:
- Docker環境起動（PostgreSQL + LocalStack）
- データベースマイグレーション実行
- LocalStackの初期化とS3バケット作成

### 起動パターン

**パターン1: ローカル開発（推奨）**

```bash
make start-local     # インフラ起動 + アプリローカル実行
# Ctrl+Cでアプリ停止、インフラは起動したまま
```

**パターン2: 完全Docker環境**

```bash
make build           # イメージビルド
make start-container # 全コンテナ起動
# http://localhost:8080 でアクセス可能
```

### テスト実行

```bash
./gradlew test                         # 全テスト実行
./gradlew test --tests UserServiceTest # 特定テスト実行
```

詳細: [docs/DEVELOPMENT.md](./docs/DEVELOPMENT.md)

## アーキテクチャ概要

### レイヤー構成

```
presentation (controller) → service → domain (repository/entity)
                                ↓
                          infrastructure (S3)
```

### コアドメインモデル

**パッケージ構成**: `com.example.handson`（全クラス）

**User**（ユーザー）:
- パス: `src/main/java/com/example/handson/domain/user/User.java`
- `id`, `username`（一意）, `password`（BCrypt）, `enabled`, `createdAt`, `updatedAt`
- `@CreationTimestamp`, `@UpdateTimestamp`で自動タイムスタンプ管理

**ImageMemo**（画像メモ）:
- パス: `src/main/java/com/example/handson/domain/imagememo/ImageMemo.java`
- `id`, `user`（ManyToOne, LAZY）, `title`, `description`, `s3Key`, `createdAt`, `updatedAt`
- 画像URLは動的生成（`/memos/{id}/image`）のためDBには保存しない
- LombokアノテーションでBuilderパターン採用

### データベース設計

**ユーザー権限分離**（最小権限の原則）:
- `handson_migration`: Flywayマイグレーション実行（DDL権限）
- `handson_app`: Spring Bootアプリケーション実行（DML権限のみ）

**メリット**: SQLインジェクション対策、事故防止、監査対応

### S3パス設計

**形式**: `uploads/{userId}/{UUID}.{拡張子}`

**理由**:
- ユーザーIDでパス分離（運用性・管理性向上）
- UUID併用で推測攻撃を防止

### 画像配信方式

**アプリケーションProxy方式**を採用:
- S3バケット名を外部に露出しない
- 認証・認可をアプリケーション層で制御
- S3はブロックパブリックアクセスを維持可能

詳細: [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md)

## よく使うコマンド

### Makefileコマンド

```bash
# 基本操作
make help             # ヘルプ表示
make setup            # 初回セットアップ
make build            # 全Dockerイメージビルド（アプリ + Flyway）

# 起動・停止
make start-local      # ローカル開発起動（推奨）
make start-container  # 完全Docker環境起動
make stop             # 停止
make clean            # 環境完全リセット（ボリューム削除）

# 開発支援
make test             # テスト実行
make migrate          # マイグレーション実行
```

### カスタムコマンド

```bash
/migrate              # DBマイグレーション実行
/test                 # テスト実行とエラー分析
/review               # コードレビュー実施
```

### Gradleコマンド

```bash
# アプリケーション起動
./gradlew bootRun --args='--spring.profiles.active=local'

# テスト
./gradlew test
./gradlew test --tests ClassName

# ビルド
./gradlew build -x test
```

詳細: [docs/DEVELOPMENT.md](./docs/DEVELOPMENT.md#makefileコマンド一覧)

## よくある作業フロー

### 新機能追加（TDDアプローチ）

1. **マイグレーションファイル作成**: `migration/sql/V{version}__{description}.sql`
2. **エンティティ作成**: `src/main/java/com/example/imagememo/domain/{entity}/`
3. **リポジトリテスト作成**（TDD Red）
4. **リポジトリ実装**（TDD Green）
5. **サービステスト作成**（TDD Red）
6. **サービス実装**（TDD Green）
7. **コントローラー実装**
8. **ビュー作成**: `src/main/resources/templates/`
9. **手動テスト**

### コードレビュー

```bash
/review  # カスタムコマンド実行
```

チェック項目:
- アーキテクチャ準拠性（レイヤー構成）
- セキュリティ（認証・認可、SQLインジェクション対策）
- パフォーマンス（N+1クエリ、トランザクション境界）
- テストカバレッジ
- コーディング規約

### DBマイグレーション

```bash
/migrate  # カスタムコマンド実行

# または
make migrate
```

**注意**: マイグレーションは`handson_migration`ユーザーで実行されます。


## トラブルシューティング

### ポート競合エラー

```bash
# ポート確認
lsof -ti:8080

# プロセス終了
lsof -ti:8080 | xargs kill -9

# または全コンテナ停止
docker compose down
```

### Docker環境リセット

```bash
make clean
make setup
```

### Flywayマイグレーションエラー

**開発環境**:
```bash
# マイグレーション履歴確認
docker exec -it handson-db psql -U handson_migration -d handson -c \
  "SELECT version, description, success FROM flyway_schema_history;"

# DB完全リセット（データ削除注意）
docker compose down -v
make setup
```

**本番環境**: 絶対にデータを削除しないこと。エラー内容を確認し、必要に応じて`flyway repair`。

### S3アップロードエラー（LocalStack）

```bash
# バケット作成
docker exec localstack awslocal s3 mb s3://image-memo-bucket

# バケット確認
docker exec localstack awslocal s3 ls
```

### Testcontainers起動失敗

- Docker Desktopが起動しているか確認
- ディスク容量確認: `docker system df`
- 不要なイメージ削除: `docker system prune -a`

詳細: [docs/TROUBLESHOOTING.md](./docs/TROUBLESHOOTING.md)

## 作業ログ管理

### worklogディレクトリへの記録

すべての重要な変更・改善作業は`worklog/`ディレクトリにマークダウンファイルとして記録してください。

**重要な原則**:
- **作業のかたまりごとに新しいファイルを作成**してください
- 1つのファイルには、関連性のある1つの作業内容のみを記録します
- 複数の独立した作業を行う場合は、それぞれ別ファイルで記録します

**ファイル命名規則**: `YYYYMMDD_NNNN_description.md`

例: `20251019_0001_transaction_boundary_improvement.md`

**テンプレート**: `worklog/TEMPLATE.md`を使用してください

**記録すべき内容**:
- 背景・目的
- 実施内容（Before/After形式で具体的に）
- 変更の影響範囲
- 確認事項
- 期待される効果

**記録すべき作業の例**:
- 新機能の追加
- リファクタリング
- バグ修正
- パフォーマンス改善
- セキュリティ対応
- ドキュメント更新（CLAUDE.md等の重要ドキュメント）

**記録不要な作業の例**:
- 軽微なタイポ修正
- コメントの追加のみ
- コードフォーマットのみの変更

### CLAUDE.mdの更新

重要な機能追加・アーキテクチャ変更を行った場合は、このドキュメント（CLAUDE.md）も併せて更新してください。

## 詳細ドキュメント

### 技術ドキュメント

| ドキュメント | 内容 | 対象読者 |
|------------|------|---------|
| [ARCHITECTURE.md](./docs/ARCHITECTURE.md) | アーキテクチャ詳細、ドメインモデル、機能フロー、セキュリティ設計 | 開発者 |
| [DEVELOPMENT.md](./docs/DEVELOPMENT.md) | 開発環境セットアップ、テスト戦略、コーディング規約、改修パターン | 開発者 |
| [TROUBLESHOOTING.md](./docs/TROUBLESHOOTING.md) | トラブルシューティング完全版、エラー診断 | 全員 |
| [HISTORY.md](./docs/HISTORY.md) | コード品質改善履歴、設計判断の記録 | 全員 |
| [UI_GUIDE.md](./docs/UI_GUIDE.md) | Thymeleaf、UIフラグメント、Material Icons | フロントエンド |

### その他のリソース

- **worklog/**: 作業ログの詳細記録
- **ecs/README.md**: ECSデプロイ詳細手順
- **README.md**: プロジェクト紹介（ユーザー向け）

## 重要なファイルパス

### 設定ファイル

- `build.gradle.kts`: 依存関係とビルド設定
- `src/main/resources/application.yml`: メイン設定（本番環境想定）
- `src/main/resources/application-local.yml`: ローカル環境設定
- `compose.yml`: Docker Compose設定
- `Dockerfile`: コンテナイメージビルド設定
- `migration/Dockerfile`: Flyway用カスタムイメージ
- `Makefile`: 開発用コマンド定義

### コアビジネスロジック

- `src/main/java/com/example/imagememo/service/ImageMemoService.java`: 画像メモビジネスロジック
- `src/main/java/com/example/imagememo/service/S3Service.java`: S3連携
- `src/main/java/com/example/imagememo/service/UserService.java`: ユーザー管理

### セキュリティ

- `src/main/java/com/example/imagememo/config/SecurityConfig.java`: Spring Security設定
- `src/main/java/com/example/imagememo/security/CustomUserDetailsService.java`: 認証処理

### データベース

- `migration/sql/`: Flywayマイグレーション
- `scripts/init-postgres.sh`: PostgreSQL初期化スクリプト（ユーザー権限分離）

### テンプレート

- `src/main/resources/templates/layout/base.html`: 共通レイアウトベース
- `src/main/resources/templates/fragments/common.html`: 再利用可能なUIフラグメント
- `src/main/resources/templates/memos/`: 画像メモ関連ビュー

## 環境変数設定

このアプリケーションは、業界標準の**完全URL方式**で環境変数を設定します。この方式はHeroku、Cloud Run等の主要PaaSプラットフォームで採用されており、12-Factor Appの原則に準拠しています。

### アプリケーションコンテナの環境変数

**必須環境変数**:
```bash
JDBC_DATABASE_URL=jdbc:postgresql://host:5432/handson  # データベース接続URL
JDBC_DATABASE_USERNAME=handson_app                      # アプリケーション用ユーザー
JDBC_DATABASE_PASSWORD=secret                           # パスワード
AWS_S3_BUCKET_NAME=my-bucket                            # S3バケット名
```

**オプション環境変数**:
```bash
AWS_REGION=ap-northeast-1      # AWSリージョン（デフォルト: ap-northeast-1）
SPRING_PROFILES_ACTIVE=production  # Spring Bootプロファイル
```

### マイグレーションコンテナの環境変数

**必須環境変数**:
```bash
JDBC_DATABASE_URL=jdbc:postgresql://host:5432/handson   # データベース接続URL
JDBC_DATABASE_USERNAME=handson_migration                # マイグレーション用ユーザー
JDBC_DATABASE_PASSWORD=secret                           # パスワード
```

### データベースユーザー権限分離

セキュリティベストプラクティスとして、2つのデータベースユーザーを使い分けています：

| ユーザー名 | 用途 | 権限 | 使用箇所 |
|-----------|------|------|---------|
| `handson_migration` | Flywayマイグレーション | DDL権限（CREATE, DROP, ALTER） | マイグレーションコンテナ |
| `handson_app` | アプリケーション実行 | DML権限のみ（SELECT, INSERT, UPDATE, DELETE） | アプリケーションコンテナ |

### セキュリティ推奨事項

- **機密情報の管理**: パスワードは環境変数またはシークレット管理サービスで管理
- **IAMロール活用**: クラウド環境ではIAMロールを使用してS3アクセス権限を付与（アクセスキー不要）
- **最小権限の原則**: データベースユーザーは必要最小限の権限のみ付与

詳細: [docs/DEVELOPMENT.md](./docs/DEVELOPMENT.md)

## コーディング規約（簡易版）

### Lombokアノテーション

- **Entity**: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor`, `@Builder`
- **Service/Controller**: `@RequiredArgsConstructor`, `@Slf4j`
- **DTO**: Java Record形式（immutable、Lombok不要）

### トランザクション

- **更新系**: `@Transactional`（メソッドレベル）
- **参照系**: `@Transactional(readOnly = true)`（メソッドレベル）

### バリデーション

- **DTOレベル**: Bean Validation（`@NotBlank`, `@Size`, `@NotNull`）をJava Recordの引数に付与
- **サービスレベル**: ビジネスルール検証、`IllegalArgumentException`スロー
- **例外ハンドリング**: `GlobalExceptionHandler`で集約的に処理

### null安全性

- **原則**: 戻り値でnullを返さない、見つからない場合は例外をスロー

```java
return userRepository.findByUsername(username)
    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
```

詳細: [docs/DEVELOPMENT.md#コーディング規約](./docs/DEVELOPMENT.md#コーディング規約)

## 参考リンク

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [Spring Cloud AWS](https://docs.awspring.io/spring-cloud-aws/docs/3.4.0/reference/html/index.html)
- [Testcontainers](https://testcontainers.com/)
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

**最終更新**: 2025-11-09（CLAUDE.mdリファクタリング完了）
