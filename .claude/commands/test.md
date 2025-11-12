# テストを実行

このコマンドは、JUnit 5とTestcontainersを使用してテストを実行します。

## 実行方法

### 全テスト実行

```bash
./gradlew test
```

または:

```bash
make test
```

### 特定のテストクラスを実行

```bash
./gradlew test --tests UserServiceTest
```

### 特定のテストメソッドを実行

```bash
./gradlew test --tests UserServiceTest.registerUser_shouldHashPassword
```

### カバレッジレポート生成

```bash
./gradlew test jacocoTestReport
```

レポート確認: `build/reports/jacoco/test/html/index.html`

## テストカテゴリ

### リポジトリ層テスト（Testcontainers使用）

**実データベース**を使用してテスト:

- `UserRepositoryTest`: ユーザー保存/検索/一意制約
- `ImageMemoRepositoryTest`: 画像メモCRUD/ユーザー関連検索/N+1クエリ対策

**特徴**:
- PostgreSQL 17コンテナを自動起動
- 実際のSQLが実行される
- トランザクションロールバックで各テスト後にクリーンアップ

### サービス層テスト（モック使用）

**ビジネスロジック**をモック・スタブで高速テスト:

- `UserServiceTest`: ユーザー登録/検索/パスワードハッシュ化/null安全性
- `ImageMemoServiceTest`: 画像メモ作成/削除/トランザクション境界
- `S3ServiceTest`: S3アップロード/削除/ストリーミングダウンロード/例外処理

**特徴**:
- Repositoryをモック化
- 高速実行
- ビジネスロジックのみに焦点

### コントローラー層テスト（MockMvc使用）

**HTTPリクエスト/レスポンス**をテスト:

- `ImageMemoControllerTest`: 画像配信エンドポイント/認証・認可/エラーハンドリング

**特徴**:
- Spring MVCのモックを使用
- HTTPステータスコードやヘッダーを検証

### インフラ層テスト

- `DatabaseConnectionTest`: PostgreSQL接続確認
- `S3ConnectionTest`: LocalStack S3接続確認

## トラブルシューティング

### Testcontainers起動失敗

**症状**: `Could not start container`

**対処法**:

1. Docker Desktopが起動しているか確認:
   ```bash
   docker --version
   docker ps
   ```

2. ディスク容量確認:
   ```bash
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

**原因**: Testcontainersがイメージをダウンロードしている

**対処法**:

1. イメージを事前にダウンロード:
   ```bash
   docker pull postgres:17
   docker pull localstack/localstack
   ```

2. 並列実行を有効化（`gradle.properties`）:
   ```properties
   org.gradle.parallel=true
   org.gradle.workers.max=4
   ```

### テストが失敗する（N+1クエリテスト）

**症状**: `Expected 1 query but got 11`

**対処法**:

1. リポジトリメソッドでJOIN FETCHを確認:
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

### ポート競合エラー

**症状**: Testcontainersがポートを取得できない

**対処法**:

```bash
# ローカルのPostgreSQLを停止
brew services stop postgresql

# または Docker Composeを停止
docker compose down
```

## TDDアプローチ

### Red → Green → Refactor

1. **Red**: テストを先に書いて失敗させる
2. **Green**: 最小限の実装でテストをパス
3. **Refactor**: コードを洗練させる

### 推奨順序

1. リポジトリテスト作成（Red）
2. リポジトリ実装（Green）
3. サービステスト作成（Red）
4. サービス実装（Green）
5. コントローラー実装
6. 手動テスト

## 重要な注意事項

- **Testcontainers使用時はDocker Desktopが必要**
- **初回実行時はイメージダウンロードで時間がかかる**
- **テスト実行前にローカルのPostgreSQLを停止推奨**（ポート競合回避）

## 詳細ドキュメント

- [docs/DEVELOPMENT.md - テスト戦略](../../docs/DEVELOPMENT.md#テスト戦略)
- [docs/TROUBLESHOOTING.md - テスト関連](../../docs/TROUBLESHOOTING.md#テスト関連)
