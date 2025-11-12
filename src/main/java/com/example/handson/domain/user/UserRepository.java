package com.example.handson.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ユーザーリポジトリ
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * ユーザー名でユーザーを検索
     */
    Optional<User> findByUsername(String username);

    /**
     * ユーザー名が存在するか確認
     */
    boolean existsByUsername(String username);
}
