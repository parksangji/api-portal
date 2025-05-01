package com.example.apiportal.security;

import com.example.apiportal.domain.User;
import com.example.apiportal.repository.UserRepository;
import com.example.apiportal.service.ApiUsageService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final ApiUsageService apiUsageService;
    private static final String API_KEY_HEADER = "X-API-KEY";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (!StringUtils.hasText(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Attempting authentication with API Key: {}", apiKey);
        Optional<User> userOptional = userRepository.findByApiKey(apiKey);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String username = user.getUsername();
            log.info("API Key validated successfully for user: {}", user.getUsername());

            try {
                long usageCount = apiUsageService.recordUsage(apiKey);
                log.debug("Usage recorded for user: {}, key: [{}]. Hourly count: {}",
                        username, (apiKey.length() > 8 ? apiKey.substring(0, 8) + "..." : apiKey), usageCount);
            } catch (Exception e) {
                // Redis 오류 등으로 사용량 기록 실패 시 에러 로깅 (요청 자체는 허용할 수도 있음)
                log.error("Failed to record API usage for user: {}, key: [{}]",
                        username, (apiKey.length() > 8 ? apiKey.substring(0, 8) + "..." : apiKey), e);
            }

            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("SecurityContext updated with UserDetails for user: {}", user.getUsername());

                filterChain.doFilter(request, response);

            } catch (UsernameNotFoundException e) {
                log.error("User found by API key '{}' but UserDetails could not be loaded for username '{}'.", apiKey, user.getUsername(), e);
                SecurityContextHolder.clearContext();
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.getWriter().write("User authentication context setup error");
                return;
            }

        } else {
            log.warn("Invalid API Key received: {}", apiKey);
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid API Key");
        }
    }

     @Override
     protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
         return !request.getRequestURI().startsWith("/api/");
     }
}