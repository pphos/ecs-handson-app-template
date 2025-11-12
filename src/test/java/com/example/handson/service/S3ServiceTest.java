package com.example.handson.service;

import com.example.handson.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * S3ServiceのテストTDD Red: まずテストを書いて失敗を確認
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class S3ServiceTest {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private S3Client s3Client;

    private static final String TEST_BUCKET = "test-bucket";

    @BeforeEach
    void setUp() {
        // テスト用バケットを作成
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(TEST_BUCKET)
                    .build());
        } catch (Exception e) {
            // バケットが既に存在する場合は無視
        }
    }

    @Test
    void ファイルをS3にアップロードできる() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // when
        String s3Key = s3Service.uploadFile(TEST_BUCKET, file, "user123");

        // then
        assertThat(s3Key).isNotNull();
        assertThat(s3Key).startsWith("uploads/user123/");  // マルチユーザーアプリケーションのベストプラクティス: userIdでパスを分離
        assertThat(s3Key).endsWith(".jpg");

        // S3に実際にファイルが存在することを確認
        var response = s3Client.getObject(GetObjectRequest.builder()
                .bucket(TEST_BUCKET)
                .key(s3Key)
                .build());
        assertThat(response).isNotNull();
    }


    @Test
    void S3からファイルを削除できる() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        String s3Key = s3Service.uploadFile(TEST_BUCKET, file, "user123");

        // when
        s3Service.deleteFile(TEST_BUCKET, s3Key);

        // then
        // ファイルが削除されたことを確認
        var listResponse = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(TEST_BUCKET)
                .prefix(s3Key)
                .build());
        assertThat(listResponse.contents()).isEmpty();
    }

    @Test
    void 許可されていない拡張子のファイルはアップロードできない() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/octet-stream",
                "test content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> s3Service.uploadFile(TEST_BUCKET, file, "user123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("許可されていないファイル形式");
    }

    @Test
    void ファイルサイズが大きすぎる場合はアップロードできない() {
        // given
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                largeContent
        );

        // when & then
        assertThatThrownBy(() -> s3Service.uploadFile(TEST_BUCKET, file, "user123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ファイルサイズ");
    }

    @Test
    void 空のファイルはアップロードできない() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                new byte[0]
        );

        // when & then
        assertThatThrownBy(() -> s3Service.uploadFile(TEST_BUCKET, file, "user123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("空のファイル");
    }

    @Test
    void S3から画像をストリーミングダウンロードできる() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        String s3Key = s3Service.uploadFile(TEST_BUCKET, file, "user123");

        // when
        ImageDownloadResult result = s3Service.downloadImageStream(TEST_BUCKET, s3Key);

        // then
        assertThat(result).isNotNull();
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.contentLength()).isGreaterThan(0);
        assertThat(result.inputStream()).isNotNull();

        // InputStreamから読み込んでデータが存在することを確認
        byte[] data = result.inputStream().readAllBytes();
        assertThat(data).isNotEmpty();
        result.inputStream().close();
    }

    @Test
    void PNG画像の場合Content_Typeが正しく設定される() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "test image content".getBytes()
        );
        String s3Key = s3Service.uploadFile(TEST_BUCKET, file, "user123");

        // when
        ImageDownloadResult result = s3Service.downloadImageStream(TEST_BUCKET, s3Key);

        // then
        assertThat(result.contentType()).isEqualTo("image/png");
        result.inputStream().close();
    }
}
