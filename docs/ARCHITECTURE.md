# アーキテクチャドキュメント

このドキュメントは、Handson Applicationのアーキテクチャ設計の詳細を説明します。

## 目次

- [アーキテクチャ概要](#アーキテクチャ概要)
- [レイヤー構成](#レイヤー構成)
- [コアドメインモデル](#コアドメインモデル)
- [主要な機能フロー](#主要な機能フロー)
- [セキュリティ設計](#セキュリティ設計)
- [データベース設計](#データベース設計)
- [AWS S3連携](#aws-s3連携)

## アーキテクチャ概要

Handson Applicationは、レイヤードアーキテクチャを採用したSpring Bootアプリケーションです。

### 設計原則

- **関心の分離**: プレゼンテーション層、ビジネスロジック層、データアクセス層を明確に分離
- **単一責任の原則**: 各クラスは1つの責務のみを持つ
- **依存関係の逆転**: ビジネスロジックがインフラストラクチャに依存しない
- **テスタビリティ**: TDDアプローチで開発、高いテストカバレッジを維持

### アーキテクチャ図

```
┌─────────────────────────────────────────┐
│  Presentation Layer (Controller)       │
│  - HomeController                       │
│  - UserController                       │
│  - ImageMemoController                  │
└────────────┬────────────────────────────┘
             │
             ↓
┌─────────────────────────────────────────┐
│  Service Layer (Business Logic)        │
│  - UserService                          │
│  - ImageMemoService                     │
│  - S3Service                            │
└────────────┬────────────────────────────┘
             │
             ↓
┌─────────────────────────────────────────┐
│  Domain Layer (Repository/Entity)      │
│  - User (Entity)                        │
│  - ImageMemo (Entity)                   │
│  - UserRepository                       │
│  - ImageMemoRepository                  │
└─────────────────────────────────────────┘

         Infrastructure
┌─────────────────────────────────────────┐
│  - AWS S3 (S3Service)                   │
│  - PostgreSQL (JPA/Hibernate)           │
│  - Spring Security                      │
└─────────────────────────────────────────┘
```

## レイヤー構成

### Presentation Layer (Controller)

**責務**: HTTPリクエストの受け付けとレスポンスの返却、ビューへのデータ渡し

#### HomeController
- **パス**: `src/main/java/com/example/handson/controller/HomeController.java`
- **役割**: ホームページとログインページの表示
- **エンドポイント**:
  - `GET /`: ホームページ表示
  - `GET /login`: ログインページ表示

#### UserController
- **パス**: `src/main/java/com/example/handson/controller/UserController.java`
- **役割**: ユーザー登録処理
- **エンドポイント**:
  - `GET /register`: 登録フォーム表示
  - `POST /register`: ユーザー登録処理

#### ImageMemoController
- **パス**: `src/main/java/com/example/handson/controller/ImageMemoController.java`
- **役割**: 画像メモのCRUD操作
- **エンドポイント**:
  - `GET /memos`: 画像メモ一覧表示（ページング対応）
  - `GET /memos/new`: 作成フォーム表示
  - `POST /memos`: 画像メモ作成
  - `GET /memos/{id}`: 詳細表示
  - `GET /memos/{id}/delete-confirm`: 削除確認画面表示
  - `POST /memos/{id}/delete`: 削除
  - `GET /memos/{id}/image`: 画像配信（アプリケーションProxy方式）

### Service Layer (Business Logic)

**責務**: ビジネスロジックの実装、トランザクション管理

#### UserService
- **パス**: `src/main/java/com/example/handson/service/UserService.java`
- **役割**: ユーザー管理のビジネスロジック
- **主要メソッド**:
  - `registerUser(UserRegistrationDto dto)`: ユーザー登録（パスワードBCryptハッシュ化、パスワード確認検証）
  - `findByUsername(username)`: ユーザー名での検索（null安全）

#### ImageMemoService
- **パス**: `src/main/java/com/example/handson/service/ImageMemoService.java`
- **役割**: 画像メモのビジネスロジック
- **主要メソッド**:
  - `createImageMemo(ImageMemoCreateDto dto, User user)`: 画像メモ作成（S3アップロード + DB保存）
  - `findByUserId(Long userId, Pageable pageable)`: ユーザーの画像メモ一覧取得（ページング）
  - `findByIdAndUserId(Long id, Long userId)`: 画像メモ取得（所有者チェック）
  - `deleteImageMemo(Long id, Long userId)`: 画像メモ削除（S3削除 + DB削除）

#### S3Service
- **パス**: `src/main/java/com/example/handson/service/S3Service.java`
- **役割**: AWS S3との連携
- **主要メソッド**:
  - `uploadFile(String bucketName, MultipartFile file, String userId)`: S3への画像アップロード
  - `downloadImageStream(String bucketName, String s3Key)`: S3からの画像ダウンロード（ストリーミング）
  - `deleteFile(String bucketName, String s3Key)`: S3からの画像削除

### Domain Layer (Repository/Entity)

**責務**: ドメインモデルの表現とデータアクセス

#### Entity

**User** (`src/main/java/com/example/handson/domain/user/User.java`):
- ユーザー情報を表現
- フィールド: id, username, password, enabled, createdAt, updatedAt
- Lombokアノテーション: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor`, `@Builder`
- タイムスタンプ: `@CreationTimestamp`, `@UpdateTimestamp`による自動設定

**ImageMemo** (`src/main/java/com/example/handson/domain/imagememo/ImageMemo.java`):
- 画像メモ情報を表現
- フィールド: id, user, title, description, s3Key, createdAt, updatedAt
- 関連: User（ManyToOne, LAZY）
- Lombokアノテーション: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor`, `@Builder`

#### Repository

**UserRepository** (`src/main/java/com/example/handson/domain/user/UserRepository.java`):
- `findByUsername(String username)`: ユーザー名での検索
- `existsByUsername(String username)`: ユーザー名の存在チェック

**ImageMemoRepository** (`src/main/java/com/example/handson/domain/imagememo/ImageMemoRepository.java`):
- `findByUserIdOrderByCreatedAtDesc(Long userId)`: ユーザーの画像メモ一覧（非ページング）
- `findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable)`: ユーザーの画像メモ一覧（N+1対策でJOIN FETCH）
- `findByIdAndUserId(Long id, Long userId)`: 画像メモ取得（所有者チェック）

### Exception Layer

**StorageException** (`src/main/java/com/example/handson/exception/StorageException.java`):
- S3操作のエラーをラップするカスタム例外
- チェック例外（IOException等）を適切に処理

**GlobalExceptionHandler** (`src/main/java/com/example/handson/exception/GlobalExceptionHandler.java`):
- `@ControllerAdvice`による集約的な例外処理
- IllegalArgumentException → 400エラー
- StorageException → 500エラー
- カスタムエラーページへの誘導

### DTO Layer

**UserRegistrationDto** (`src/main/java/com/example/handson/dto/UserRegistrationDto.java`):
- Java Record形式（immutable）
- フィールド: username, password, confirmPassword
- Bean Validationアノテーション付き

**ImageMemoCreateDto** (`src/main/java/com/example/handson/dto/ImageMemoCreateDto.java`):
- Java Record形式（immutable）
- フィールド: title, description, image
- Bean Validationアノテーション付き

### Configuration

**SecurityConfig** (`src/main/java/com/example/handson/config/SecurityConfig.java`):
- Spring Securityの設定
- アクセス制御、認証方式、CSRF保護

**S3Properties** (`src/main/java/com/example/handson/config/S3Properties.java`):
- S3関連の設定をバインド
- `@ConfigurationProperties(prefix = "app.s3")`
- バケット名、最大ファイルサイズ、許可拡張子、キャッシュ期間

**CustomUserDetailsService** (`src/main/java/com/example/handson/security/CustomUserDetailsService.java`):
- Spring SecurityのUserDetailsService実装
- ユーザー認証時の情報取得

## コアドメインモデル

### User

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // 主キー

    @Column(nullable = false, unique = true, length = 50)
    private String username;            // ユーザー名（一意、最大50文字）

    @Column(nullable = false)
    private String password;            // BCryptハッシュ化パスワード

    @Column(nullable = false)
    private boolean enabled;            // アカウント有効フラグ

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;    // 作成日時（自動設定）

    @Column(nullable = false)
    private LocalDateTime updatedAt;    // 更新日時（自動更新）
}
```

**ビジネスルール**:
- ユーザー名は一意（データベース制約 + アプリケーション層でチェック）
- パスワードは必ずBCryptでハッシュ化して保存
- 新規登録時は`enabled = true`で作成
- 作成日時・更新日時は`@PrePersist`/`@PreUpdate`で自動設定

### ImageMemo

```java
@Entity
@Table(name = "image_memos")
public class ImageMemo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // 主キー

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;                  // 所有者（ManyToOne, LAZY）

    @Column(nullable = false)
    private String title;               // タイトル（最大255文字）

    @Column(columnDefinition = "TEXT")
    private String description;         // 説明（TEXT型）

    @Column(nullable = false)
    private String s3Key;               // S3オブジェクトキー

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;    // 作成日時

    @Column(nullable = false)
    private LocalDateTime updatedAt;    // 更新日時
}
```

**ビジネスルール**:
- 画像メモは必ずユーザーに紐づく
- s3Keyは必須（画像ファイルがS3に保存されている）
- 画像URLは動的生成（`/memos/{id}/image`）のためDBには保存しない
- タイトルは必須、説明は任意
- Userとの関連はLAZY（N+1クエリ防止のためリポジトリでJOIN FETCH）

### ER図

```
┌─────────────────┐
│     users       │
├─────────────────┤
│ id (PK)         │
│ username (UQ)   │
│ password        │
│ enabled         │
│ created_at      │
│ updated_at      │
└────────┬────────┘
         │
         │ 1
         │
         │ N
┌────────┴────────┐
│  image_memos    │
├─────────────────┤
│ id (PK)         │
│ user_id (FK)    │
│ title           │
│ description     │
│ s3_key          │
│ created_at      │
│ updated_at      │
└─────────────────┘
```

## 主要な機能フロー

### 1. ユーザー登録フロー

**エンドポイント**: `POST /register`

```
[ユーザー] → [UserController.register()]
               ↓
               フォームバリデーション（Bean Validation）
               ↓
             [UserService.registerUser()]
               ↓
               ユーザー名の重複チェック
               ↓
               パスワードのBCryptハッシュ化
               ↓
             [UserRepository.save()]
               ↓
               自動ログイン処理（SecurityContextHolder設定）
               ↓
               /memosへリダイレクト
```

**トランザクション境界**: `UserService.registerUser()`

**エラーハンドリング**:
- ユーザー名重複 → バリデーションエラー
- DB保存失敗 → トランザクションロールバック

### 2. 画像メモ作成フロー

**エンドポイント**: `POST /memos`

```
[ユーザー] → [ImageMemoController.createMemo()]
               ↓
               フォームバリデーション（画像ファイル必須）
               ↓
             [ImageMemoService.createImageMemo()]
               ↓
               画像ファイルの検証
               ↓
             [S3Service.uploadFile()]
               ↓
               S3にアップロード → s3Key取得
               ↓
             [ImageMemoRepository.save()]
               ↓
               s3Keyを含めてDB保存
               ↓
               /memosへリダイレクト
```

**トランザクション境界**: `ImageMemoService.createImageMemo()`

**S3キー形式**: `uploads/{userId}/{UUID}.{拡張子}`
- ユーザーIDでパス分離（運用性・管理性向上）
- UUID併用で推測攻撃を防止

**エラーハンドリング**:
- S3アップロード失敗 → `StorageException`スロー → トランザクションロールバック
- DB保存失敗 → トランザクションロールバック（S3画像は残る可能性あり）

### 3. 画像メモ一覧表示フロー

**エンドポイント**: `GET /memos`

```
[ユーザー] → [ImageMemoController.listMemos()]
               ↓
               ページング設定（デフォルト10件/ページ）
               ↓
             [ImageMemoService.findByUserId()]
               ↓
             [ImageMemoRepository.findByUserIdOrderByCreatedAtDesc()]
               ↓
               JOIN FETCHでUser情報を一緒に取得（N+1対策）
               ↓
               作成日時降順でソート
               ↓
               Thymeleafビュー "memos/list" で表示
               画像URLは /memos/{id}/image として動的生成
```

**パフォーマンス最適化**:
- JOIN FETCHでN+1クエリを防止
- ページングでメモリ使用量を制御
- 画像はLazy配信（アプリケーションProxy方式）

### 4. 画像配信フロー（アプリケーションProxy方式）

**エンドポイント**: `GET /memos/{id}/image`

```
[ブラウザ] → [ImageMemoController.getImage(id)]
               ↓
               認証チェック（Spring Security自動実行）
               ↓
             [ImageMemoService.findByIdAndUserId(id, userId)]
               ↓
               所有者チェック（他ユーザーの画像は404）
               ↓
             [S3Service.downloadImageStream(bucket, s3Key)]
               ↓
               S3から画像をストリーミングダウンロード
               ↓
               InputStreamResourceで画像を配信
               Content-Type、Content-Length、Cache-Controlヘッダー設定
```

**セキュリティ**:
- 認証必須（未認証は401）
- 所有者チェック（他ユーザーは404）
- S3バケット名を外部に露出しない

**パフォーマンス**:
- ストリーミング配信（メモリ効率的）
- Cache-Controlヘッダー（max-age=3600, private）でブラウザキャッシュ有効化

### 5. 画像メモ削除フロー

**エンドポイント**: `POST /memos/{id}/delete`

```
[ユーザー] → [ImageMemoController.deleteMemo(id)]
               ↓
             [ImageMemoService.deleteImageMemo(id, userId)]
               ↓
               権限チェック（ユーザーIDとメモの所有者が一致）
               ↓
             [S3Service.deleteFile(bucket, s3Key)]
               ↓
               S3から画像削除
               ↓
             [ImageMemoRepository.delete()]
               ↓
               DBから削除
               ↓
               /memosへリダイレクト
```

**トランザクション境界**: `ImageMemoService.deleteImageMemo()`

**エラーハンドリング**:
- S3削除失敗 → `StorageException`スロー → DB削除もロールバック
- 所有者不一致 → `IllegalArgumentException`スロー

## セキュリティ設計

### 認証・認可

**認証方式**: Spring Security フォームベース認証

**アクセス制御** (`SecurityConfig.java`):

| パス | アクセス権 | 理由 |
|------|----------|------|
| `/`, `/register` | 全員 | 公開ページ |
| `/css/**`, `/js/**`, `/images/**` | 全員 | 静的リソース |
| `/actuator/health` | 全員 | ヘルスチェック（認証不要） |
| その他すべて | 認証必須 | プライベートコンテンツ |

### パスワード管理

- **ハッシュ化**: BCryptPasswordEncoder（強度10）
- **保存形式**: `$2a$10$...`（BCrypt形式）
- **検証**: Spring Securityが自動で比較

### CSRF保護

- **設定**: デフォルトで有効
- **Thymeleaf**: フォームに自動でCSRFトークン挿入
- **API**: 将来的にREST API化する場合は無効化検討

### セッション管理

- **ログイン成功時**: `/memos`へリダイレクト
- **ログアウト成功時**: `/`へリダイレクト
- **セッションタイムアウト**: デフォルト（30分）

### 画像アクセス制御

**アプリケーションProxy方式**を採用:

**メリット**:
- S3バケット名・インフラ情報を完全に隠蔽
- 認証・認可をアプリケーション層で制御
- S3はブロックパブリックアクセスを維持可能
- きめ細かいアクセス制御（所有者チェック）

**実装**:
1. S3バケットはプライベート（ブロックパブリックアクセス有効）
2. アプリケーションがIAMロール経由でS3にアクセス
3. 画像配信時に認証・所有者チェック実施
4. ストリーミング配信で効率的

## データベース設計

### ユーザー権限分離（セキュリティベストプラクティス）

**最小権限の原則**に基づき、マイグレーション用とアプリケーション用でDBユーザーを分離。

#### DBユーザー構成

| ユーザー名 | 用途 | 権限 |
|-----------|------|------|
| `handson_migration` | Flywayマイグレーション実行 | DDL権限（CREATE, DROP, ALTER）、スキーマオーナー |
| `handson_app` | Spring Bootアプリケーション実行 | DML権限のみ（SELECT, INSERT, UPDATE, DELETE） |

#### セキュリティメリット

- **事故防止**: アプリケーションから誤ってテーブルを削除・変更できない
- **SQLインジェクション対策**: DROP TABLEなどのDDL攻撃を防止
- **監査対応**: マイグレーションとアプリケーション操作のログ分離が可能
- **本番環境安全性**: 実行中アプリがスキーマを変更できない

#### マイグレーション管理

- **ツール**: Flyway
- **場所**: `migration/sql/`
- **命名規則**: `V{version}__{description}.sql`

**既存マイグレーション**:
- `V1__create_initial_schema.sql`: users、image_memosテーブル作成、インデックス設定

### JPA設定

- **DDL自動生成**: `validate`（マイグレーションのみでスキーマ変更）
- **ネーミング戦略**: CamelCaseToUnderscores（Spring Boot標準）
- **SQLログ**: 有効（`show-sql: true`）
- **フォーマット**: 有効（`format_sql: true`）

### トランザクション管理

**原則**:
- **メソッドレベルで定義**: トランザクション境界を明確化
- **更新系**: `@Transactional`
- **参照系**: `@Transactional(readOnly = true)`

**理由**:
- クラスレベルより境界が明確
- 読み取り専用トランザクションで最適化
- リードレプリカ活用の準備

## AWS S3連携

### S3パス設計

**形式**: `uploads/{userId}/{UUID}.{拡張子}`

**設計理由**:
- **ユーザーIDでパス分離**: 運用性・管理性向上
  - ユーザーごとの一括削除が容易
  - ユーザーごとの容量確認が容易
  - データの可視性向上
- **UUID併用**: 推測攻撃を防止
- **拡張子保持**: Content-Type判定に利用

**例**: `uploads/123/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg`

### 画像配信方式

**アプリケーションProxy方式**を採用（Pre-signed URLではなく）:

**比較**:

| 方式 | メリット | デメリット |
|------|---------|----------|
| Pre-signed URL | アプリケーション負荷低 | URL有効期限管理必要、S3構成が露出 |
| Application Proxy | セキュリティ高、認可制御容易 | アプリケーション負荷あり |

**採用理由**:
- セキュリティ最優先（S3バケット名を隠蔽）
- きめ細かいアクセス制御（所有者チェック）
- ブラウザキャッシュで負荷軽減

### S3Service実装

**主要メソッド**:

| メソッド | 役割 | 例外 |
|---------|------|------|
| `uploadFile(bucket, file, userId)` | S3にアップロード、s3Key返却 | `StorageException` |
| `downloadImageStream(bucket, s3Key)` | S3からストリーミングダウンロード | `StorageException` |
| `deleteFile(bucket, s3Key)` | S3から削除 | `StorageException` |

**ImageDownloadResult**:
- `InputStream inputStream`: 画像データストリーム
- `String contentType`: Content-Type（例: `image/jpeg`）
- `long contentLength`: ファイルサイズ（バイト）

### 環境別設定

**本番環境** (`application.yml`):
```yaml
spring:
  cloud:
    aws:
      region:
        static: ${AWS_REGION:ap-northeast-1}

app:
  s3:
    bucket-name: ${AWS_S3_BUCKET_NAME}  # 必須（デフォルト値なし）
```

**ローカル開発環境** (`application-local.yml`):
```yaml
spring:
  cloud:
    aws:
      region:
        static: ap-northeast-1
      s3:
        endpoint: http://localhost:4566
        bucket-name: handson-app-bucket
        path-style-access-enabled: true  # LocalStack必須
      credentials:
        access-key: test
        secret-key: test
```

**path-style-accessについて**:
- LocalStack: path-style URL（`http://localhost:4566/bucket-name/key`）必須
- AWS本番: virtual-hosted-style（`https://bucket-name.s3.region.amazonaws.com/key`）推奨

---

## 参考資料

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [Spring Cloud AWS](https://docs.awspring.io/spring-cloud-aws/docs/3.4.0/reference/html/index.html)
- [DEVELOPMENT.md](./DEVELOPMENT.md) - 開発ガイド
