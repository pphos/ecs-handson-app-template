package com.example.handson.service;

import com.example.handson.TestcontainersConfiguration;
import com.example.handson.domain.user.User;
import com.example.handson.domain.user.UserRepository;
import com.example.handson.dto.UserRegistrationDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UserServiceのテスト
 * TDD Red: まずテストを書いて失敗を確認
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void ユーザーを登録できる() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
                "testuser",
                "password123",
                "password123"
        );

        // when
        User user = userService.registerUser(dto);

        // then
        assertThat(user.getId()).isNotNull();
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.isEnabled()).isTrue();
        // パスワードがハッシュ化されていることを確認
        assertThat(user.getPassword()).isNotEqualTo("password123");
        assertThat(user.getPassword()).startsWith("$2a$"); // BCrypt形式
    }

    @Test
    void パスワードが一致しない場合は登録できない() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
                "testuser",
                "password123",
                "password456"
        );

        // when & then
        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("パスワードが一致しません");
    }

    @Test
    void 既に存在するユーザー名では登録できない() {
        // given
        userRepository.save(User.builder()
                .username("existing")
                .password("password")
                .enabled(true)
                .build());

        UserRegistrationDto dto = new UserRegistrationDto(
                "existing",
                "password123",
                "password123"
        );

        // when & then
        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("既に使用されているユーザー名");
    }

    @Test
    void ユーザー名でユーザーを検索できる() {
        // given
        userRepository.save(User.builder()
                .username("searchuser")
                .password("password")
                .enabled(true)
                .build());

        // when
        User found = userService.findByUsername("searchuser");

        // then
        assertThat(found).isNotNull();
        assertThat(found.getUsername()).isEqualTo("searchuser");
    }

    @Test
    void 存在しないユーザー名で検索すると例外がスローされる() {
        // when & then
        assertThatThrownBy(() -> userService.findByUsername("notexist"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ユーザーが見つかりません");
    }
}
