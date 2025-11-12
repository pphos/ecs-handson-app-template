-- PostgreSQL初期化SQL

-- データベースユーザーの作成
CREATE ROLE handson_migration WITH LOGIN PASSWORD :'MIGRATION_PASSWORD';
CREATE ROLE handson_app WITH LOGIN PASSWORD :'APP_PASSWORD';

GRANT CONNECT ON DATABASE handson TO handson_migration;
GRANT CONNECT ON DATABASE handson TO handson_app;

-- マイグレーションユーザーへの既存オブジェクトへの権限設定
GRANT USAGE, CREATE ON SCHEMA public TO handson_migration;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO handson_migration;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO handson_migration;

-- アプリケーションユーザーへの既存オブジェクトへの権限設定
GRANT USAGE ON SCHEMA public TO handson_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO handson_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO handson_app;

-- マイグレーションユーザーに切り替え
SET ROLE handson_migration;

-- 今後マイグレーションで作成されるテーブルへのデフォルト権限設定
-- マイグレーション実行後に個別に権限設定する手間を省くために実施します
ALTER DEFAULT PRIVILEGES FOR ROLE handson_migration IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO handson_app;

ALTER DEFAULT PRIVILEGES FOR ROLE handson_migration IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO handson_app;

-- マスターユーザーの権限にリセット
RESET ROLE;

-- 設定確認用
SELECT 'ユーザー作成完了:' as status;
SELECT usename, usesuper, usecreatedb FROM pg_user WHERE usename IN ('handson_migration', 'handson_app');
