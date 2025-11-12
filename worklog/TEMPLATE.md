# [作業内容の簡潔なタイトル]

**日付**: YYYY-MM-DD
**担当**: [担当者名またはClaude Code]
**カテゴリ**: [新機能追加 | リファクタリング | バグ修正 | パフォーマンス改善 | セキュリティ対応 | ドキュメント更新]

## 背景・目的

### なぜこの作業が必要だったのか

（ここに背景を記載）

### 達成したい目標

（ここに目標を記載）

## 実施内容

### Before（変更前）

（変更前の状態を具体的に記載）

```java
// 変更前のコード例（該当する場合）
```

### After（変更後）

（変更後の状態を具体的に記載）

```java
// 変更後のコード例（該当する場合）
```

### 具体的な変更内容

1. **変更箇所1**:
   - ファイル: `src/main/java/com/example/...`
   - 内容: ...

2. **変更箇所2**:
   - ファイル: `src/main/resources/...`
   - 内容: ...

## 変更の影響範囲

### 影響を受けるコンポーネント

- [ ] Controller層
- [ ] Service層
- [ ] Repository層
- [ ] Entity
- [ ] Configuration
- [ ] テンプレート（Thymeleaf）
- [ ] データベーススキーマ（マイグレーション）
- [ ] その他: ...

### 影響を受けるファイル一覧

```
src/main/java/com/example/imagememo/
├── controller/
│   └── XxxController.java (変更)
├── service/
│   └── XxxService.java (変更)
└── ...
```

## 確認事項

### 実施した確認

- [ ] ローカル環境でテスト実行（`./gradlew test`）
- [ ] ローカル環境で動作確認（`make start-local`）
- [ ] コードレビュー実施（`/review`）
- [ ] ドキュメント更新（CLAUDE.md等）
- [ ] worklogへの記録

### テスト結果

```bash
# テスト実行コマンド
./gradlew test

# 結果
All tests passed: XXX tests
```

## 期待される効果

### パフォーマンス

（該当する場合）

- クエリ実行時間: X秒 → Y秒（Z%改善）
- メモリ使用量: XMB → YMB（Z%削減）

### セキュリティ

（該当する場合）

- 脆弱性対策: ...
- 権限制御強化: ...

### 保守性

（該当する場合）

- コードの可読性向上
- テストカバレッジ向上: X% → Y%

## 参考情報

### 関連ドキュメント

- [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)
- [docs/DEVELOPMENT.md](../docs/DEVELOPMENT.md)

### 関連Issue/PR

- Issue #XXX
- PR #XXX

### 参考URL

- https://...

## 備考

（追加の補足情報があれば記載）

---

**チェックリスト**:
- [ ] 作業内容が明確に記載されている
- [ ] Before/Afterが具体的
- [ ] 影響範囲が明確
- [ ] 確認事項が実施済み
- [ ] ファイル名が命名規則に従っている（YYYYMMDD_NNNN_description.md）
