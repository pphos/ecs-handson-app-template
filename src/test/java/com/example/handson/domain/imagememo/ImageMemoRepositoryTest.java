package com.example.handson.domain.imagememo;

import com.example.handson.TestcontainersConfiguration;
import com.example.handson.domain.user.User;
import com.example.handson.domain.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ImageMemoRepositoryのテスト
 * TDD Red: まずテストを書いて失敗を確認
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ImageMemoRepositoryTest {

    @Autowired
    private ImageMemoRepository imageMemoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .username("testuser")
                .password("password")
                .enabled(true)
                .build());
    }

    @Test
    void 画像メモを保存できる() {
        // given
        ImageMemo memo = ImageMemo.builder()
                .user(testUser)
                .title("テスト画像")
                .description("これはテストです")
                .s3Key("test.jpg")
                .build();

        // when
        ImageMemo saved = imageMemoRepository.save(memo);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("テスト画像");
        assertThat(saved.getDescription()).isEqualTo("これはテストです");
        assertThat(saved.getS3Key()).isEqualTo("test.jpg");
        assertThat(saved.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void ユーザーIDで画像メモ一覧を取得できる() {
        // given
        imageMemoRepository.save(ImageMemo.builder()
                .user(testUser)
                .title("画像1")
                .description("説明1")
                .s3Key("1.jpg")
                .build());

        imageMemoRepository.save(ImageMemo.builder()
                .user(testUser)
                .title("画像2")
                .description("説明2")
                .s3Key("2.jpg")
                .build());

        // when
        List<ImageMemo> memos = imageMemoRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());

        // then
        assertThat(memos).hasSize(2);
        assertThat(memos.get(0).getTitle()).isEqualTo("画像2"); // 新しい順
        assertThat(memos.get(1).getTitle()).isEqualTo("画像1");
    }

    @Test
    void ユーザーIDでページング付き画像メモ一覧を取得できる() {
        // given
        for (int i = 1; i <= 15; i++) {
            imageMemoRepository.save(ImageMemo.builder()
                    .user(testUser)
                    .title("画像" + i)
                    .description("説明" + i)
                    .s3Key(i + ".jpg")
                    .build());
        }

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<ImageMemo> page = imageMemoRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId(), pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(15);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(10);
    }

    @Test
    void IDとユーザーIDで画像メモを取得できる() {
        // given
        ImageMemo memo = imageMemoRepository.save(ImageMemo.builder()
                .user(testUser)
                .title("特定の画像")
                .description("特定の説明")
                .s3Key("specific.jpg")
                .build());

        // when
        Optional<ImageMemo> found = imageMemoRepository.findByIdAndUserId(memo.getId(), testUser.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("特定の画像");
    }

    @Test
    void 存在しないIDで検索すると空のOptionalが返る() {
        // when
        Optional<ImageMemo> found = imageMemoRepository.findByIdAndUserId(999L, testUser.getId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void 他のユーザーの画像メモは取得できない() {
        // given
        User otherUser = userRepository.save(User.builder()
                .username("otheruser")
                .password("password")
                .enabled(true)
                .build());

        ImageMemo memo = imageMemoRepository.save(ImageMemo.builder()
                .user(otherUser)
                .title("他人の画像")
                .description("他人の説明")
                .s3Key("other.jpg")
                .build());

        // when
        Optional<ImageMemo> found = imageMemoRepository.findByIdAndUserId(memo.getId(), testUser.getId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void 画像メモを削除できる() {
        // given
        ImageMemo memo = imageMemoRepository.save(ImageMemo.builder()
                .user(testUser)
                .title("削除予定の画像")
                .description("削除予定の説明")
                .s3Key("delete.jpg")
                .build());

        Long memoId = memo.getId();

        // when
        imageMemoRepository.delete(memo);

        // then
        Optional<ImageMemo> found = imageMemoRepository.findById(memoId);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("ページング付き検索でUserをEager fetchすることを確認（N+1クエリ対策）")
    void ページング付き検索でN_1クエリが発生しないことを確認() {
        // given: テストデータ作成
        for (int i = 1; i <= 3; i++) {
            imageMemoRepository.save(ImageMemo.builder()
                    .user(testUser)
                    .title("画像" + i)
                    .description("説明" + i)
                    .s3Key(i + ".jpg")
                    .build());
        }

        entityManager.flush();
        entityManager.clear(); // 永続化コンテキストをクリア

        // when: ページング付きで検索
        Pageable pageable = PageRequest.of(0, 10);
        Page<ImageMemo> result = imageMemoRepository.findByUserIdOrderByCreatedAtDesc(
                testUser.getId(),
                pageable
        );

        entityManager.clear(); // 再度クリアして、Userがロード済みか確認

        // then: Userが既にロードされていることを確認（追加クエリなし）
        // LazyInitializationExceptionが発生しなければ、JOIN FETCHで取得できている
        ImageMemo memo = result.getContent().get(0);
        assertThat(memo.getUser().getUsername()).isEqualTo("testuser");
        assertThat(memo.getUser().getId()).isEqualTo(testUser.getId());
    }
}
