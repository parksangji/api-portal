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

        String maskedApiKey = apiKey.length() > 8 ? apiKey.substring(0, 8) + "..." : apiKey;
        log.debug("Attempting authentication with API Key: [{}]", maskedApiKey);

        Optional<User> userOptional = userRepository.findByApiKey(apiKey);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String username = user.getUsername();
            log.info("API Key validated successfully for user: {}", user.getUsername());

            if (apiUsageService.isLimitExceeded(apiKey)) {
                log.warn("API Request REJECTED due to rate limit exceeded for user: {}, key: [{}]", username, maskedApiKey);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Rate limit exceeded. Please try again later.");
                 response.addHeader("Retry-After", "60");
                return; // 필터 체인 중단
            }

            try {
                long usageCount = apiUsageService.recordUsage(apiKey); // 한도 체크 통과 후 기록
                log.debug("Usage recorded for user: {}, key: [{}]. Hourly count: {}", username, maskedApiKey, usageCount);
            } catch (Exception e) {
                log.error("Failed to record API usage for user: {}, key: [{}]", username, maskedApiKey, e);
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
            }

        } else {
            log.warn("Invalid API Key received: [{}]", maskedApiKey);
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