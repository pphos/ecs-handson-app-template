# 変更履歴・設計判断

このドキュメントは、Handson Applicationの主要な技術的判断、コード品質改善の履歴、設計変更の記録を保持します。

## 目次

- [コード品質改善の履歴](#コード品質改善の履歴)
- [設計判断の記録](#設計判断の記録)
- [今後の拡張アイデア](#今後の拡張アイデア)

## コード品質改善の履歴

### 2025-10-28: アプリケーションProxyパターンによる画像配信の実装

#### セキュリティとアーキテクチャの改善

- **アプリケーションProxy方式の採用**: S3バケット名・インフラ情報を完全に隠蔽
- **ブロックパブリックアクセス対応**: S3をプライベートに保ったまま画像配信が可能
- **影響範囲**: ImageMemo、S3Service、ImageMemoController、Thymeleafテンプレート
- **メリット**: セキュリティ強化、RESTful設計、きめ細かいアクセス制御

#### データモデルの整理

- **image_urlカラムの削除**: 画像URLは`/memos/{id}/image`として動的生成
- **影響範囲**: ImageMemoエンティティ、データベースマイグレーション（V3）
- **メリット**: データの冗長性排除、Single Source of Truth（s3Keyのみ）

#### 画像配信の実装

- **新規エンドポイント**: `GET /memos/{id}/image`を追加
- **ストリーミング配信**: InputStreamResourceによるメモリ効率的な配信
- **キャッシュ戦略**: Cache-Controlヘッダーでブラウザキャッシュ（1時間）
- **影響範囲**: ImageMemoController、S3Service、Thymeleafテンプレート

#### テストの拡充

- **新規テスト**: ImageMemoControllerTest（認証・認可・エラーハンドリング）
- **更新テスト**: S3ServiceTest（downloadImageStream）、ImageMemoRepositoryTest

#### S3パスのユーザー分離

- **パス形式変更**: `uploads/{UUID}.{拡張子}` → `uploads/{userId}/{UUID}.{拡張子}`
- **影響範囲**: S3Service.generateS3Key()
- **メリット**:
  - ユーザーごとのデータ分離と可視性向上
  - 運用の容易さ（一括削除、容量確認）
  - スケーラビリティ向上（単一プレフィックスへの集中を回避）
  - 将来の拡張機能実装の容易化（ストレージ容量制限、バッチ処理等）
- **セキュリティ**: S3はブロックパブリックアクセス有効、認可はアプリケーション層で制御、UUID併用で推測攻撃防止

詳細: `worklog/20251028_0000_application_proxy_image_delivery.md`

---

### 2025-10-19: コードレビューに基づく包括的な改善

#### パフォーマンス最適化

- **N+1クエリ対策**: `ImageMemoRepository`でJOIN FETCHを使用してUserを一緒に取得 (src/main/java/com/example/handson/domain/imagememo/ImageMemoRepository.java)
- **影響範囲**: ページング時の一覧表示で、ユーザー情報取得のための追加クエリを削減

#### トランザクション管理の改善

- **トランザクション境界の明確化**: クラスレベルからメソッドレベルへ移行
- **影響範囲**: `ImageMemoService`, `UserService`の全メソッド
- **メリット**: 各メソッドのトランザクションスコープが明確になり、保守性向上

#### 例外処理の統一

- **カスタム例外の導入**: `StorageException`でS3操作エラーをラップ (src/main/java/com/example/handson/exception/StorageException.java)
- **影響範囲**: `S3Service`の全メソッド
- **メリット**: チェック例外（IOException等）を適切にラップし、エラーの原因を明確化

#### セキュリティ強化（当初の設計、後に見直し）

- **S3キーの推測困難化**: UUID/ランダム文字列を使用
- **影響範囲**: `S3Service.uploadFile()` (src/main/java/com/example/handson/service/S3Service.java)
- **注**: 当初はユーザーIDを除外する設計だったが、2025-10-28にマルチユーザーアプリケーションのベストプラクティスに従いユーザーIDでパス分離する設計に変更

#### null安全性の向上

- **null返却の廃止**: `UserService.findByUsername()`でnullの代わりに例外をスロー (src/main/java/com/example/handson/service/UserService.java)
- **影響範囲**: `UserService.findByUsername()`
- **メリット**: NullPointerExceptionのリスクを削減し、エラーメッセージを明確化

#### バリデーション強化

- **DTOレベルのバリデーション追加**: `ImageMemoCreateDto`の画像フィールドに`@NotNull`を追加 (src/main/java/com/example/handson/dto/ImageMemoCreateDto.java)
- **影響範囲**: 画像メモ作成フォーム
- **メリット**: 早期バリデーションでビジネスロジック層の負担を軽減

#### 設定の明示化

- **画像設定の追加**: `application.yml`にmax-size、allowed-extensionsを明示 (src/main/resources/application.yml:28)
- **影響範囲**: アプリケーション全体の設定
- **メリット**: デフォルト値が設定ファイルで確認可能になり、保守性向上

詳細: `worklog/20251019_*_*.md`

---

### 2025-11-07: 環境変数のセキュリティ強化

#### 必須環境変数のデフォルト値削除

- **変更内容**: `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_MIGRATION_PASSWORD`, `AWS_S3_BUCKET_NAME`のデフォルト値を削除
- **影響範囲**: `application.yml`の環境変数設定
- **メリット**:
  - DB認証情報の明示的な設定を強制
  - デフォルトパスワードが本番環境に流出することを防止
  - 環境変数未設定時に即座にエラーで起動失敗し、誤った接続先への接続を事前に防止

詳細: `worklog/20251107_0000_mandatory_environment_variables.md`

## 設計判断の記録

### マイグレーション分離アーキテクチャの採用

**日付**: プロジェクト初期

**背景**:
- Flywayマイグレーションをアプリケーションと同一イメージに含めると、本番イメージが肥大化
- アプリケーション起動時のマイグレーション自動実行は、スケールアウト時に問題が発生
- 本番環境ではマイグレーションの実行タイミングを明示的に制御したい

**判断**:
- **Flyway用カスタムDockerイメージを別途作成**
- マイグレーションSQLをFlywayイメージに焼き込む
- ローカル開発では`make migrate`で明示的に実行
- ECS/Fargateでは別タスクとして事前実行

**メリット**:
- 本番イメージの軽量化（約10-20MB削減）
- マイグレーションタイミングの明示的制御
- スケールアウト対応（複数appコンテナでもマイグレーションは1回のみ）
- ロールバック安全性

**トレードオフ**:
- ローカル開発で`make migrate`を明示的に実行する手間
- CI/CDパイプラインでFlywayタスク実行を組み込む必要

**参考資料**: `docs/DEPLOYMENT.md#マイグレーション分離アーキテクチャ`

---

### アプリケーションProxy方式 vs Pre-signed URL

**日付**: 2025-10-28

**背景**:
- 画像配信方式として、**Pre-signed URL**方式と**アプリケーションProxy**方式の2つの選択肢
- Pre-signed URLは署名付きURLを生成してS3へ直接アクセス
- アプリケーションProxyはアプリケーション経由で画像を配信

**判断**: **アプリケーションProxy方式を採用**

**比較**:

| 方式 | メリット | デメリット |
|------|---------|----------|
| Pre-signed URL | アプリケーション負荷低、高速 | URL有効期限管理必要、S3構成が露出、認可制御が粗い |
| Application Proxy | セキュリティ高、認可制御容易、S3バケット名隠蔽 | アプリケーション負荷あり |

**採用理由**:
- **セキュリティ最優先**: S3バケット名・インフラ情報を完全に隠蔽
- **きめ細かいアクセス制御**: 所有者チェックをアプリケーション層で実施
- **ブロックパブリックアクセス対応**: S3をプライベートに保つ
- **ブラウザキャッシュで負荷軽減**: Cache-Controlヘッダーで性能問題を緩和

**トレードオフ**:
- アプリケーションの負荷が増加（ただしキャッシュで緩和）
- ストリーミング配信でメモリ効率を維持

**参考資料**: `docs/ARCHITECTURE.md#画像配信方式`

---

### DB権限分離（handson_migration / handson_app）

**日付**: プロジェクト初期

**背景**:
- **最小権限の原則**に基づき、アプリケーションがDDL操作（CREATE, DROP, ALTER）を実行できないようにしたい
- SQLインジェクション攻撃時にDROP TABLEを防止したい

**判断**:
- **マイグレーション用ユーザー**（`handson_migration`）: DDL権限のみ
- **アプリケーション用ユーザー**（`handson_app`）: DML権限のみ（SELECT, INSERT, UPDATE, DELETE）

**メリット**:
- **事故防止**: アプリケーションから誤ってテーブルを削除・変更できない
- **SQLインジェクション対策**: DROP TABLEなどのDDL攻撃を防止
- **監査対応**: マイグレーションとアプリケーション操作のログ分離が可能
- **本番環境安全性**: 実行中アプリがスキーマを変更できない

**実装**:
- ローカル開発: `scripts/init-postgres.sh`で自動設定
- 本番環境: TerraformまたはAWS Secrets Managerで設定

**参考資料**: `docs/ARCHITECTURE.md#ユーザー権限分離`

---

### レイヤードJAR方式の採用

**日付**: プロジェクト初期

**背景**:
- Dockerイメージのビルド・プッシュ時間を短縮したい
- 依存関係の変更は少なく、アプリケーションコードの変更は頻繁

**判断**: **Spring Bootのレイヤード JAR機能を活用**

**レイヤー構成**:
1. `dependencies`: サードパーティライブラリ（変更頻度: 低）
2. `spring-boot-loader`: Spring Bootローダー（変更頻度: 低）
3. `snapshot-dependencies`: スナップショット依存関係（変更頻度: 中）
4. `application`: アプリケーションコード（変更頻度: 高）

**メリット**:
- ソースコード変更時、`application`レイヤーのみ再ビルド
- Dockerビルド時間短縮（2回目以降）
- ECRへのプッシュ時間短縮（変更レイヤーのみ）
- イメージのダウンロード時間短縮

**参考資料**: `docs/DEPLOYMENT.md#レイヤードJAR方式`

## 今後の拡張アイデア

### 機能拡張

- [ ] 画像メモの編集機能
- [ ] タグ・カテゴリ機能
- [ ] 全文検索（タイトル・説明）
- [ ] 画像サムネイル生成
- [ ] 共有機能（他ユーザーとの共有）
- [ ] API化（REST API / GraphQL）
- [ ] 画像認識（AWS Rekognition連携）
- [ ] PDF・動画対応
- [ ] 画像ファイルサイズ・拡張子のバリデーション実装（現在は設定のみ）

### インフラ/運用

- [x] Dockerコンテナ化（完了）
- [x] ヘルスチェックエンドポイント（完了）
- [ ] ECS/Fargateへのデプロイ
- [ ] CI/CDパイプライン構築（GitHub Actions）
- [ ] メトリクス監視（Prometheus/Grafana）
- [ ] ログ集約（CloudWatch Logs/Loki）
- [ ] 自動スケーリング設定
- [ ] マルチアーキテクチャ対応（ARM64/AMD64）

### セキュリティ

- [ ] AWS WAF導入
- [ ] Secrets Managerによる認証情報管理
- [ ] VPC PrivateLink経由でのS3アクセス
- [ ] セキュリティスキャン自動化（Trivy, Snyk）

### パフォーマンス

- [ ] CloudFront CDN導入（画像配信高速化）
- [ ] ElastiCache Redis導入（セッション管理、キャッシュ）
- [ ] RDSリードレプリカ導入（読み取り性能向上）
- [ ] 画像のWebP変換（ファイルサイズ削減）

---

## 参考資料

- [worklog/](../worklog/) - 作業ログの詳細
- [ARCHITECTURE.md](./ARCHITECTURE.md) - アーキテクチャ詳細
- [DEVELOPMENT.md](./DEVELOPMENT.md) - 開発ガイド
