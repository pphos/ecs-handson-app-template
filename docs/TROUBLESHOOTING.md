# トラブルシューティング

このドキュメントは、Handson Applicationの開発・運用で発生する一般的な問題と解決策を説明します。

## 目次

- [ローカル開発環境](#ローカル開発環境)
- [Docker関連](#docker関連)
- [データベース関連](#データベース関連)
- [S3/LocalStack関連](#s3localstack関連)
- [テスト関連](#テスト関連)

## ローカル開発環境

### ポート競合エラー

**症状**:
```
Port 8080 is already in use
```

**原因**: ポート8080, 5432, 4566が既に使用されている

**対処法**:

```bash
# ポート使用状況確認
lsof -ti:8080
lsof -ti:5432
lsof -ti:4566

# プロセス終了（8080の例）
lsof -ti:8080 | xargs kill -9

# または、すべてのDockerコンテナ停止
docker compose down
```

### アプリケーション起動失敗

**症状**:
```
Failed to configure a DataSource: 'url' attribute is not specified
```

**原因**: データベース接続設定が不足

**対処法**:

1. プロファイル確認:
   ```bash
   # localプロファイルで起動しているか確認
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

2. Docker起動確認:
   ```bash
   docker compose ps
   # db, localstackが起動していることを確認
   ```

3. 環境リセット:
   ```bash
   make clean
   make setup
   ```

### ホットリロードが効かない

**症状**: コード変更が反映されない

**原因**: Spring Boot DevToolsの設定問題

**対処法**:

1. `build.gradle.kts`にDevTools追加:
   ```kotlin
   developmentOnly("org.springframework.boot:spring-boot-devtools")
   ```

2. IDE設定確認（IntelliJ IDEA）:
   - `Settings` → `Build, Execution, Deployment` → `Compiler`
   - `Build project automatically`にチェック

3. アプリケーション再起動:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

## Docker関連

### Docker環境の不整合

**症状**: コンテナが正常に動作しない、データが消えている

**原因**: ボリュームやイメージの不整合

**対処法**:

```bash
# 完全リセット（データ削除注意）
make clean

# 再セットアップ
make setup
```

### Dockerビルドが遅い

**症状**: `docker build`に時間がかかる

**原因**: Dockerレイヤーキャッシュが効いていない

**対処法**:

1. `.dockerignore`確認:
   ```
   # 不要なファイルを除外
   .git/
   .gradle/
   build/
   *.md
   ```

2. ビルドコンテキスト削減:
   ```bash
   # 不要なファイルを削除
   ./gradlew clean
   ```

3. BuildKitを有効化:
   ```bash
   export DOCKER_BUILDKIT=1
   docker build .
   ```

### コンテナログが見れない

**症状**: `docker logs`でログが表示されない

**対処法**:

```bash
# コンテナ名確認
docker compose ps

# ログ確認（フォロー）
docker compose logs -f app

# 特定のコンテナのみ
docker logs -f handson-app
```

## データベース関連

### Flywayマイグレーションエラー

**症状**:
```
Validate failed: Detected applied migration not resolved locally
```

**原因**: マイグレーションファイルが変更または削除された

**対処法**:

#### 開発環境の場合

```bash
# マイグレーション履歴確認
docker exec -it handson-db psql -U handson_migration -d handson -c \
  "SELECT version, description, success FROM flyway_schema_history;"

# DB完全リセット（データ削除注意）
docker compose down -v
make setup
```

#### 本番環境の場合

**絶対にデータを削除しないこと**

1. エラー内容を詳細に確認
2. マイグレーションファイルの整合性確認
3. 必要に応じてFlywayの`repair`コマンド実行:
   ```bash
   flyway repair
   ```

### DB接続エラー（本番環境）

**症状**:
```
java.net.UnknownHostException: ${DB_HOST}
```

**原因**: 環境変数が設定されていない

**対処法**:

1. 環境変数確認:
   ```bash
   # ECSタスク定義で確認
   aws ecs describe-task-definition --task-definition handson-app
   ```

2. 必須環境変数が設定されているか確認:
   - `DB_HOST`
   - `DB_NAME`
   - `DB_USER`
   - `DB_PASSWORD`（Secrets Manager）

3. RDS接続確認:
   ```bash
   # セキュリティグループでECSからのアクセスが許可されているか確認
   aws ec2 describe-security-groups --group-ids sg-xxx
   ```

### DB権限エラー

**症状**:
```
permission denied for table users
```

**原因**: アプリケーションユーザー（`handson_app`）に権限がない

**対処法**:

```sql
-- マイグレーションユーザーで接続
psql -U handson_migration -d handson

-- 権限確認
\dp users

-- 権限付与（必要に応じて）
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO handson_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO handson_app;
```

**予防策**: `scripts/init-postgres.sh`でデフォルト権限を設定済み

## S3/LocalStack関連

### S3アップロードエラー（LocalStack）

**症状**:
```
The specified bucket does not exist: handson-app-bucket
```

**原因**: LocalStackのS3バケットが作成されていない

**対処法**:

```bash
# バケット作成
docker exec localstack awslocal s3 mb s3://handson-app-bucket

# バケット確認
docker exec localstack awslocal s3 ls

# または環境リセット
make clean
make setup
```

### S3接続エラー（本番環境）

**症状**:
```
Unable to execute HTTP request: handson-app-bucket.s3.ap-northeast-1.amazonaws.com
```

**原因**: S3バケット名またはリージョンが正しくない

**対処法**:

1. 環境変数確認:
   ```yaml
   AWS_S3_BUCKET_NAME: <バケット名>
   AWS_REGION: ap-northeast-1
   ```

2. IAMロール確認:
   ```json
   {
     "Effect": "Allow",
     "Action": [
       "s3:GetObject",
       "s3:PutObject",
       "s3:DeleteObject"
     ],
     "Resource": "arn:aws:s3:::handson-app-bucket/*"
   }
   ```

3. S3バケット存在確認:
   ```bash
   aws s3 ls s3://handson-app-bucket/
   ```

### 画像が表示されない

**症状**: 画像メモ一覧で画像が表示されない（404エラー）

**原因**: S3キーが正しくない、または認可エラー

**対処法**:

1. ブラウザの開発者ツールでネットワークエラー確認
2. ログ確認:
   ```bash
   # ローカル環境
   ./gradlew bootRun
   # エラーログに "StorageException" がないか確認

   # 本番環境
   aws logs tail /ecs/handson-app --follow
   ```

3. S3キー確認:
   ```bash
   # ローカル（LocalStack）
   docker exec localstack awslocal s3 ls s3://handson-app-bucket/uploads/

   # 本番
   aws s3 ls s3://handson-app-bucket/uploads/
   ```

## テスト関連

### Testcontainers起動失敗

**症状**:
```
Could not start container
```

**原因**: Dockerが起動していない、またはディスク容量不足

**対処法**:

1. Docker Desktop起動確認:
   ```bash
   docker --version
   docker ps
   ```

2. ディスク容量確認:
   ```bash
   df -h
   docker system df
   ```

3. 不要なイメージ・コンテナ削除:
   ```bash
   docker system prune -a
   ```

4. Testcontainersキャッシュ削除:
   ```bash
   rm -rf ~/.testcontainers
   ```

### テスト実行が遅い

**症状**: `./gradlew test`に時間がかかる

**原因**: Testcontainersがイメージをダウンロードしている

**対処法**:

1. イメージを事前にダウンロード:
   ```bash
   docker pull postgres:17
   docker pull localstack/localstack
   ```

2. 特定のテストのみ実行:
   ```bash
   # 特定のテストクラス
   ./gradlew test --tests UserServiceTest

   # 特定のテストメソッド
   ./gradlew test --tests UserServiceTest.registerUser_shouldHashPassword
   ```

3. 並列実行を有効化（`gradle.properties`）:
   ```properties
   org.gradle.parallel=true
   org.gradle.workers.max=4
   ```

### テストが失敗する（N+1クエリテスト）

**症状**:
```
Expected 1 query but got 11
```

**原因**: JOIN FETCHが正しく動作していない

**対処法**:

1. リポジトリメソッド確認:
   ```java
   @Query("SELECT m FROM ImageMemo m JOIN FETCH m.user WHERE m.user.id = :userId")
   Page<ImageMemo> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
   ```

2. エンティティのfetchタイプ確認:
   ```java
   @ManyToOne(fetch = FetchType.LAZY, optional = false)
   private User user;
   ```

3. テストログでクエリ確認:
   ```yaml
   # application-test.yml
   spring:
     jpa:
       show-sql: true
       properties:
         hibernate:
           format_sql: true
   ```

## 診断コマンド一覧

### ローカル環境

```bash
# 環境状態確認
docker compose ps
docker compose logs

# ポート使用状況
lsof -ti:8080
lsof -ti:5432
lsof -ti:4566

# DB接続確認
docker exec -it handson-db psql -U handson_app -d handson -c "SELECT 1;"

# S3接続確認（LocalStack）
docker exec localstack awslocal s3 ls s3://handson-app-bucket/
```

---

## 参考資料

- [DEVELOPMENT.md](./DEVELOPMENT.md) - 開発環境セットアップ
- [ARCHITECTURE.md](./ARCHITECTURE.md) - アーキテクチャ詳細
