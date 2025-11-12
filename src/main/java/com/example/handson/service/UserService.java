package com.example.handson.service;

import com.example.handson.domain.user.User;
import com.example.handson.domain.user.UserRepository;
import com.example.handson.dto.UserRegistrationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ユーザーサービス
 * ユーザーの登録・検索を担当
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * ユーザー登録
     *
     * @param dto ユーザー登録DTO
     * @return 登録されたユーザー
     */
    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        // パスワード確認
        if (!dto.password().equals(dto.confirmPassword())) {
            throw new IllegalArgumentException("パスワードが一致しません");
        }

        // ユーザー名の重複チェック
        if (userRepository.existsByUsername(dto.username())) {
            throw new IllegalArgumentException("既に使用されているユーザー名です");
        }

        // ユーザー作成
        User user = User.builder()
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        log.info("新規ユーザーを登録しました: username={}", saved.getUsername());

        return saved;
    }

    /**
     * ユーザー名でユーザーを検索
     *
     * @param username ユーザー名
     * @return ユーザー
     * @throws IllegalArgumentException ユーザーが見つからない場合
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "ユーザーが見つかりません: " + username));
    }
}
