package com.example.handson.domain.imagememo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 画像メモリポジトリ
 */
@Repository
public interface ImageMemoRepository extends JpaRepository<ImageMemo, Long> {

    /**
     * ユーザーIDで画像メモ一覧を作成日時の降順で取得
     */
    List<ImageMemo> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * ユーザーIDで画像メモ一覧をページング付きで取得
     * N+1クエリ対策として、JOIN FETCHでUserを一緒に取得
     */
    @Query("SELECT DISTINCT im FROM ImageMemo im JOIN FETCH im.user WHERE im.user.id = :userId ORDER BY im.createdAt DESC")
    Page<ImageMemo> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * IDとユーザーIDで画像メモを取得
     */
    Optional<ImageMemo> findByIdAndUserId(Long id, Long userId);
}
