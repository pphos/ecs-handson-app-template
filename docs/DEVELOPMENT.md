# 開発ガイド

このドキュメントは、Handson Applicationの開発手順とベストプラクティスを説明します。

## 目次

- [ローカル開発環境](#ローカル開発環境)
- [テスト戦略](#テスト戦略)
- [コーディング規約](#コーディング規約)
- [よくある改修パターン](#よくある改修パターン)

## ローカル開発環境

### クイックスタート（推奨）

```bash
make setup
```

このコマンド1つで以下がすべて自動実行されます:
- Docker環境起動（PostgreSQL + LocalStack）
- データベースマイグレーション実行
- LocalStackの初期化とS3バケット作成

### Makefileコマンド一覧

#### 基本操作

```bash
make help             # ヘルプ表示
make setup            # 初回セットアップ
make build            # 全Dockerイメージビルド（アプリ + Flyway）
```

#### 起動・停止

```bash
make start-local      # ローカル開発起動（インフラコンテナ + アプリローカル実行）
make start-container  # 完全Docker環境起動（全コンテナ起動）
make stop             # 停止
make clean            # 環境完全リセット（ボリューム削除）
```

#### 開発支援

```bash
make test             # テスト実行
make migrate          # マイグレーション実行
```

### 起動パターン

#### パターン1: ローカル開発（推奨）

インフラはコンテナ、アプリはローカルで実行（ホットリロード有効）:

```bash
make setup           # 初回のみ
make start-local     # インフラ起動 + アプリローカル実行
# Ctrl+Cでアプリ停止、インフラは起動したまま
```

**メリット**:
- ホットリロード有効（コード変更が即座に反映）
- IDEのデバッガーが使える
- ログが見やすい

#### パターン2: 完全Docker環境

すべてコンテナで実行（本番環境に近い状態）:

```bash
make setup           # 初回のみ
make build           # イメージビルド
make start-container # 全コンテナ起動
# http://localhost:8080 でアクセス可能
```

**メリット**:
- 本番環境に近い動作確認
- 環境の完全な隔離
- 複数プロジェクトの切り替えが容易

### Docker サービス

| サービス       | ポート | 説明                                                          |
| -------------- | ------ | ------------------------------------------------------------- |
| PostgreSQL     | 5432   | データベース                                                  |
| LocalStack     | 4566   | S3互換サービス                                               |
| app (optional) | 8080   | Spring Bootアプリケーション（`--profile app`指定時のみ起動） |

### プロファイル

- **デフォルト** (`application.yml`): 本番環境想定
- **local** (`application-local.yml`): LocalStack使用

### 環境変数

このアプリケーションは、業界標準の**完全URL方式**で環境変数を設定します。この方式はHeroku、Cloud Run等の主要PaaSプラットフォームで採用されており、12-Factor Appの原則に準拠しています。

#### ローカル開発環境

`application-local.yml`ですべての値が設定済みのため、環境変数不要。

#### 本番環境

**アプリケーションコンテナ**:
```bash
# 必須環境変数
JDBC_DATABASE_URL=jdbc:postgresql://host:5432/handson  # データベース接続URL
JDBC_DATABASE_USERNAME=handson_app                      # アプリケーション用ユーザー
JDBC_DATABASE_PASSWORD=secret                           # パスワード
AWS_S3_BUCKET_NAME=my-bucket                            # S3バケット名

# オプション環境変数
AWS_REGION=ap-northeast-1                               # AWSリージョン（デフォルト: ap-northeast-1）
SPRING_PROFILES_ACTIVE=production                       # Spring Bootプロファイル
```

**マイグレーションコンテナ**:
```bash
# 必須環境変数
JDBC_DATABASE_URL=jdbc:postgresql://host:5432/handson   # データベース接続URL
JDBC_DATABASE_USERNAME=handson_migration                # マイグレーション用ユーザー
JDBC_DATABASE_PASSWORD=secret                           # パスワード
```

#### データベースユーザー権限分離

セキュリティベストプラクティスとして、2つのデータベースユーザーを使い分けています：

| ユーザー名 | 用途 | 権限 | 使用箇所 |
|-----------|------|------|---------|
| `handson_migration` | Flywayマイグレーション | DDL権限（CREATE, DROP, ALTER） | マイグレーションコンテナ |
| `handson_app` | アプリケーション実行 | DML権限のみ（SELECT, INSERT, UPDATE, DELETE） | アプリケーションコンテナ |

**セキュリティメリット**:
- アプリケーションから誤ってテーブルを削除・変更できない
- SQLインジェクション対策（DROP TABLEなどのDDL攻撃を防止）
- 本番環境でアプリがスキーマを変更できない

#### 実装詳細

**application.yml**での設定:
```yaml
spring:
  datasource:
    url: ${JDBC_DATABASE_URL}
    username: ${JDBC_DATABASE_USERNAME}
    password: ${JDBC_DATABASE_PASSWORD}
```

**compose.yml**でのFlyway設定:
```yaml
flyway:
  environment:
    FLYWAY_URL: ${JDBC_DATABASE_URL:-jdbc:postgresql://db:5432/handson}
    FLYWAY_USER: ${JDBC_DATABASE_USERNAME:-handson_migration}
    FLYWAY_PASSWORD: ${JDBC_DATABASE_PASSWORD:-migration_password}
```

デフォルト値はローカル開発環境用に設定されており、本番環境では環境変数で上書きします。

## テスト戦略

### TDDアプローチ

このプロジェクトはTest-Driven Development（TDD）で開発されています。

**TDDサイクル**:
1. **Red**: テストを先に書いて失敗させる
2. **Green**: 最小限の実装でテストをパス
3. **Refactor**: コードを洗練させる

### テストカバレッジ

#### リポジトリ層テスト（Testcontainers使用）

**実データベース**を使用してテスト:

**UserRepositoryTest**:
- ユーザー保存
- ユーザー名での検索
- ユーザー名一意制約

**ImageMemoRepositoryTest**:
- 画像メモCRUD操作
- ユーザー関連検索
- N+1クエリ対策テスト（JOIN FETCH検証）

**実装例**:
```java
@DataJpaTest
@Testcontainers
class ImageMemoRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17");

    @Test
    void findByUserIdOrderByCreatedAtDesc_shouldFetchUserEagerly() {
        // N+1クエリが発生しないことを検証
    }
}
```

#### サービス層テスト（モック使用）

**ビジネスロジック**をモック・スタブで高速テスト:

**UserServiceTest**:
- ユーザー登録
- ユーザー検索
- パスワードハッシュ化検証
- null安全性

**ImageMemoServiceTest**:
- 画像メモ作成（S3アップロード + DB保存）
- 画像メモ削除（S3削除 + DB削除）
- トランザクション境界の確認

**S3ServiceTest**:
- S3アップロード
- S3削除
- ストリーミングダウンロード
- 例外処理（StorageException）

**実装例**:
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUser_shouldHashPasswordAndSave() {
        // パスワードハッシュ化のテスト
    }
}
```

#### コントローラー層テスト（MockMvc使用）

**HTTPリクエスト/レスポンス**をテスト:

**ImageMemoControllerTest**:
- 画像配信エンドポイント（`GET /memos/{id}/image`）
- 認証チェック（未認証は401）
- 認可チェック（所有者のみ200、他は404）
- エラーハンドリング

**実装例**:
```java
@WebMvcTest(ImageMemoController.class)
class ImageMemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageMemoService imageMemoService;

    @Test
    @WithMockUser(username = "user1")
    void getImage_shouldReturn200_whenOwner() throws Exception {
        // 所有者は画像を取得できる
    }
}
```

#### インフラ層テスト

**DatabaseConnectionTest**:
- PostgreSQL接続確認
- Testcontainers動作確認

**S3ConnectionTest**:
- LocalStack S3接続確認
- バケット操作確認

### テスト実行

```bash
# 全テスト実行
./gradlew test

# 特定テスト実行
./gradlew test --tests UserServiceTest

# カバレッジレポート生成
./gradlew test jacocoTestReport
# build/reports/jacoco/test/html/index.html で確認
```

### Testcontainers注意事項

- Docker Desktopが起動している必要がある
- 初回実行時はイメージダウンロードで時間がかかる
- ディスク容量を確保すること

詳細は`/test`カスタムコマンドを参照。

## コーディング規約

### Lombokアノテーション

#### Entity

```java
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {
    // フィールド定義
}
```

- `@Getter`: Getterメソッド自動生成
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`: JPA要件（外部からは使えない）
- `@AllArgsConstructor`: 全フィールドコンストラクタ（Builderと併用）
- `@Builder`: Builderパターン

#### Service/Controller

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(String username, String password) {
        log.info("Registering user: {}", username);
        // 実装
    }
}
```

- `@RequiredArgsConstructor`: `final`フィールドのコンストラクタ自動生成（DI）
- `@Slf4j`: ロガー自動生成（`log`変数が使える）

### トランザクション

**原則**: メソッドレベルで明示的に定義（クラスレベルではない）

**更新系**:
```java
@Transactional
public User registerUser(String username, String password) {
    // 更新処理
}
```

**参照系**:
```java
@Transactional(readOnly = true)
public User findByUsername(String username) {
    // 参照処理
}
```

**理由**:
- トランザクション境界が明確になる
- 読み取り専用トランザクションで最適化可能
- リードレプリカ活用の準備（将来）

### ロギング

```java
@Slf4j
public class SomeService {

    public void someMethod(String param) {
        log.info("情報メッセージ: param={}", param);
        log.warn("警告メッセージ: param={}", param);

        try {
            // 処理
        } catch (Exception e) {
            log.error("エラーメッセージ", e);
            throw e;
        }
    }
}
```

**レベルの使い分け**:
- `DEBUG`: 詳細なデバッグ情報
- `INFO`: 通常の情報（リクエスト処理開始/終了等）
- `WARN`: 警告（リトライ可能なエラー等）
- `ERROR`: エラー（例外スタックトレース含む）

### バリデーション

#### DTOレベル（Bean Validation）

```java
public record ImageMemoCreateDto(
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 255, message = "タイトルは255文字以内です")
    String title,

    @Size(max = 1000, message = "説明は1000文字以内です")
    String description,

    @NotNull(message = "画像ファイルは必須です")
    MultipartFile image
) {}
```

**使用可能なアノテーション**:
- `@NotNull`: null禁止
- `@NotBlank`: 空文字・null禁止
- `@Size(min, max)`: 文字列長・コレクションサイズ
- `@Min`, `@Max`: 数値範囲
- `@Email`: メールアドレス形式
- `@Pattern(regexp)`: 正規表現

**Note**: Java Recordを使用する場合は、コンストラクタ引数にアノテーションを付与します

#### サービスレベル（ビジネスルール検証）

```java
@Transactional
public ImageMemo createImageMemo(ImageMemoCreateDto dto, Long userId) {
    if (dto.getImageFile().isEmpty()) {
        throw new IllegalArgumentException("画像ファイルが空です");
    }

    // ビジネスロジック
}
```

### null安全性

**原則**: 戻り値でnullを返さない

**良い例**:
```java
@Transactional(readOnly = true)
public User findByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException(
            "User not found: " + username));
}
```

**悪い例**:
```java
public User findByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElse(null);  // nullを返すとNPE発生リスク
}
```

### 例外処理

#### カスタム例外

**StorageException** (`src/main/java/com/example/handson/exception/StorageException.java`):
- S3などの外部ストレージ操作エラー
- チェック例外（IOException等）をラップ

```java
public class StorageException extends RuntimeException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### ビジネスロジック例外

**IllegalArgumentException**:
- バリデーションエラー
- データ不整合
- 権限エラー

```java
if (!memo.getUser().getId().equals(userId)) {
    throw new IllegalArgumentException(
        "User does not have permission to delete this memo");
}
```

#### 例外のラップ

**原則**: チェック例外を適切にラップして、コンテキストを提供

```java
try {
    s3Client.putObject(request, RequestBody.fromInputStream(
        file.getInputStream(), file.getSize()));
} catch (IOException e) {
    throw new StorageException("Failed to upload file to S3", e);
}
```

## よくある改修パターン

### 新しいエンティティ追加

**手順**:

1. **Flywayマイグレーションファイル作成**
   ```sql
   -- migration/sql/V2__create_tags_table.sql
   CREATE TABLE tags (
       id BIGSERIAL PRIMARY KEY,
       name VARCHAR(50) NOT NULL UNIQUE,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
   );
   CREATE INDEX idx_tags_name ON tags(name);
   ```

2. **エンティティクラス作成**
   ```java
   // src/main/java/com/example/handson/domain/tag/Tag.java
   @Entity
   @Table(name = "tags")
   @Getter
   @NoArgsConstructor(access = AccessLevel.PROTECTED)
   @AllArgsConstructor
   @Builder
   public class Tag {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;

       @Column(nullable = false, unique = true, length = 50)
       private String name;

       @CreationTimestamp
       @Column(name = "created_at", nullable = false, updatable = false)
       private LocalDateTime createdAt;

       @UpdateTimestamp
       @Column(name = "updated_at", nullable = false)
       private LocalDateTime updatedAt;
   }
   ```

3. **Repositoryインターフェース作成**
   ```java
   // src/main/java/com/example/handson/domain/tag/TagRepository.java
   public interface TagRepository extends JpaRepository<Tag, Long> {
       Optional<Tag> findByName(String name);
   }
   ```

4. **Serviceクラス作成**
   ```java
   // src/main/java/com/example/handson/service/TagService.java
   @Service
   @RequiredArgsConstructor
   @Slf4j
   public class TagService {

       private final TagRepository tagRepository;

       @Transactional
       public Tag createTag(String name) {
           log.info("Creating tag: {}", name);
           // ビジネスロジック実装
       }
   }
   ```

5. **Controllerクラス作成**（必要に応じて）

6. **DTOクラス作成**（必要に応じて）

7. **Thymeleafテンプレート作成**（必要に応じて）

**TDD順序**: Repository Test → Service Test → 実装 → Controller（手動テスト）

### 画像メモに新しいフィールド追加

**例**: `priority`フィールド（優先度）を追加

1. **マイグレーションファイル作成**
   ```sql
   -- migration/sql/V2__add_priority_to_image_memos.sql
   ALTER TABLE image_memos
   ADD COLUMN priority INTEGER DEFAULT 0 NOT NULL;
   CREATE INDEX idx_image_memos_priority ON image_memos(priority);
   ```

2. **ImageMemo.javaにフィールド追加**
   ```java
   @Column(nullable = false)
   private Integer priority = 0;
   ```

3. **ImageMemoCreateDto更新** (Java Record)
   ```java
   public record ImageMemoCreateDto(
       @NotBlank @Size(max = 255) String title,
       @Size(max = 1000) String description,
       @NotNull MultipartFile image,
       @Min(value = 0, message = "優先度は0以上です")
       @Max(value = 5, message = "優先度は5以下です")
       Integer priority
   ) {}
   ```

4. **ImageMemoService.createImageMemo()更新**
   ```java
   ImageMemo memo = ImageMemo.builder()
       .user(user)
       .title(dto.title())  // Record形式
       .description(dto.description())
       .priority(dto.priority())  // 追加
       .s3Key(s3Key)
       .build();
   ```

5. **memos/create.htmlフォーム更新**
   ```html
   <label for="priority">優先度:</label>
   <select id="priority" name="priority">
       <option value="0">低</option>
       <option value="1">中</option>
       <option value="2">高</option>
   </select>
   ```

6. **memos/detail.html表示更新**
   ```html
   <p>優先度: <span th:text="${memo.priority}"></span></p>
   ```

### S3以外のストレージ対応

**例**: ローカルファイルシステム対応

1. **StorageServiceインターフェース作成**
   ```java
   public interface StorageService {
       String uploadFile(MultipartFile file, Long userId);
       ImageDownloadResult downloadImageStream(String key);
       void deleteFile(String key);
   }
   ```

2. **S3StorageServiceImpl実装**
   ```java
   @Service
   @Profile("!local-storage")
   public class S3StorageServiceImpl implements StorageService {
       // 既存のS3Service実装を移行
   }
   ```

3. **LocalStorageServiceImpl実装**
   ```java
   @Service
   @Profile("local-storage")
   public class LocalStorageServiceImpl implements StorageService {
       // ローカルファイルシステム実装
   }
   ```

4. **ImageMemoServiceでStorageService使用**
   ```java
   @Service
   @RequiredArgsConstructor
   public class ImageMemoService {

       private final StorageService storageService;  // インターフェース注入

       @Transactional
       public ImageMemo createImageMemo(ImageMemoCreateDto dto, Long userId) {
           String key = storageService.uploadFile(dto.getImageFile(), userId);
           // ...
       }
   }
   ```

### 画像編集機能追加

**Note**: ImageMemoエンティティはLombokの`@Getter`のみで、セッターは提供していません。更新機能を実装する場合は、エンティティにセッターメソッドを追加するか、Builderパターンで新しいインスタンスを生成する必要があります。

1. **GET /memos/{id}/edit エンドポイント追加**
   ```java
   @GetMapping("/{id}/edit")
   public String showEditForm(@PathVariable Long id, Model model,
                              @AuthenticationPrincipal UserDetails userDetails) {
       User user = userService.findByUsername(userDetails.getUsername());
       ImageMemo memo = imageMemoService.findByIdAndUserId(id, user.getId());
       model.addAttribute("memo", memo);
       return "memos/edit";
   }
   ```

2. **ImageMemoUpdateDto作成** (Java Record)
   ```java
   public record ImageMemoUpdateDto(
       @NotBlank @Size(max = 255) String title,
       @Size(max = 1000) String description,
       MultipartFile image  // オプショナル
   ) {}
   ```

3. **ImageMemo.javaにセッターメソッド追加** (更新用)
   ```java
   public void updateTitle(String title) {
       this.title = title;
   }

   public void updateDescription(String description) {
       this.description = description;
   }

   public void updateS3Key(String s3Key) {
       this.s3Key = s3Key;
   }
   ```

4. **POST /memos/{id} エンドポイント追加**
   ```java
   @PostMapping("/{id}")
   public String updateMemo(@PathVariable Long id,
                            @Valid @ModelAttribute ImageMemoUpdateDto dto,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
       User user = userService.findByUsername(userDetails.getUsername());
       imageMemoService.updateImageMemo(id, dto, user);
       redirectAttributes.addFlashAttribute("successMessage", "画像メモを更新しました");
       return "redirect:/memos/" + id;
   }
   ```

5. **ImageMemoService.updateImageMemo()実装**
   ```java
   @Transactional
   public ImageMemo updateImageMemo(Long id, ImageMemoUpdateDto dto, User user) {
       ImageMemo memo = findByIdAndUserId(id, user.getId());
       memo.updateTitle(dto.title());
       memo.updateDescription(dto.description());

       // 画像が更新された場合
       if (dto.image() != null && !dto.image().isEmpty()) {
           s3Service.deleteFile(bucketName, memo.getS3Key());
           String newS3Key = s3Service.uploadFile(bucketName, dto.image(), user.getId().toString());
           memo.updateS3Key(newS3Key);
       }

       return imageMemoRepository.save(memo);
   }
   ```

4. **memos/edit.htmlテンプレート作成**

5. **画像更新時の旧画像削除処理追加**（上記で実装済み）

---

## 参考資料

- [ARCHITECTURE.md](./ARCHITECTURE.md) - アーキテクチャ詳細
- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) - トラブルシューティング
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Testcontainers](https://testcontainers.com/)
