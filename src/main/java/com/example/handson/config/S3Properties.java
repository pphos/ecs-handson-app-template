package com.example.handson.config;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.List;

/**
 * S3関連のプロパティ設定
 * application.ymlの"app.s3"プレフィックスに対応
 */
@Configuration
@ConfigurationProperties(prefix = "app.s3")
@Validated
@Getter
@Setter
public class S3Properties {

    /**
     * S3バケット名
     * 環境変数AWS_S3_BUCKET_NAMEで上書き可能
     */
    @NotBlank(message = "S3バケット名は必須です")
    private String bucketName;

    /**
     * 最大ファイルサイズ（バイト）
     * デフォルト: 10MB
     */
    @Min(value = 1024, message = "最小ファイルサイズは1KB（1024バイト）以上です")
    @Max(value = 52428800, message = "最大ファイルサイズは50MB（52428800バイト）以下です")
    private long maxFileSize = 10485760; // 10MB

    /**
     * 許可する画像ファイル拡張子
     * デフォルト: jpg, jpeg, png, gif
     */
    @NotEmpty(message = "許可する拡張子を最低1つ指定してください")
    private List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif");

    /**
     * ブラウザキャッシュの有効期間（秒）
     * デフォルト: 3600秒（1時間）
     */
    @Min(value = 60, message = "キャッシュ時間は最小60秒です")
    private int cacheMaxAge = 3600;
}
