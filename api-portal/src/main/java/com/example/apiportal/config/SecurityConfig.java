package com.example.apiportal.config;

import com.example.apiportal.repository.UserRepository;
import com.example.apiportal.security.ApiKeyAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 특정 경로(/api/**)에만 이 필터 체인을 적용
                .securityMatcher(new AntPathRequestMatcher("/api/**"))
                // 2. 세션 관리: 상태 없음(Stateless)으로 설정 (API 키 인증 사용)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 3. CSRF 보호 비활성화 (Stateless API에서는 보통 사용 안 함)
                .csrf(AbstractHttpConfigurer::disable)
                // 4. FormLogin, HttpBasic, Logout 비활성화 (API에는 불필요)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // 5. 커스텀 API 키 인증 필터 추가
                //    UsernamePasswordAuthenticationFilter.class 앞에 위치시켜 먼저 실행되도록 함
                //    또는 AuthorizationFilter.class 앞에 위치시키는 것도 일반적임
                .addFilterBefore(new ApiKeyAuthFilter(userRepository), UsernamePasswordAuthenticationFilter.class)
                // 6. 요청 권한 설정: /api/** 경로는 인증(API키 인증)을 요구
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 요청별 권한 설정 (기존 설정 유지)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/", "/users/register", "/login", "/error", "/webjars/**").permitAll() // 루트, 등록, 로그인 등 허용
                        .requestMatchers("/users/my-profile/**", "/api-docs", "/home").authenticated() // 인증 필요한 페이지
                        .anyRequest().authenticated() // 그 외 모든 웹 요청은 인증 필요
                )
                // 2. 폼 기반 로그인 설정 (기존 설정 유지)
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                // 3. 로그아웃 설정 (기존 설정 유지)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );
        // CSRF 설정 (Thymeleaf 사용 시 기본 활성화 권장)
        // .csrf(csrf -> csrf.disable()); // 개발 초기 테스트 시 비활성화 고려

        return http.build();
    }
}