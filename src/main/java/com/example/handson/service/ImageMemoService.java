package com.example.handson.service;

import com.example.handson.config.S3Properties;
import com.example.handson.domain.imagememo.ImageMemo;
import com.example.handson.domain.imagememo.ImageMemoRepository;
import com.example.handson.domain.user.User;
import com.example.handson.dto.ImageMemoCreateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * 画像メモサービス
 * 画像メモの作成・取得・削除を担当
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageMemoService {

    private final ImageMemoRepository imageMemoRepository;
    private final S3Service s3Service;
    private final S3Properties s3Properties;

    /**
     * 画像メモを作成
     *
     * @param dto 画像メモ作成DTO
     * @param user ログイン中のユーザー
     * @return 作成された画像メモ
     */
    @Transactional
    public ImageMemo createImageMemo(ImageMemoCreateDto dto, User user) throws IOException {
        if (dto.image() == null || dto.image().isEmpty()) {
            throw new IllegalArgumentException("画像ファイルは必須です");
        }

        // S3にアップロード
        String s3Key = s3Service.uploadFile(s3Properties.getBucketName(), dto.image(), String.valueOf(user.getId()));

        // 画像メモを保存
        // 画像URLはアプリケーション経由で配信するため、DBには保存しない
        ImageMemo imageMemo = ImageMemo.builder()
                .user(user)
                .title(dto.title())
                .description(dto.description())
                .s3Key(s3Key)
                .build();

        ImageMemo saved = imageMemoRepository.save(imageMemo);
        log.info("画像メモを作成しました: id={}, userId={}, title={}", saved.getId(), user.getId(), saved.getTitle());

        return saved;
    }

    /**
     * ユーザーの画像メモ一覧をページング付きで取得
     *
     * @param userId ユーザーID
     * @param pageable ページング情報
     * @return 画像メモページ
     */
    @Transactional(readOnly = true)
    public Page<ImageMemo> findByUserId(Long userId, Pageable pageable) {
        return imageMemoRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 画像メモを取得
     *
     * @param id 画像メモID
     * @param userId ユーザーID
     * @return 画像メモ（見つからない場合はnull）
     */
    @Transactional(readOnly = true)
    public ImageMemo findByIdAndUserId(Long id, Long userId) {
        return imageMemoRepository.findByIdAndUserId(id, userId).orElse(null);
    }

    /**
     * 画像メモを削除
     * S3削除を先に実行し、成功後にDB削除を行う
     *
     * @param id 画像メモID
     * @param userId ユーザーID
     */
    @Transactional
    public void deleteImageMemo(Long id, Long userId) {
        ImageMemo imageMemo = imageMemoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("画像メモが見つかりません"));

        String s3Key = imageMemo.getS3Key();

        // S3から画像を削除（失敗時は例外をスローしてDB削除を中止）
        s3Service.deleteFile(s3Properties.getBucketName(), s3Key);
        log.info("S3から画像を削除しました: s3Key={}", s3Key);

        // S3削除成功後、DBから削除
        imageMemoRepository.delete(imageMemo);
        log.info("画像メモを削除しました: id={}, userId={}", id, userId);
    }
}
