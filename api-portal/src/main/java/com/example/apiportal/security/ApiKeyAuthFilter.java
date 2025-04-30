package com.example.apiportal.security;

import com.example.apiportal.domain.User;
import com.example.apiportal.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter { // OncePerRequestFilter 상속

    private final UserRepository userRepository;
    private static final String API_KEY_HEADER = "X-API-KEY";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        // 2. API Key 유효성 검사 (헤더가 없거나 비어있는 경우)
        if (!StringUtils.hasText(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Attempting authentication with API Key: {}", apiKey);

        // 3. DB에서 API Key로 사용자 조회
        Optional<User> userOptional = userRepository.findByApiKey(apiKey);

        // 4. API Key 유효 여부 판단 및 SecurityContext 설정
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            log.info("API Key validated successfully for user: {}", user.getUsername());

            // 5. 인증(Authentication) 객체 생성 및 SecurityContext에 저장
            //    - Principal: User 객체 또는 사용자 이름 등 식별자
            //    - Credentials: 보통 null 또는 API 키 자체
            //    - Authorities: 사용자의 권한 목록 (여기서는 임시로 ROLE_USER 부여)
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    apiKey,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("SecurityContext updated for user: {}", user.getUsername());

            // 6. 다음 필터 체인 진행
            filterChain.doFilter(request, response);

        } else {
            // 7. 유효하지 않은 API Key인 경우
            log.warn("Invalid API Key received: {}", apiKey);
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid API Key");
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid API Key");
        }
    }

    // 특정 경로(/api/**)에만 이 필터가 동작하도록 하는 로직은 SecurityConfig에서 처리
     @Override
     protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
         return !request.getRequestURI().startsWith("/api/");
     }
}