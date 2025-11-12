package com.example.handson.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * 画像メモ作成DTO
 */
public record ImageMemoCreateDto(
        @NotBlank(message = "タイトルは必須です")
        @Size(max = 255, message = "タイトルは255文字以下で入力してください")
        String title,

        @Size(max = 1000, message = "説明は1000文字以下で入力してください")
        String description,

        @NotNull(message = "画像ファイルは必須です")
        MultipartFile image
) {
}
