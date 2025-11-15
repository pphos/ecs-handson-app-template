FROM flyway/flyway:11.16.0

# マイグレーションSQLファイルをイメージに含める
COPY sql /flyway/sql

# 作業ディレクトリを設定
WORKDIR /flyway

# デフォルト環境変数（イメージにSQLが含まれているため固定パス）
ENV FLYWAY_LOCATIONS=filesystem:/flyway/sql

CMD ["migrate"]
