-- ユーザーテーブル
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ユーザー名検索用インデックス（ログイン認証の高速化）
CREATE INDEX idx_users_username ON users(username);

-- 画像メモテーブル
CREATE TABLE image_memos (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    s3_key VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_image_memos_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ユーザーごとの画像メモ検索用インデックス
CREATE INDEX idx_image_memos_user_id ON image_memos(user_id);

-- 作成日時降順ソート用インデックス（一覧表示の高速化）
CREATE INDEX idx_image_memos_created_at ON image_memos(created_at DESC);
