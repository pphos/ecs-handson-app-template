#!/bin/bash

# LocalStackの初期化スクリプト
# S3バケットを自動作成します

set -e

BUCKET_NAME="handson-app-bucket"
MAX_RETRIES=30
RETRY_INTERVAL=2

echo "LocalStackの準備完了を待っています..."

# LocalStackが起動するまで待機
for i in $(seq 1 $MAX_RETRIES); do
    if docker exec localstack awslocal s3 ls > /dev/null 2>&1; then
        echo "LocalStackが起動しました"
        break
    fi

    if [ $i -eq $MAX_RETRIES ]; then
        echo "エラー: LocalStackの起動がタイムアウトしました"
        exit 1
    fi

    echo "待機中... ($i/$MAX_RETRIES)"
    sleep $RETRY_INTERVAL
done

# S3バケットが既に存在するかチェック
if docker exec localstack awslocal s3 ls "s3://$BUCKET_NAME" > /dev/null 2>&1; then
    echo "S3バケット '$BUCKET_NAME' は既に存在します"
else
    echo "S3バケット '$BUCKET_NAME' を作成しています..."
    docker exec localstack awslocal s3 mb "s3://$BUCKET_NAME"
    echo "S3バケット '$BUCKET_NAME' を作成しました"
fi

echo "LocalStackの初期化が完了しました"
