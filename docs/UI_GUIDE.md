# UIフロントエンド開発ガイド

このドキュメントは、Handson ApplicationのUI/フロントエンド開発に関するガイドです。

## 目次

- [Thymeleafテンプレート構成](#thymeleafテンプレート構成)
- [UIフラグメント化](#uiフラグメント化)
- [Material Icons](#material-icons)
- [CSSカスタマイズ](#cssカスタマイズ)

## Thymeleafテンプレート構成

### ディレクトリ構造

```
src/main/resources/templates/
├── layout/
│   └── base.html              # 共通レイアウトベース
├── fragments/
│   └── common.html            # 再利用可能なUIフラグメント
├── memos/
│   ├── list.html              # 画像メモ一覧
│   ├── create.html            # 画像メモ作成フォーム
│   └── detail.html            # 画像メモ詳細
├── index.html                 # ホームページ
├── login.html                 # ログインページ
└── register.html              # ユーザー登録ページ
```

### レイアウトベース

**layout/base.html**:

共通のHTMLレイアウトを定義し、各ページで再利用します。

```html
<!DOCTYPE html>
<html th:fragment="layout(title, content)" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:replace="${title}">画像メモアプリ</title>
    <link rel="stylesheet" href="/css/style.css">
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
</head>
<body>
    <th:block th:replace="${content}" />
</body>
</html>
```

**使用例**（各ページ）:

```html
<!DOCTYPE html>
<html th:replace="~{layout/base :: layout(~{::title}, ~{::body})}">
<head>
    <title>画像メモ一覧</title>
</head>
<body>
    <h1>画像メモ一覧</h1>
    <!-- ページ固有のコンテンツ -->
</body>
</html>
```

**メリット**:
- 共通のheadタグ（CSS、メタタグ）を一元管理
- ページごとのコード重複を削減
- 変更が容易（base.htmlを変更すれば全ページに反映）

## UIフラグメント化

### fragments/common.html

再利用可能なUIコンポーネントを定義します。

#### アラートメッセージ

**成功メッセージ**:

```html
<div th:fragment="alert-success(message)" th:if="${message}">
    <div class="alert alert-success">
        <p th:text="${message}"></p>
    </div>
</div>
```

**エラーメッセージ**:

```html
<div th:fragment="alert-error(message)" th:if="${message}">
    <div class="alert alert-error">
        <p th:text="${message}"></p>
    </div>
</div>
```

**使用例**:

```html
<!-- 成功メッセージ -->
<div th:replace="~{fragments/common :: alert-success(${successMessage})}"></div>

<!-- エラーメッセージ -->
<div th:replace="~{fragments/common :: alert-error(${errorMessage})}"></div>
```

#### Material Icons付きタイトル

**定義**:

```html
<h1 th:fragment="page-title(icon, color, text)" class="page-title">
    <span class="material-icons" th:style="'color: ' + ${color}">[[${icon}]]</span>
    <span th:text="${text}"></span>
</h1>
```

**使用例**:

```html
<!-- 画像メモ一覧 -->
<h1 th:replace="~{fragments/common :: page-title('image', '#4CAF50', '画像メモ一覧')}"></h1>

<!-- ユーザー登録 -->
<h1 th:replace="~{fragments/common :: page-title('person_add', '#2196F3', 'ユーザー登録')}"></h1>
```

#### Material Icons付きボタン

**定義**:

```html
<button th:fragment="icon-button(icon, text, type, color)"
        th:type="${type}"
        class="btn"
        th:classappend="${'btn-' + color}">
    <span class="material-icons">[[${icon}]]</span>
    <span th:text="${text}"></span>
</button>
```

**使用例**:

```html
<!-- 画像アップロードボタン -->
<button th:replace="~{fragments/common :: icon-button('add_photo_alternate', '画像を追加', 'submit', 'primary')}"></button>

<!-- 削除ボタン -->
<button th:replace="~{fragments/common :: icon-button('delete', '削除', 'submit', 'danger')}"></button>
```

#### Material Icons付きラベル

**定義**:

```html
<label th:fragment="icon-label(icon, text, for)" th:for="${for}">
    <span class="material-icons">[[${icon}]]</span>
    <span th:text="${text}"></span>
</label>
```

**使用例**:

```html
<label th:replace="~{fragments/common :: icon-label('title', 'タイトル', 'title')}"></label>
<input type="text" id="title" name="title" />
```

### フラグメントのメリット

- **コード再利用**: 同じUIコンポーネントを複数ページで使い回し
- **一貫性**: デザインの統一性を保つ
- **保守性**: フラグメントを変更すれば全ページに反映
- **可読性**: ページのHTMLがシンプルになる

## Material Icons

### 概要

Google Material Iconsを使用し、モダンで統一されたアイコンデザインを実現しています。

**CDN読み込み**（layout/base.html）:

```html
<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
```

### 主な使用アイコン

| アイコン名 | 用途 | 使用場所 |
|-----------|------|---------|
| `image` | 画像関連 | 画像メモ一覧タイトル |
| `person_add` | ユーザー登録 | 登録ページタイトル |
| `login` | ログイン | ログインボタン |
| `add_photo_alternate` | 画像アップロード | 画像追加ボタン |
| `delete` | 削除 | 削除ボタン |
| `title` | タイトル | タイトル入力ラベル |
| `description` | 説明 | 説明入力ラベル |
| `home` | ホーム | ホームリンク |
| `logout` | ログアウト | ログアウトボタン |

### 使用方法

**基本的な使い方**:

```html
<span class="material-icons">icon_name</span>
```

**サイズ変更**:

```css
.material-icons {
    font-size: 18px;  /* デフォルト: 24px */
}

.material-icons.md-18 { font-size: 18px; }
.material-icons.md-24 { font-size: 24px; }
.material-icons.md-36 { font-size: 36px; }
.material-icons.md-48 { font-size: 48px; }
```

**色変更**:

```html
<span class="material-icons" style="color: #4CAF50;">image</span>
```

**ボタンと組み合わせ**:

```html
<button class="btn btn-primary">
    <span class="material-icons">add_photo_alternate</span>
    <span>画像を追加</span>
</button>
```

### アイコン一覧

公式アイコン一覧: [Google Material Icons](https://fonts.google.com/icons)

## CSSカスタマイズ

### ファイル構成

```
src/main/resources/static/css/
└── style.css              # メインスタイルシート
```

### スタイルガイド

#### カラーパレット

```css
:root {
    --primary-color: #2196F3;      /* 青 */
    --success-color: #4CAF50;      /* 緑 */
    --danger-color: #f44336;       /* 赤 */
    --warning-color: #ff9800;      /* オレンジ */
    --text-color: #333;            /* テキスト */
    --border-color: #ddd;          /* 境界線 */
    --background-color: #f5f5f5;   /* 背景 */
}
```

#### ボタンスタイル

```css
.btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 10px 20px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    transition: background-color 0.3s;
}

.btn-primary {
    background-color: var(--primary-color);
    color: white;
}

.btn-primary:hover {
    background-color: #1976D2;
}

.btn-danger {
    background-color: var(--danger-color);
    color: white;
}

.btn-danger:hover {
    background-color: #d32f2f;
}
```

#### フォームスタイル

```css
.form-group {
    margin-bottom: 20px;
}

.form-group label {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    font-weight: bold;
    color: var(--text-color);
}

.form-group input[type="text"],
.form-group input[type="password"],
.form-group textarea {
    width: 100%;
    padding: 10px;
    border: 1px solid var(--border-color);
    border-radius: 4px;
    font-size: 14px;
}
```

#### アラートスタイル

```css
.alert {
    padding: 15px;
    border-radius: 4px;
    margin-bottom: 20px;
}

.alert-success {
    background-color: #d4edda;
    color: #155724;
    border: 1px solid #c3e6cb;
}

.alert-error {
    background-color: #f8d7da;
    color: #721c24;
    border: 1px solid #f5c6cb;
}
```

#### ページタイトルスタイル

```css
.page-title {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 30px;
    font-size: 32px;
    color: var(--text-color);
}

.page-title .material-icons {
    font-size: 36px;
}
```

### レスポンシブデザイン

```css
/* モバイル対応 */
@media (max-width: 768px) {
    .page-title {
        font-size: 24px;
    }

    .page-title .material-icons {
        font-size: 28px;
    }

    .btn {
        padding: 8px 16px;
        font-size: 12px;
    }
}
```

### 画像メモ一覧のグリッドレイアウト

```css
.memo-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 20px;
    margin-top: 30px;
}

.memo-card {
    border: 1px solid var(--border-color);
    border-radius: 8px;
    overflow: hidden;
    transition: box-shadow 0.3s;
}

.memo-card:hover {
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.memo-card img {
    width: 100%;
    height: 200px;
    object-fit: cover;
}

.memo-card-body {
    padding: 15px;
}

.memo-card-title {
    font-size: 18px;
    font-weight: bold;
    margin-bottom: 10px;
}

.memo-card-description {
    font-size: 14px;
    color: #666;
    margin-bottom: 15px;
}
```

### アクセシビリティ

- **フォーカス状態**:
  ```css
  button:focus,
  input:focus,
  textarea:focus {
      outline: 2px solid var(--primary-color);
      outline-offset: 2px;
  }
  ```

- **アイコンのaria-label**:
  ```html
  <span class="material-icons" aria-label="削除">delete</span>
  ```

---

## 参考資料

- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [Google Material Icons](https://fonts.google.com/icons)
- [Material Design Guidelines](https://material.io/design)
- [ARCHITECTURE.md](./ARCHITECTURE.md) - アーキテクチャ詳細
