package com.example.handson.infrastructure;

import com.example.handson.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LocalStack S3接続テスト
 * TDDアプローチ: S3への接続とバケット操作ができることを確認
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class S3ConnectionTest {

    private static final String TEST_BUCKET = "test-image-memo-bucket";

    @Autowired
    private S3Client s3Client;

    @Test
    void testS3Connection() {
        // S3に接続できることを確認
        ListBucketsResponse response = s3Client.listBuckets();
        assertThat(response).isNotNull();
    }

    @Test
    void testCreateBucket() {
        // バケットを作成できることを確認
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket(TEST_BUCKET)
                .build();

        s3Client.createBucket(createBucketRequest);

        // バケットが作成されたことを確認
        ListBucketsResponse response = s3Client.listBuckets();
        assertThat(response.buckets()).extracting(Bucket::name)
                .contains(TEST_BUCKET);
    }

    @Test
    void testUploadAndDownloadFile() throws IOException {
        // バケット作成
        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(TEST_BUCKET)
                .build());

        // ファイルアップロード
        String key = "test-image.txt";
        String content = "Hello from S3 test!";

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(TEST_BUCKET)
                        .key(key)
                        .build(),
                RequestBody.fromString(content)
        );

        // ファイルダウンロード
        var response = s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(TEST_BUCKET)
                        .key(key)
                        .build()
        );

        String downloadedContent = new String(response.readAllBytes());
        assertThat(downloadedContent).isEqualTo(content);
    }

    @Test
    void testDeleteFile() {
        // バケット作成
        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(TEST_BUCKET)
                .build());

        // ファイルアップロード
        String key = "test-delete.txt";
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(TEST_BUCKET)
                        .key(key)
                        .build(),
                RequestBody.fromString("To be deleted")
        );

        // ファイル削除
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(TEST_BUCKET)
                .key(key)
                .build());

        // ファイルが削除されたことを確認
        var listResponse = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(TEST_BUCKET)
                .build());

        assertThat(listResponse.contents()).extracting(S3Object::key)
                .doesNotContain(key);
    }
}
