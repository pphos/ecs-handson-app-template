package com.example.handson.service;

import java.io.InputStream;

/**
 * S3から画像をダウンロードした結果を保持するレコード
 *
 * @param inputStream 画像データのInputStream
 * @param contentType Content-Type（例: image/jpeg）
 * @param contentLength ファイルサイズ（バイト）
 */
public record ImageDownloadResult(
    InputStream inputStream,
    String contentType,
    long contentLength
) {
}
