package com.example.handson.controller;

import com.example.handson.config.S3Properties;
import com.example.handson.domain.imagememo.ImageMemo;
import com.example.handson.domain.user.User;
import com.example.handson.dto.ImageMemoCreateDto;
import com.example.handson.service.ImageDownloadResult;
import com.example.handson.service.ImageMemoService;
import com.example.handson.service.S3Service;
import com.example.handson.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * 画像メモコントローラー
 */
@Controller
@RequestMapping("/memos")
@RequiredArgsConstructor
@Slf4j
public class ImageMemoController {

    private final ImageMemoService imageMemoService;
    private final UserService userService;
    private final S3Service s3Service;
    private final S3Properties s3Properties;

    @GetMapping
    public String listMemos(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model
    ) {
        User user = userService.findByUsername(userDetails.getUsername());
        Page<ImageMemo> memos = imageMemoService.findByUserId(user.getId(), pageable);
        model.addAttribute("memos", memos);
        return "memos/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("memo", new ImageMemoCreateDto("", "", null));
        return "memos/create";
    }

    @PostMapping
    public String createMemo(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("memo") ImageMemoCreateDto dto,
            BindingResult result,
            RedirectAttributes redirectAttributes
    ) throws IOException {
        if (result.hasErrors()) {
            return "memos/create";
        }

        User user = userService.findByUsername(userDetails.getUsername());
        imageMemoService.createImageMemo(dto, user);
        redirectAttributes.addFlashAttribute("successMessage", "画像メモを作成しました。");
        return "redirect:/memos";

        // 注: IllegalArgumentException、IOExceptionはGlobalExceptionHandlerがキャッチ
    }

    @GetMapping("/{id}")
    public String showMemo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.findByUsername(userDetails.getUsername());
        ImageMemo memo = imageMemoService.findByIdAndUserId(id, user.getId());

        if (memo == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "画像メモが見つかりません。");
            return "redirect:/memos";
        }

        model.addAttribute("memo", memo);
        return "memos/detail";
    }

    @GetMapping("/{id}/delete-confirm")
    public String showDeleteConfirm(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.findByUsername(userDetails.getUsername());
        ImageMemo memo = imageMemoService.findByIdAndUserId(id, user.getId());

        if (memo == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "画像メモが見つかりません。");
            return "redirect:/memos";
        }

        model.addAttribute("memo", memo);
        return "memos/delete-confirm";
    }

    @PostMapping("/{id}/delete")
    public String deleteMemo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.findByUsername(userDetails.getUsername());
        imageMemoService.deleteImageMemo(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "画像メモを削除しました。");
        return "redirect:/memos";

        // 注: IllegalArgumentExceptionはGlobalExceptionHandlerがキャッチ
    }

    /**
     * 画像を配信（アプリケーションProxy方式）
     * S3バケット名を外部に露出せず、認証・認可を統合
     *
     * @param userDetails ログイン中のユーザー情報
     * @param id 画像メモID
     * @return 画像データ（ストリーミング配信）
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<InputStreamResource> getImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        // ユーザー認証チェック（Spring Securityで自動実行済み）
        User user = userService.findByUsername(userDetails.getUsername());

        // 画像メモを取得（所有者チェック）
        ImageMemo memo = imageMemoService.findByIdAndUserId(id, user.getId());
        if (memo == null) {
            log.warn("画像メモが見つからないか、アクセス権限がありません: id={}, userId={}", id, user.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // S3から画像をダウンロード
        ImageDownloadResult result = s3Service.downloadImageStream(s3Properties.getBucketName(), memo.getS3Key());

        // HTTPヘッダーを設定
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.contentType()));
        headers.setContentLength(result.contentLength());
        // ブラウザキャッシュを有効化（プロパティから取得）
        headers.setCacheControl("max-age=" + s3Properties.getCacheMaxAge() + ", private");

        log.info("画像を配信しました: id={}, userId={}, s3Key={}", id, user.getId(), memo.getS3Key());

        // InputStreamResourceでストリーミング配信
        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(result.inputStream()));

        // 注: StorageExceptionはGlobalExceptionHandlerがキャッチ
    }
}
