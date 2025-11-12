package com.example.handson.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * セキュリティ設定
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/register", "/css/**", "/js/**", "/images/**", "/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/memos", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)  // セッション無効化
                        .deleteCookies("JSESSIONID")  // セッションクッキー削除
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionFixation().changeSessionId()  // セッション固定攻撃対策（ログイン時にIDを変更）
                        .maximumSessions(1)  // 同一ユーザーの同時セッション数を1に制限
                        .maxSessionsPreventsLogin(false)  // 新しいログインを優先（古いセッションを無効化）
                );

        return http.build();
    }
}
