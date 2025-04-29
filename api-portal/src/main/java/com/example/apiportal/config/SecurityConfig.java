package com.example.apiportal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 요청별 권한 설정
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll() // 정적 리소스 허용
                        .requestMatchers("/", "/users/register", "/login", "/error").permitAll() // 특정 경로 허용
                        .anyRequest().authenticated() // 나머지 요청은 인증 필요
                )
                // 폼 기반 로그인 설정
                .formLogin(formLogin -> formLogin
                        .loginPage("/login") // 커스텀 로그인 페이지 경로
                        .loginProcessingUrl("/login") // 로그인 처리 URL (Spring Security가 처리)
                        .defaultSuccessUrl("/home", true) // 로그인 성공 시 이동할 기본 URL
                        .failureUrl("/login?error=true") // 로그인 실패 시 이동할 URL
                        .permitAll() // 로그인 페이지 접근 허용
                )
                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/logout") // 로그아웃 처리 URL
                        .logoutSuccessUrl("/login?logout=true") // 로그아웃 성공 시 이동할 URL
                        .invalidateHttpSession(true) // 세션 무효화
                        .deleteCookies("JSESSIONID") // 쿠키 삭제 (필요시)
                        .permitAll() // 로그아웃 URL 접근 허용
                );
        // CSRF 설정 (Thymeleaf 사용 시 기본 활성화 권장)
        // .csrf(csrf -> csrf.disable()); // 개발 초기 테스트 시 비활성화 고려

        return http.build();
    }
}