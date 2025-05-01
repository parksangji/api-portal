package com.example.apiportal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiUsageService {

    private final StringRedisTemplate redisTemplate;
    private static final DateTimeFormatter HOURLY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final long EXPIRATION_MINUTES = 70;

    @Value("${api.rate-limit.hourly:100}")
    private long hourlyLimit;
    public long recordUsage(String apiKey) {
        String hourlyKey = buildHourlyKey(apiKey);
        Long currentCount = redisTemplate.opsForValue().increment(hourlyKey);
        if (currentCount == null) {
            throw new RuntimeException("Failed to record API usage in Redis");
        }
        if (currentCount == 1) {
            redisTemplate.expire(hourlyKey, Duration.ofMinutes(EXPIRATION_MINUTES));
        }
        String maskedApiKey = apiKey.length() > 8 ? apiKey.substring(0, 8) + "..." : apiKey;
        log.debug("API usage recorded for key [{}]. Current hourly count: {}. (Redis Key: {})",
                maskedApiKey, currentCount, hourlyKey);
        return currentCount;
    }

    private String buildHourlyKey(String apiKey) {
        String currentHour = LocalDateTime.now().format(HOURLY_FORMATTER);
        return String.format("api_usage:%s:%s", apiKey, currentHour);
    }

    /**
     * 주어진 API 키의 현재 시간당 사용량이 설정된 한도를 초과했는지 확인합니다.
     * 주의: 이 메서드는 사용량을 증가시키지 않고 확인만 합니다.
     *
     * @param apiKey 확인할 API 키
     * @return 한도를 초과했으면 true, 아니면 false
     */
    public boolean isLimitExceeded(String apiKey) {
        String hourlyKey = buildHourlyKey(apiKey);

        String currentCountStr = redisTemplate.opsForValue().get(hourlyKey);

        long currentCount = 0;
        if (StringUtils.hasText(currentCountStr)) {
            try {
                currentCount = Long.parseLong(currentCountStr);
            } catch (NumberFormatException e) {
                log.warn("Could not parse Redis count '{}' for key '{}'. Assuming count is 0.", currentCountStr, hourlyKey);
            }
        }

        boolean exceeded = currentCount >= hourlyLimit;

        if (exceeded) {
            String maskedApiKey = apiKey.length() > 8 ? apiKey.substring(0, 8) + "..." : apiKey;
            log.warn("Rate limit check: Limit EXCEEDED for key [{}]. Current count: {}, Limit: {}",
                    maskedApiKey, currentCount, hourlyLimit);
        } else {
             log.debug("Rate limit check: OK for key [{}]. Current count: {}, Limit: {}", apiKey, currentCount, hourlyLimit);
        }

        return exceeded;
    }
}
