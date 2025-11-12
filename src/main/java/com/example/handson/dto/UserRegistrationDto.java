package com.example.handson.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ユーザー登録DTO
 */
public record UserRegistrationDto(
        @NotBlank(message = "ユーザー名は必須です")
        @Size(min = 3, max = 50, message = "ユーザー名は3文字以上50文字以下で入力してください")
        String username,

        @NotBlank(message = "パスワードは必須です")
        @Size(min = 8, message = "パスワードは8文字以上で入力してください")
        String password,

        @NotBlank(message = "確認用パスワードは必須です")
        String confirmPassword
) {
}
