package com.example.handson.controller;

import com.example.handson.dto.UserRegistrationDto;
import com.example.handson.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ユーザーコントローラー
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto("", "", ""));
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("user") UserRegistrationDto dto,
            BindingResult result,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            return "register";
        }

        userService.registerUser(dto);
        redirectAttributes.addFlashAttribute("successMessage", "登録が完了しました。ログインしてください。");
        return "redirect:/login";

        // 注: IllegalArgumentExceptionはGlobalExceptionHandlerがキャッチ
    }
}
