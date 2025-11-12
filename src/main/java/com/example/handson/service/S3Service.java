package com.example.handson.service;

import com.example.handson.config.S3Properties;
import com.example.handson.exception.StorageException;
import io.awspring.cloud.s3.S3Exception;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * S3サービス
 * 画像ファイルのアップロード・削除・URL取得を担当
 * Spring Cloud AWSのS3Templateを使用
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Template s3Template;
    private final S3Properties s3Properties;

    /**
     * ファイルをS3にアップロード
     *
     * @param bucketName バケット名
     * @param file アップロードするファイル
     * @param userId ユーザーID
     * @return S3キー
     */
    public String uploadFile(String bucketName, MultipartFile file, String userId) throws IOException {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String s3Key = generateS3Key(userId, extension);

        try (InputStream inputStream = file.getInputStream()) {
            // Spring Cloud AWSのS3Templateを使用してアップロード
            s3Template.upload(bucketName, s3Key, inputStream,
                io.awspring.cloud.s3.ObjectMetadata.builder()
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build());

            log.info("ファイルをS3にアップロードしました: bucket={}, key={}", bucketName, s3Key);
            return s3Key;
        } catch (S3Exception e) {
            log.error("S3へのアップロードに失敗しました: bucket={}, key={}", bucketName, s3Key, e);
            throw new StorageException("ファイルのアップロードに失敗しました", e);
        } catch (IOException e) {
            log.error("ファイル読み込みエラー: key={}", s3Key, e);
            throw new StorageException("ファイルの読み込みに失敗しました", e);
        }
    }

    /**
     * S3から画像をダウンロード（ストリーミング配信用）
     *
     * @param bucketName バケット名
     * @param s3Key S3キー
     * @return 画像ダウンロード結果（InputStream、Content-Type、Content-Length）
     */
    public ImageDownloadResult downloadImageStream(String bucketName, String s3Key) {
        try {
            // Spring Cloud AWSのS3Templateを使用してダウンロード
            S3Resource resource = s3Template.download(bucketName, s3Key);

            // Content-Typeを取得（ファイル名から推測）
            String contentType = getContentType(s3Key);

            // Content-Lengthを取得
            long contentLength = resource.contentLength();

            // InputStreamを取得
            InputStream inputStream = resource.getInputStream();

            log.info("S3から画像をダウンロードしました: bucket={}, key={}, contentType={}, size={}",
                bucketName, s3Key, contentType, contentLength);

            return new ImageDownloadResult(inputStream, contentType, contentLength);
        } catch (S3Exception e) {
            log.error("S3からのダウンロードに失敗しました: bucket={}, key={}", bucketName, s3Key, e);
            throw new StorageException("ファイルのダウンロードに失敗しました", e);
        } catch (IOException e) {
            log.error("S3リソースアクセスエラー: bucket={}, key={}", bucketName, s3Key, e);
            throw new StorageException("ファイルのダウンロードに失敗しました", e);
        }
    }

    /**
     * S3キーからContent-Typeを推測
     */
    private String getContentType(String s3Key) {
        String extension = getFileExtension(s3Key).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    /**
     * S3からファイルを削除
     *
     * @param bucketName バケット名
     * @param s3Key S3キー
     */
    public void deleteFile(String bucketName, String s3Key) {
        try {
            // Spring Cloud AWSのS3Templateを使用して削除
            s3Template.deleteObject(bucketName, s3Key);
            log.info("S3からファイルを削除しました: bucket={}, key={}", bucketName, s3Key);
        } catch (S3Exception e) {
            log.error("S3からの削除に失敗しました: bucket={}, key={}", bucketName, s3Key, e);
            throw new StorageException("ファイルの削除に失敗しました", e);
        }
    }

    /**
     * ファイルのバリデーション
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("空のファイルはアップロードできません");
        }

        if (file.getSize() > s3Properties.getMaxFileSize()) {
            throw new IllegalArgumentException(
                    String.format("ファイルサイズが大きすぎます。最大サイズ: %d bytes", s3Properties.getMaxFileSize())
            );
        }

        String extension = getFileExtension(file.getOriginalFilename());
        List<String> allowed = s3Properties.getAllowedExtensions();

        if (!allowed.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("許可されていないファイル形式です。許可されている形式: %s", String.join(", ", allowed))
            );
        }
    }

    /**
     * ファイル拡張子を取得
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("無効なファイル名です");
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * S3キーを生成
     * マルチユーザーアプリケーションのベストプラクティスに従い、
     * ユーザーIDでパスを分離することで運用性・管理性を向上
     *
     * @param userId ユーザーID
     * @param extension ファイル拡張子
     * @return S3キー（形式: uploads/{userId}/{UUID}.{extension}）
     */
    private String generateS3Key(String userId, String extension) {
        String uuid = UUID.randomUUID().toString();
        return String.format("uploads/%s/%s.%s", userId, uuid, extension);
    }
}
