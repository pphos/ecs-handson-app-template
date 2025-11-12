# コードレビューを実施

このコマンドは、新規コードまたは変更コードを以下の観点でレビューします。

## レビュー観点

### 1. アーキテクチャ準拠性

**チェック項目**:
- レイヤー構成に従っているか（Controller → Service → Domain）
- Controllerが直接Repositoryを呼んでいないか
- Serviceに適切にビジネスロジックが集約されているか
- 依存関係の方向が正しいか

**参照ドキュメント**: [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md)

### 2. セキュリティ

**チェック項目**:
- 認証・認可は適切か（Spring Security）
- SQLインジェクション対策は十分か（PreparedStatement使用）
- 入力バリデーションは実装されているか（Bean Validation）
- パスワードはBCryptでハッシュ化されているか
- S3バケット名など機密情報が露出していないか
- CSRF保護は有効か

**セキュリティチェックリスト**:
- [ ] ユーザー入力はすべてバリデーション済み
- [ ] SQLは動的生成ではなくJPQL/Criteria API使用
- [ ] 認証が必要なエンドポイントはSecurityConfigで保護
- [ ] 所有者チェックが実装されている（他ユーザーのデータアクセス防止）
- [ ] パスワードは平文で保存していない

**参照ドキュメント**: [docs/ARCHITECTURE.md#セキュリティ設計](../../docs/ARCHITECTURE.md#セキュリティ設計)

### 3. パフォーマンス

**チェック項目**:
- N+1クエリは発生していないか（JOIN FETCH使用）
- トランザクション境界は適切か（メソッドレベルで定義）
- 不要なEagerロードはないか
- ページングは実装されているか（大量データ対応）
- インデックスは適切か

**N+1クエリ対策**:
```java
// 良い例: JOIN FETCHでUserを一緒に取得
@Query("SELECT m FROM ImageMemo m JOIN FETCH m.user WHERE m.user.id = :userId")
Page<ImageMemo> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

// 悪い例: LazyロードでN+1発生
List<ImageMemo> memos = repository.findByUserId(userId);
for (ImageMemo memo : memos) {
    String username = memo.getUser().getUsername(); // N+1発生
}
```

**トランザクション境界**:
```java
// 良い例: メソッドレベルで明示的に定義
@Transactional
public User registerUser(String username, String password) {
    // 更新処理
}

@Transactional(readOnly = true)
public User findByUsername(String username) {
    // 参照処理
}

// 悪い例: クラスレベルで定義（境界が不明確）
@Transactional
public class UserService {
    // ...
}
```

**参照ドキュメント**: [docs/ARCHITECTURE.md#データベース設計](../../docs/ARCHITECTURE.md#データベース設計)

### 4. テストカバレッジ

**チェック項目**:
- TDDアプローチに従っているか
- リポジトリテストは実装されているか（Testcontainers使用）
- サービステストは実装されているか（モック使用）
- エッジケースをカバーしているか
- 例外処理のテストがあるか

**TDD順序**:
1. Red: テストを先に書いて失敗させる
2. Green: 最小限の実装でテストをパス
3. Refactor: コードを洗練させる

**推奨テストカバレッジ**:
- リポジトリ層: 100%（Testcontainers使用）
- サービス層: 90%以上（モック使用）
- コントローラー層: 主要パスのみ（MockMvc使用）

**参照ドキュメント**: [docs/DEVELOPMENT.md#テスト戦略](../../docs/DEVELOPMENT.md#テスト戦略)

### 5. コーディング規約

**チェック項目**:
- Lombokアノテーションは適切か
- トランザクションはメソッドレベルで定義されているか
- ログは適切に出力されているか（@Slf4j使用）
- null安全性は確保されているか（Optional使用）
- 例外処理は適切か（カスタム例外 or IllegalArgumentException）

**Lombokアノテーション**:
- Entity: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor`, `@Builder`
- Service/Controller: `@RequiredArgsConstructor`, `@Slf4j`

**null安全性**:
```java
// 良い例: nullを返さず例外をスロー
return userRepository.findByUsername(username)
    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

// 悪い例: nullを返す（NullPointerExceptionリスク）
return userRepository.findByUsername(username).orElse(null);
```

**バリデーション**:
- DTOレベル: Bean Validation（`@NotBlank`, `@Size`, `@NotNull`）
- サービスレベル: ビジネスルール検証、`IllegalArgumentException`スロー

**参照ドキュメント**: [docs/DEVELOPMENT.md#コーディング規約](../../docs/DEVELOPMENT.md#コーディング規約)

## レビュー実行手順

1. **変更差分確認**:
   ```bash
   git diff main
   ```

2. **静的解析実行**（任意）:
   ```bash
   ./gradlew check
   ```

3. **テスト実行**:
   ```bash
   ./gradlew test
   ```

4. **手動レビュー**:
   - 上記のチェック項目を確認
   - コメントで指摘事項を記録

5. **修正後の再確認**:
   ```bash
   git diff main
   ./gradlew test
   ```

## レビューチェックリスト

コピーして使用してください:

```markdown
## アーキテクチャ準拠性
- [ ] レイヤー構成に従っている
- [ ] 責務分離は適切
- [ ] 依存関係の方向が正しい

## セキュリティ
- [ ] 認証・認可は適切
- [ ] 入力バリデーション実装済み
- [ ] SQLインジェクション対策済み
- [ ] パスワードハッシュ化済み
- [ ] 所有者チェック実装済み

## パフォーマンス
- [ ] N+1クエリ対策済み
- [ ] トランザクション境界が適切
- [ ] ページング実装済み（必要な場合）

## テストカバレッジ
- [ ] リポジトリテスト実装済み
- [ ] サービステスト実装済み
- [ ] エッジケースをカバー
- [ ] 例外処理のテストあり

## コーディング規約
- [ ] Lombokアノテーション適切
- [ ] トランザクションがメソッドレベル
- [ ] ログ出力適切
- [ ] null安全性確保
- [ ] 例外処理適切
```

## よくある指摘事項

### アーキテクチャ違反

**問題**: ControllerがRepositoryを直接呼んでいる

```java
// 悪い例
@RestController
public class UserController {
    private final UserRepository userRepository; // NG

    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
}

// 良い例
@RestController
public class UserController {
    private final UserService userService; // OK

    public User getUser(Long id) {
        return userService.findById(id);
    }
}
```

### N+1クエリ

**問題**: LazyロードでN+1クエリが発生

**修正**: JOIN FETCHを使用

### トランザクション境界の不明確さ

**問題**: クラスレベルでトランザクション定義

**修正**: メソッドレベルで明示的に定義

### null安全性

**問題**: nullを返すメソッド

**修正**: `Optional`または例外をスロー

## 詳細ドキュメント

- [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md)
- [docs/DEVELOPMENT.md](../../docs/DEVELOPMENT.md)
- [docs/HISTORY.md - 設計判断の記録](../../docs/HISTORY.md#設計判断の記録)
