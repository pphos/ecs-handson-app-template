package com.example.handson.service;

import com.example.handson.TestcontainersConfiguration;
import com.example.handson.domain.imagememo.ImageMemo;
import com.example.handson.domain.imagememo.ImageMemoRepository;
import com.example.handson.domain.user.User;
import com.example.handson.domain.user.UserRepository;
import com.example.handson.dto.ImageMemoCreateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * ImageMemoServiceの統合テスト
 * Testcontainers（PostgreSQL + LocalStack S3）を使用して実環境に近い状態でテスト
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ImageMemoServiceTest {

    @Autowired
    private ImageMemoService imageMemoService;

    @Autowired
    private ImageMemoRepository imageMemoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Client s3Client;

    @Value("${app.s3.bucket-name:handson-app-bucket}")
    private String bucketName;

    private User testUser;

    @BeforeEach
    void setUp() {
        // テスト用バケットを作成
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            // バケットが既に存在する場合は無視
        }

        // テスト用ユーザーを作成（一意のユーザー名を生成）
        String uniqueUsername = "testuser-" + UUID.randomUUID().toString().substring(0, 8);
        testUser = userRepository.save(User.builder()
                .username(uniqueUsername)
                .password("password")
                .enabled(true)
                .build());
    }

    @Test
    void 画像メモを作成できる() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        ImageMemoCreateDto dto = new ImageMemoCreateDto("テストタイトル", "テスト説明", file);

        // when
        ImageMemo memo = imageMemoService.createImageMemo(dto, testUser);

        // then
        assertThat(memo.getId()).isNotNull();
        assertThat(memo.getTitle()).isEqualTo("テストタイトル");
        assertThat(memo.getDescription()).isEqualTo("テスト説明");
        assertThat(memo.getS3Key()).startsWith("uploads/" + testUser.getId() + "/");
        assertThat(memo.getS3Key()).endsWith(".jpg");
        assertThat(memo.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(memo.getCreatedAt()).isNotNull();

        // S3に実際にファイルが存在することを確認
        var response = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(memo.getS3Key())
                .build());
        assertThat(response).isNotNull();
    }

    @Test
    void 画像ファイルがnullの場合は例外がスローされる() {
        // given
        ImageMemoCreateDto dto = new ImageMemoCreateDto("タイトル", "説明", null);

        // when & then
        assertThatThrownBy(() -> imageMemoService.createImageMemo(dto, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("画像ファイルは必須です");
    }

    @Test
    void 画像ファイルが空の場合は例外がスローされる() {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                new byte[0]
        );
        ImageMemoCreateDto dto = new ImageMemoCreateDto("タイトル", "説明", emptyFile);

        // when & then
        // 注: MultipartFile.isEmpty()はサイズが0の場合にtrueを返すため、
        // ImageMemoServiceでnullチェックと同じメッセージがスローされる
        assertThatThrownBy(() -> imageMemoService.createImageMemo(dto, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("画像ファイルは必須です");
    }

    @Test
    void ユーザーIDでページング付き一覧を取得できる() throws IOException {
        // given: 3件の画像メモを作成
        for (int i = 1; i <= 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test" + i + ".jpg",
                    "image/jpeg",
                    ("test content " + i).getBytes()
            );
            ImageMemoCreateDto dto = new ImageMemoCreateDto("タイトル" + i, "説明" + i, file);
            imageMemoService.createImageMemo(dto, testUser);
        }

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<ImageMemo> page = imageMemoService.findByUserId(testUser.getId(), pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(3);
        // 作成日時の降順で取得されることを確認
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("タイトル3");
    }

    @Test
    void IDとユーザーIDで画像メモを取得できる() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
        );
        ImageMemoCreateDto dto = new ImageMemoCreateDto("タイトル", "説明", file);
        ImageMemo created = imageMemoService.createImageMemo(dto, testUser);

        // when
        ImageMemo found = imageMemoService.findByIdAndUserId(created.getId(), testUser.getId());

        // then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getTitle()).isEqualTo("タイトル");
    }

    @Test
    void 他ユーザーの画像メモは取得できない() throws IOException {
        // given: 別ユーザーを作成
        String otherUsername = "otheruser-" + UUID.randomUUID().toString().substring(0, 8);
        User otherUser = userRepository.save(User.builder()
                .username(otherUsername)
                .password("password")
                .enabled(true)
                .build());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
        );
        ImageMemoCreateDto dto = new ImageMemoCreateDto("他人のメモ", "説明", file);
        ImageMemo otherMemo = imageMemoService.createImageMemo(dto, otherUser);

        // when
        ImageMemo found = imageMemoService.findByIdAndUserId(otherMemo.getId(), testUser.getId());

        // then
        assertThat(found).isNull();
    }

    @Test
    void 画像メモを削除できる() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
        );
        ImageMemoCreateDto dto = new ImageMemoCreateDto("削除対象", "説明", file);
        ImageMemo memo = imageMemoService.createImageMemo(dto, testUser);
        String s3Key = memo.getS3Key();

        // when
        imageMemoService.deleteImageMemo(memo.getId(), testUser.getId());

        // then: DBから削除されていることを確認
        ImageMemo deleted = imageMemoService.findByIdAndUserId(memo.getId(), testUser.getId());
        assertThat(deleted).isNull();

        // S3からも削除されていることを確認
        var listResponse = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(s3Key)
                .build());
        assertThat(listResponse.contents()).isEmpty();
    }

    @Test
    void 存在しない画像メモの削除は例外がスローされる() {
        // when & then
        assertThatThrownBy(() -> imageMemoService.deleteImageMemo(999L, testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("画像メモが見つかりません");
    }

    @Test
    void 他ユーザーの画像メモは削除できない() throws IOException {
        // given: 別ユーザーを作成
        String otherUsername = "otheruser2-" + UUID.randomUUID().toString().substring(0, 8);
        User otherUser = userRepository.save(User.builder()
                .username(otherUsername)
                .password("password")
                .enabled(true)
                .build());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
        );
        ImageMemoCreateDto dto = new ImageMemoCreateDto("他人のメモ", "説明", file);
        ImageMemo otherMemo = imageMemoService.createImageMemo(dto, otherUser);

        // when & then
        assertThatThrownBy(() -> imageMemoService.deleteImageMemo(otherMemo.getId(), testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("画像メモが見つかりません");
    }
}
