package com.example.handson.controller;

import com.example.handson.config.S3Properties;
import com.example.handson.domain.imagememo.ImageMemo;
import com.example.handson.domain.user.User;
import com.example.handson.service.ImageDownloadResult;
import com.example.handson.service.ImageMemoService;
import com.example.handson.service.S3Service;
import com.example.handson.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ImageMemoControllerのテスト
 * 画像配信エンドポイントの動作を検証
 */
@WebMvcTest(ImageMemoController.class)
class ImageMemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageMemoService imageMemoService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private S3Service s3Service;

    @MockitoBean
    private S3Properties s3Properties;

    @Test
    @WithMockUser(username = "testuser")
    void 認証済みユーザーが自分の画像にアクセスできる() throws Exception {
        // given
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .enabled(true)
                .build();

        ImageMemo memo = ImageMemo.builder()
                .id(1L)
                .user(user)
                .title("Test Memo")
                .description("Test Description")
                .s3Key("uploads/test.jpg")
                .build();

        byte[] imageData = "test image data".getBytes();
        InputStream inputStream = new ByteArrayInputStream(imageData);
        ImageDownloadResult downloadResult = new ImageDownloadResult(
                inputStream,
                "image/jpeg",
                imageData.length
        );

        when(userService.findByUsername("testuser")).thenReturn(user);
        when(imageMemoService.findByIdAndUserId(1L, 1L)).thenReturn(memo);
        when(s3Properties.getBucketName()).thenReturn("test-bucket");
        when(s3Properties.getCacheMaxAge()).thenReturn(3600);
        when(s3Service.downloadImageStream(eq("test-bucket"), eq("uploads/test.jpg")))
                .thenReturn(downloadResult);

        // when & then
        mockMvc.perform(get("/memos/1/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().exists("Content-Length"))
                .andExpect(header().string("Cache-Control", "max-age=3600, private"));
    }

    @Test
    void 認証なしでアクセスすると認証が必要() throws Exception {
        // when & then
        // Spring Securityの設定により、未認証アクセスはログインページへリダイレクトまたは401/403を返す
        mockMvc.perform(get("/memos/1/image"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void 存在しない画像にアクセスすると404エラー() throws Exception {
        // given
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .enabled(true)
                .build();

        when(userService.findByUsername("testuser")).thenReturn(user);
        when(imageMemoService.findByIdAndUserId(999L, 1L)).thenReturn(null);

        // when & then
        mockMvc.perform(get("/memos/999/image"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void 他ユーザーの画像にアクセスすると404エラー() throws Exception {
        // given
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .enabled(true)
                .build();

        when(userService.findByUsername("testuser")).thenReturn(user);
        // 他ユーザーの画像なので、findByIdAndUserIdがnullを返す
        when(imageMemoService.findByIdAndUserId(2L, 1L)).thenReturn(null);

        // when & then
        mockMvc.perform(get("/memos/2/image"))
                .andExpect(status().isNotFound());
    }
}
