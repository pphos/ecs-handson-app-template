
# Handson Application - 機能概要

## アプリケーション概要

画像とメモを組み合わせて保存・管理できるWebアプリケーション。Spring Boot 3.5.7をベースに、セキュアでスケーラブルなアーキテクチャで構築されています。

---

## システムアーキテクチャ

```mermaid
graph TB
    subgraph "プレゼンテーション層"
        HC[HomeController]
        UC[UserController]
        IMC[ImageMemoController]
    end

    subgraph "サービス層"
        US[UserService]
        IMS[ImageMemoService]
        S3S[S3Service]
    end

    subgraph "ドメイン層"
        UR[UserRepository]
        IMR[ImageMemoRepository]
        UE[User Entity]
        IME[ImageMemo Entity]
    end

    subgraph "インフラ層"
        DB[(PostgreSQL)]
        S3[(AWS S3)]
    end

    HC --> IMS
    UC --> US
    IMC --> IMS
    IMC --> S3S

    US --> UR
    IMS --> IMR
    IMS --> S3S

    UR --> DB
    IMR --> DB
    S3S --> S3

    UR -.-> UE
    IMR -.-> IME
    IME -.-> UE
```

---

## 主要機能

### 1. ユーザー管理機能

#### ユーザー登録フロー

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant UserController
    participant UserService
    participant UserRepository
    participant DB

    User->>Browser: ユーザー登録フォーム入力
    Browser->>UserController: POST /register
    UserController->>UserController: バリデーション
    UserController->>UserService: registerUser()
    UserService->>UserService: BCryptでパスワードハッシュ化
    UserService->>UserRepository: findByUsername()
    UserRepository->>DB: SELECT
    DB-->>UserRepository: ユーザー存在チェック
    alt ユーザー名重複
        UserRepository-->>UserService: 既存ユーザー
        UserService-->>UserController: 例外スロー
        UserController-->>Browser: エラーメッセージ
    else 新規ユーザー
        UserService->>UserRepository: save()
        UserRepository->>DB: INSERT
        DB-->>UserRepository: 保存完了
        UserRepository-->>UserService: User
        UserService-->>UserController: User
        UserController->>UserController: 自動ログイン処理
        UserController-->>Browser: リダイレクト /memos
    end
```

#### 1.1 ユーザー登録
- **エンドポイント**: `POST /register`
- **機能説明**:
  - 新規アカウント作成（ユーザー名とパスワード）
  - パスワード確認入力によるタイプミス防止
  - ユーザー名の重複チェック
  - BCryptによるパスワードハッシュ化
  - 登録後の自動リダイレクト

#### 1.2 ログイン/ログアウト
- **エンドポイント**:
  - `GET /login` - ログインフォーム表示
  - `POST /login` - ログイン処理（Spring Security）
  - `POST /logout` - ログアウト処理
- **機能説明**:
  - フォームベース認証
  - セッション管理
  - ログイン成功時に画像メモ一覧へリダイレクト
  - わかりやすいエラーメッセージ表示

---

### 2. 画像メモ管理機能

#### 画像メモ作成フロー

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant ImageMemoController
    participant ImageMemoService
    participant S3Service
    participant S3
    participant ImageMemoRepository
    participant DB

    User->>Browser: 画像メモ作成フォーム入力
    Browser->>ImageMemoController: POST /memos (title, description, image)
    ImageMemoController->>ImageMemoController: バリデーション
    ImageMemoController->>ImageMemoService: createImageMemo()

    ImageMemoService->>S3Service: uploadFile()
    S3Service->>S3Service: S3キー生成 (uploads/{userId}/{UUID}.{ext})
    S3Service->>S3: PUT Object
    S3-->>S3Service: アップロード成功
    S3Service-->>ImageMemoService: s3Key

    ImageMemoService->>ImageMemoRepository: save()
    ImageMemoRepository->>DB: INSERT (title, description, s3Key, userId)
    DB-->>ImageMemoRepository: 保存完了
    ImageMemoRepository-->>ImageMemoService: ImageMemo
    ImageMemoService-->>ImageMemoController: ImageMemo
    ImageMemoController-->>Browser: リダイレクト /memos (成功メッセージ)
    Browser-->>User: 一覧ページ表示
```

#### 画像メモ削除フロー

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant ImageMemoController
    participant ImageMemoService
    participant S3Service
    participant S3
    participant ImageMemoRepository
    participant DB

    User->>Browser: 削除ボタンクリック
    Browser->>Browser: JavaScript確認ダイアログ
    Browser->>ImageMemoController: POST /memos/{id}/delete
    ImageMemoController->>ImageMemoService: deleteImageMemo()

    ImageMemoService->>ImageMemoRepository: findByIdAndUserId()
    ImageMemoRepository->>DB: SELECT (所有者チェック)
    DB-->>ImageMemoRepository: ImageMemo
    ImageMemoRepository-->>ImageMemoService: ImageMemo

    ImageMemoService->>S3Service: deleteFile(s3Key)
    S3Service->>S3: DELETE Object
    alt S3削除失敗
        S3-->>S3Service: エラー
        S3Service-->>ImageMemoService: StorageException
        ImageMemoService-->>ImageMemoController: トランザクションロールバック
        ImageMemoController-->>Browser: エラーメッセージ
    else S3削除成功
        S3-->>S3Service: 削除完了
        S3Service-->>ImageMemoService: 成功
        ImageMemoService->>ImageMemoRepository: delete()
        ImageMemoRepository->>DB: DELETE
        DB-->>ImageMemoRepository: 削除完了
        ImageMemoRepository-->>ImageMemoService: 完了
        ImageMemoService-->>ImageMemoController: 完了
        ImageMemoController-->>Browser: リダイレクト /memos (成功メッセージ)
    end
```

#### 画像配信フロー（アプリケーションProxyパターン）

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant SpringSecurity
    participant ImageMemoController
    participant ImageMemoService
    participant S3Service
    participant S3

    User->>Browser: 画像表示要求
    Browser->>SpringSecurity: GET /memos/{id}/image
    SpringSecurity->>SpringSecurity: 認証チェック
    alt 未認証
        SpringSecurity-->>Browser: 401 Unauthorized
    else 認証済み
        SpringSecurity->>ImageMemoController: リクエスト転送
        ImageMemoController->>ImageMemoService: findByIdAndUserId()
        alt 所有者でない
            ImageMemoService-->>ImageMemoController: 404 Not Found
            ImageMemoController-->>Browser: 404エラー
        else 所有者
            ImageMemoService-->>ImageMemoController: ImageMemo (s3Key含む)
            ImageMemoController->>S3Service: downloadImageStream(s3Key)
            S3Service->>S3: GET Object (ストリーミング)
            S3-->>S3Service: InputStream + メタデータ
            S3Service-->>ImageMemoController: ImageDownloadResult
            ImageMemoController->>ImageMemoController: Cache-Controlヘッダー設定
            ImageMemoController-->>Browser: InputStreamResource (画像データ)
            Browser-->>User: 画像表示
        end
    end
```

#### 2.1 画像メモ作成
- **エンドポイント**:
  - `GET /memos/new` - 作成フォーム表示
  - `POST /memos` - 新規作成処理
- **機能説明**:
  - タイトル入力（必須、最大255文字）
  - 説明入力（任意、TEXT型で無制限）
  - 画像ファイルアップロード（必須）
  - 対応形式: JPG, JPEG, PNG, GIF
  - 最大ファイルサイズ: 10MB
  - S3への自動アップロード
  - データベースへのメタデータ保存
- **S3キー形式**: `uploads/{userId}/{UUID}.{拡張子}`
  - ユーザーIDでパス分離
  - UUIDによる一意性保証
  - 拡張子保持でContent-Type自動判定

#### 2.2 画像メモ一覧表示
- **エンドポイント**: `GET /memos`
- **機能説明**:
  - ログインユーザーの全画像メモを表示
  - グリッドレイアウトによる見やすい表示
  - サムネイル画像プレビュー
  - タイトル、説明（100文字まで）、作成日時を表示
  - 作成日時の降順でソート（最新が上）
  - ページネーション対応（10件/ページ）
  - 各メモをクリックで詳細表示へ
- **パフォーマンス最適化**:
  - JOIN FETCHによるN+1クエリ対策
  - ページングでデータベース負荷を軽減

#### 2.3 画像メモ詳細表示
- **エンドポイント**: `GET /memos/{id}`
- **機能説明**:
  - フルサイズ画像表示
  - 完全なタイトルと説明文
  - 作成日時表示
  - アクションボタン（一覧に戻る、削除）
  - 所有者チェック（自分のメモのみ閲覧可能）

#### 2.4 画像メモ削除
- **エンドポイント**: `POST /memos/{id}/delete`
- **機能説明**:
  - JavaScriptによる確認ダイアログ表示
  - 所有者検証（自分のメモのみ削除可能）
  - S3から画像を削除
  - データベースからレコードを削除
  - トランザクション制御（S3削除失敗時はDB削除もロールバック）
  - 成功メッセージ表示後、一覧へリダイレクト

#### 2.5 セキュアな画像配信
- **エンドポイント**: `GET /memos/{id}/image`
- **機能説明**:
  - アプリケーションProxyパターンによる配信
  - 認証チェック（未ログインは401エラー）
  - 所有者チェック（他人の画像は404エラー）
  - S3からストリーミングダウンロード
  - InputStreamResourceによるメモリ効率的な配信
  - ブラウザキャッシュ対応（1時間、private）
  - Content-Type自動設定
- **セキュリティメリット**:
  - S3バケット名を外部に露出しない
  - S3をプライベート設定のまま利用可能
  - アプリケーション層できめ細かいアクセス制御

---

### 3. セキュリティ機能

#### 認証・認可フロー

```mermaid
graph LR
    A[ユーザー] -->|リクエスト| B{Spring Security}
    B -->|認証必要| C{ログイン済み?}
    C -->|No| D[/login へリダイレクト]
    C -->|Yes| E{CSRF Token検証}
    E -->|Invalid| F[403 Forbidden]
    E -->|Valid| G{所有者チェック}
    G -->|他人のリソース| H[404 Not Found]
    G -->|自分のリソース| I[処理実行]
    B -->|公開リソース| I
```

#### 3.1 認証機能
- **実装**: Spring Security
- **認証方式**: フォームベース認証
- **パスワード管理**:
  - BCryptアルゴリズムによるハッシュ化
  - ソルト付きハッシュで辞書攻撃対策
  - 平文保存なし

#### 3.2 アクセス制御

| パス | アクセス権限 | 備考 |
|------|------------|------|
| `/` | 全員 | トップページ |
| `/register` | 全員 | ユーザー登録 |
| `/login` | 全員 | ログインページ |
| `/css/**`, `/js/**`, `/images/**` | 全員 | 静的リソース |
| `/actuator/health` | 全員 | ヘルスチェック |
| 上記以外すべて | 認証済みユーザーのみ | ログイン必須 |

#### 3.3 認可機能
- **所有者ベースのアクセス制御**:
  - ユーザーは自分の画像メモのみ閲覧・削除可能
  - すべてのクエリで`userId`によるフィルタリング
  - 不正アクセス時は404を返却（情報漏洩防止）

#### 3.4 CSRF保護
- デフォルトで有効
- Thymeleafが自動的にトークン処理

---

### 4. データ管理機能

#### データモデル（ER図）

```mermaid
erDiagram
    USERS ||--o{ IMAGE_MEMOS : owns

    USERS {
        bigint id PK
        varchar username UK "最大50文字"
        varchar password "BCryptハッシュ"
        boolean enabled
        timestamp created_at
        timestamp updated_at
    }

    IMAGE_MEMOS {
        bigint id PK
        bigint user_id FK
        varchar title "最大255文字"
        text description "無制限"
        varchar s3_key "最大500文字"
        timestamp created_at
        timestamp updated_at
    }
```

#### クエリ最適化戦略

```mermaid
graph TB
    subgraph "N+1クエリ対策"
        A[一覧取得リクエスト] --> B[JOIN FETCH使用]
        B --> C[ImageMemo + User を1クエリで取得]
        C --> D[追加クエリなし]
    end

    subgraph "従来の方式（N+1問題あり）"
        E[一覧取得リクエスト] --> F[ImageMemoのみ取得]
        F --> G[各ImageMemoのUser取得]
        G --> H[N回の追加クエリ発生]
    end
```

#### 4.1 データベース構成
- **RDBMS**: PostgreSQL 17
- **ORM**: Spring Data JPA / Hibernate
- **マイグレーション**: Flyway

**エンティティ構造**:

**Userエンティティ**:
```
- id: 主キー（自動採番）
- username: ユーザー名（一意、最大50文字）
- password: BCryptハッシュ化パスワード（255文字）
- enabled: アカウント有効フラグ
- createdAt: 作成日時（自動設定）
- updatedAt: 更新日時（自動更新）
```

**ImageMemoエンティティ**:
```
- id: 主キー（自動採番）
- user: 所有者（ManyToOne, LAZY fetch）
- title: タイトル（最大255文字）
- description: 説明（TEXT型、無制限）
- s3Key: S3オブジェクトキー（最大500文字）
- createdAt: 作成日時（自動設定）
- updatedAt: 更新日時（自動更新）
```

**リレーション**:
- 1人のユーザー → 複数の画像メモ（One-to-Many）
- 画像メモは必ず1人のユーザーに紐づく（ManyToOne）

#### 4.2 トランザクション管理
- **戦略**: メソッドレベルのトランザクション境界
- **読み取り専用**: `@Transactional(readOnly = true)`
- **更新系**: `@Transactional`
- **ロールバック**:
  - S3アップロード失敗 → DB保存なし
  - S3削除失敗 → DB削除もロールバック

#### 4.3 クエリ最適化
- **N+1クエリ対策**: JOIN FETCHでUserデータを事前取得
- **ページネーション**: デフォルト10件/ページ
- **ソート**: 作成日時降順（最新が先頭）

---

### 5. クラウド連携機能

#### S3統合アーキテクチャ

```mermaid
graph TB
    subgraph "Application Layer"
        S3Service[S3Service]
    end

    subgraph "Spring Cloud AWS"
        S3Template[S3Template]
    end

    subgraph "本番環境"
        S3Prod[AWS S3<br/>バケット: handson-app-bucket<br/>リージョン: ap-northeast-1]
    end

    subgraph "ローカル環境"
        LocalStack[LocalStack S3<br/>endpoint: http://localhost:4566<br/>path-style-access: true]
    end

    S3Service --> S3Template
    S3Template -->|本番profile| S3Prod
    S3Template -->|localプロファイル| LocalStack

    style S3Prod fill:#FF9900
    style LocalStack fill:#00C7B7
```

#### S3パス設計

```mermaid
graph LR
    A[S3 Bucket] --> B[uploads/]
    B --> C[user_1/]
    B --> D[user_2/]
    B --> E[user_N/]

    C --> F[uuid1.jpg]
    C --> G[uuid2.png]

    D --> H[uuid3.gif]

    E --> I[uuid4.jpeg]

    style A fill:#FF9900
    style B fill:#FFA500
    style C fill:#FFD700
    style D fill:#FFD700
    style E fill:#FFD700
```

#### 5.1 AWS S3統合
- **ライブラリ**: Spring Cloud AWS 3.4.0
- **主要機能**:
  - 画像アップロード（S3Template使用）
  - 画像ダウンロード（ストリーミング）
  - 画像削除
  - Content-Type自動判定
  - エラーハンドリング（StorageException）

#### 5.2 環境対応
- **本番環境**: AWS S3（`application.yml`）
- **ローカル環境**: LocalStack（`application-local.yml`）
  - Path-style URL対応
  - テストアクセスキー使用

---

### 6. 監視・運用機能

#### ヘルスチェック構成

```mermaid
graph TB
    subgraph "外部監視システム"
        LB[ロードバランサー]
        K8S[Kubernetes/ECS]
        Monitor[監視システム]
    end

    subgraph "Spring Boot Actuator"
        Health[/actuator/health]
        Info[/actuator/info]
    end

    subgraph "ヘルスインジケーター"
        DB[Database Health]
        Disk[Disk Space Health]
        Custom[Custom Indicators]
    end

    LB -.->|ヘルスチェック| Health
    K8S -.->|Liveness/Readiness| Health
    Monitor -.->|監視| Health
    Monitor -.->|メトリクス取得| Info

    Health --> DB
    Health --> Disk
    Health --> Custom

    style Health fill:#4CAF50
    style Info fill:#2196F3
```

#### 6.1 ヘルスチェック
- **フレームワーク**: Spring Boot Actuator
- **エンドポイント**:
  - `/actuator/health` - ヘルスステータス
    - 未認証: ステータスのみ（UP/DOWN）
    - 認証済み: 詳細情報（DB接続、ディスク容量等）
  - `/actuator/info` - アプリケーション情報
- **用途**:
  - コンテナオーケストレーション（Docker, ECS, Kubernetes）
  - ロードバランサーのヘルスチェック
  - 監視システム連携

#### 6.2 ログ機能
- **フレームワーク**: SLF4J + Logback
- **ログレベル**:
  - `INFO`: 成功した操作（ユーザー登録、メモ作成、S3操作）
  - `ERROR`: 失敗（バリデーションエラー、S3エラー）
  - `WARN`: セキュリティイベント（不正アクセス試行）

---

### 7. UI/UX機能

#### テンプレート構成

```mermaid
graph TB
    subgraph "レイアウト"
        Base[layout/base.html<br/>共通レイアウトベース]
    end

    subgraph "フラグメント"
        Common[fragments/common.html<br/>再利用可能コンポーネント]
    end

    subgraph "ページテンプレート"
        Home[index.html]
        Login[login.html]
        Register[register.html]
        MemoList[memos/list.html]
        MemoDetail[memos/detail.html]
        MemoCreate[memos/create.html]
    end

    Base -.->|継承| Home
    Base -.->|継承| Login
    Base -.->|継承| Register
    Base -.->|継承| MemoList
    Base -.->|継承| MemoDetail
    Base -.->|継承| MemoCreate

    Common -.->|フラグメント使用| MemoList
    Common -.->|フラグメント使用| MemoDetail
    Common -.->|フラグメント使用| MemoCreate
    Common -.->|フラグメント使用| Login
    Common -.->|フラグメント使用| Register

    style Base fill:#9C27B0
    style Common fill:#673AB7
```

#### 7.1 テンプレート構成
- **エンジン**: Thymeleaf
- **デザインシステム**: Material Icons + カスタムCSS
- **再利用可能コンポーネント**:
  - アラートメッセージ（成功/エラー/情報）
  - アイコン付きページタイトル
  - アイコン付きボタン
  - レイアウトベーステンプレート

#### 7.2 ユーザーフィードバック
- フラッシュメッセージ（成功/エラー状態）
- インラインバリデーションエラー表示
- 確認ダイアログ（削除時）
- レスポンシブデザイン
- ホバーエフェクト

---

### 8. 開発・テスト機能

#### Docker Compose構成

```mermaid
graph TB
    subgraph "Docker Compose環境"
        subgraph "デフォルト起動"
            DB[PostgreSQL<br/>port: 5432]
            LS[LocalStack S3<br/>port: 4566]
        end

        subgraph "オプション起動（--profile app）"
            APP[Spring Boot App<br/>port: 8080]
        end
    end

    APP -->|depends_on| DB
    APP -->|depends_on| LS

    APP -.->|JDBC接続| DB
    APP -.->|S3 API| LS

    style DB fill:#336791
    style LS fill:#00C7B7
    style APP fill:#6DB33F
```

#### テスト戦略

```mermaid
graph TB
    subgraph "テストピラミッド"
        E2E[E2Eテスト<br/>未実装]

        subgraph "統合テスト"
            Controller[Controllerテスト<br/>MockMvc]
            Infra[インフラテスト<br/>接続確認]
        end

        subgraph "単体テスト"
            Service[Serviceテスト<br/>Mockito]
            Repo[Repositoryテスト<br/>Testcontainers]
        end
    end

    E2E --> Controller
    Controller --> Service
    Controller --> Infra
    Service --> Repo

    style E2E fill:#FF5722
    style Controller fill:#FF9800
    style Infra fill:#FF9800
    style Service fill:#4CAF50
    style Repo fill:#4CAF50
```

#### 8.1 ローカル開発環境
- **Docker Compose構成**:
  - PostgreSQLコンテナ
  - LocalStackコンテナ（AWS S3エミュレーション）
  - オプション: アプリケーションコンテナ
- **Makefileコマンド**:
  - `make setup`: 環境構築から起動まで一括実行
  - `make start`: インフラのみ起動
  - `make start-all`: 全サービス起動
  - `make stop`: 停止
  - `make clean`: 環境完全リセット
  - `make test`: テスト実行

#### 8.2 テスト戦略
- **TDDアプローチ**: テスト駆動開発
- **テスト種類**:
  1. **リポジトリテスト** (Testcontainers)
     - 実PostgreSQLデータベース使用
     - CRUD操作検証
     - N+1クエリ検証
  2. **サービステスト** (Mockito)
     - ビジネスロジック検証
     - S3連携（モック）
     - トランザクション動作
  3. **コントローラーテスト** (MockMvc)
     - HTTPエンドポイントテスト
     - 認証/認可
     - エラーハンドリング
  4. **インフラテスト**
     - データベース接続確認
     - S3接続確認（LocalStack）

#### 8.3 コンテナ化機能

**Dockerマルチステージビルド**:

```mermaid
graph LR
    subgraph "Stage 1: Builder"
        A[eclipse-temurin:21-jdk] --> B[ソースコピー]
        B --> C[Gradle Build]
        C --> D[レイヤー展開]
    end

    subgraph "Stage 2: Runtime"
        E[eclipse-temurin:21-jre] --> F[依存関係レイヤー]
        F --> G[Spring Bootローダー]
        G --> H[スナップショット依存関係]
        H --> I[アプリケーションレイヤー]
    end

    D -.->|COPY --from=builder| F
    D -.->|COPY --from=builder| G
    D -.->|COPY --from=builder| H
    D -.->|COPY --from=builder| I

    style A fill:#FF9800
    style E fill:#4CAF50
```

- **Dockerfile特徴**:
  - マルチステージビルド（builder + runtime）
  - レイヤードJARパターン（高速リビルド）
  - 非rootユーザーで実行（セキュリティ）
  - JREのみのランタイム（軽量化）
  - コンテナ最適化JVM設定
- **イメージサイズ**: 約572MB

---

## デプロイメントアーキテクチャ（想定）

```mermaid
graph TB
    subgraph "AWS Cloud"
        subgraph "VPC"
            subgraph "Public Subnet"
                ALB[Application Load Balancer]
            end

            subgraph "Private Subnet"
                ECS1[ECS Task 1<br/>Spring Boot App]
                ECS2[ECS Task 2<br/>Spring Boot App]
                ECS3[ECS Task N<br/>Spring Boot App]
            end

            subgraph "Database Subnet"
                RDS[(RDS PostgreSQL)]
            end
        end

        S3Bucket[(S3 Bucket<br/>handson-app-bucket)]
        ECR[ECR<br/>Dockerイメージレジストリ]
    end

    Users[ユーザー] -->|HTTPS| ALB
    ALB -->|ヘルスチェック| ECS1
    ALB -->|ヘルスチェック| ECS2
    ALB -->|ヘルスチェック| ECS3
    ALB --> ECS1
    ALB --> ECS2
    ALB --> ECS3

    ECS1 --> RDS
    ECS2 --> RDS
    ECS3 --> RDS

    ECS1 --> S3Bucket
    ECS2 --> S3Bucket
    ECS3 --> S3Bucket

    ECR -.->|イメージPull| ECS1
    ECR -.->|イメージPull| ECS2
    ECR -.->|イメージPull| ECS3

    style ALB fill:#FF9900
    style S3Bucket fill:#FF9900
    style RDS fill:#336791
    style ECR fill:#FF9900
```

---

## セキュリティ設計原則

```mermaid
mindmap
  root((セキュリティ設計))
    多層防御
      認証レイヤー
      認可レイヤー
      データアクセスレイヤー
    最小権限
      ユーザー分離
      所有者チェック
      リソースレベル認可
    デフォルトでセキュア
      全保護ルートで認証必須
      CSRF保護有効
      BCryptパスワード
    情報隠蔽
      S3バケット名非公開
      エラーメッセージ抑制
      404で情報漏洩防止
    安全な失敗
      例外の適切な処理
      トランザクションロールバック
      ログ記録
```

- **多層防御**: 複数の認可チェックポイント
- **最小権限**: ユーザーは自分のデータのみアクセス可能
- **デフォルトでセキュア**: すべての保護ルートで認証必須
- **情報隠蔽**: S3インフラ情報を外部に露出しない
- **安全な失敗**: 404を返す（403ではなく）、スタックトレースを隠蔽

---

## 技術スタック

| カテゴリ | 技術 |
|---------|------|
| 言語 | Java 25 |
| フレームワーク | Spring Boot 3.5.7 |
| ビルドツール | Gradle (Kotlin DSL) |
| データベース | PostgreSQL 17 |
| ORM | Spring Data JPA / Hibernate |
| セキュリティ | Spring Security |
| テンプレート | Thymeleaf |
| クラウド | Spring Cloud AWS 3.4.0 |
| マイグレーション | Flyway |
| テスト | JUnit 5, Testcontainers, MockMvc |
| コンテナ | Docker, Docker Compose |
| 監視 | Spring Boot Actuator |

---

## 今後の拡張可能性

```mermaid
mindmap
  root((拡張アイデア))
    機能拡張
      画像編集機能
      タグ・カテゴリ
      全文検索
      サムネイル生成
      共有機能
      API化
      画像認識
      PDF・動画対応
    インフラ/運用
      ECS/Fargate
      CI/CD
      メトリクス監視
      ログ集約
      自動スケーリング
      マルチアーキテクチャ
    セキュリティ
      OAuth2対応
      MFA
      監査ログ
      暗号化強化
```

---

この概要書は、Handson Applicationの全機能をMermaid図と共に網羅しています。実装の詳細については、`CLAUDE.md`を参照してください。
