package com.example.handson.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;

/**
 * グローバル例外ハンドラー
 * アプリケーション全体で発生する例外を統一的に処理
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * バリデーションエラー・ビジネスロジックエラー
     * HTTPステータス: 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(
            IllegalArgumentException e,
            Model model
    ) {
        log.warn("バリデーションエラー: {}", e.getMessage());
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("errorType", "validation");
        return "error/400";
    }

    /**
     * ストレージ操作エラー（S3等）
     * HTTPステータス: 500 Internal Server Error
     */
    @ExceptionHandler(StorageException.class)
    public String handleStorageException(
            StorageException e,
            Model model
    ) {
        log.error("ストレージエラー", e);
        model.addAttribute("errorMessage", "ファイル操作でエラーが発生しました。しばらく時間をおいて再度お試しください。");
        model.addAttribute("errorType", "storage");
        return "error/500";
    }

    /**
     * ファイルI/Oエラー
     * HTTPステータス: 500 Internal Server Error
     */
    @ExceptionHandler(IOException.class)
    public String handleIOException(
            IOException e,
            Model model
    ) {
        log.error("ファイルI/Oエラー", e);
        model.addAttribute("errorMessage", "ファイル処理でエラーが発生しました。");
        model.addAttribute("errorType", "io");
        return "error/500";
    }

    /**
     * 予期しないエラー
     * HTTPステータス: 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(
            Exception e,
            Model model
    ) {
        log.error("予期しないエラー", e);
        model.addAttribute("errorMessage", "システムエラーが発生しました。管理者に連絡してください。");
        model.addAttribute("errorType", "unexpected");
        return "error/500";
    }
}
