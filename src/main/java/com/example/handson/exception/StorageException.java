package com.example.handson.exception;

/**
 * ストレージ操作に関する例外
 * S3などの外部ストレージとの連携で発生したエラーをラップする
 */
public class StorageException extends RuntimeException {

    /**
     * エラーメッセージと原因例外を指定してStorageExceptionを生成
     *
     * @param message エラーメッセージ
     * @param cause 原因例外
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
