package com.example.handson.domain.user;

import com.example.handson.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepositoryのテスト
 * TDD Red: まずテストを書いて失敗を確認
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void ユーザーを保存できる() {
        // given
        User user = User.builder()
                .username("testuser")
                .password("password123")
                .enabled(true)
                .build();

        // when
        User saved = userRepository.save(user);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("testuser");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void ユーザー名でユーザーを検索できる() {
        // given
        User user = User.builder()
                .username("searchuser")
                .password("password123")
                .enabled(true)
                .build();
        userRepository.save(user);

        // when
        Optional<User> found = userRepository.findByUsername("searchuser");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("searchuser");
    }

    @Test
    void 存在しないユーザー名で検索すると空のOptionalが返る() {
        // when
        Optional<User> found = userRepository.findByUsername("notexist");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void ユーザー名が重複している場合を検証できる() {
        // given
        User user = User.builder()
                .username("duplicate")
                .password("password123")
                .enabled(true)
                .build();
        userRepository.save(user);

        // when
        boolean exists = userRepository.existsByUsername("duplicate");

        // then
        assertThat(exists).isTrue();
    }
}
