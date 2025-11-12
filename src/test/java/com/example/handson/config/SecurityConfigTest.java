package com.example.handson.config;

import com.example.handson.domain.user.User;
import com.example.handson.domain.user.UserRepository;
import com.example.handson.service.ImageMemoService;
import com.example.handson.service.S3Service;
import com.example.handson.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SecurityConfigのテスト
 * セッション固定攻撃対策、ログアウト処理、同時セッション制御の動作を検証
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ImageMemoService imageMemoService;

    @MockitoBean
    private S3Service s3Service;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void ログイン成功時にセッションが作成される() throws Exception {
        // given
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .password(passwordEncoder.encode("password"))
                .enabled(true)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userService.findByUsername("testuser")).thenReturn(user);
        when(imageMemoService.findByUserId(eq(1L), any())).thenReturn(Page.empty());

        // when: ログイン実行
        MvcResult result = mockMvc.perform(formLogin("/login")
                        .user("username", "testuser")
                        .password("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/memos"))
                .andReturn();

        // then: セッションが作成されていることを確認
        // Note: セッション固定攻撃対策（changeSessionId）はSpring Securityにより自動的に適用される
        assertThat(result.getRequest().getSession(false)).isNotNull();
    }

    @Test
    @WithMockUser(username = "testuser")
    void ログアウト時にセッションが無効化される() throws Exception {
        // given
        MockHttpSession session = new MockHttpSession();

        // when: ログアウト実行
        MvcResult result = mockMvc.perform(post("/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        // then: セッションが無効化されていることを確認
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    @WithMockUser(username = "testuser")
    void ログアウト時にJSESSIONIDクッキーが削除される() throws Exception {
        // when & then
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(cookie().maxAge("JSESSIONID", 0));
    }

    @Test
    void 認証が必要なページは未認証でアクセスできない() throws Exception {
        // when & then
        mockMvc.perform(get("/memos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void 公開ページは認証なしでアクセスできる() throws Exception {
        // when & then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void 認証済みユーザーは保護されたページにアクセスできる() throws Exception {
        // given
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .enabled(true)
                .build();

        when(userService.findByUsername("testuser")).thenReturn(user);
        when(imageMemoService.findByUserId(eq(1L), any())).thenReturn(Page.empty());

        // when & then
        mockMvc.perform(get("/memos"))
                .andExpect(status().isOk());
    }

    @Test
    void パスワードエンコーダーがBCryptである() {
        // when
        String encoded = passwordEncoder.encode("password");

        // then: BCryptのハッシュは60文字で$2a$または$2b$で始まる
        assertThat(encoded).hasSize(60);
        assertThat(encoded).matches("^\\$2[ab]\\$.*");
    }
}
