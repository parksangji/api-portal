package com.example.apiportal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration; // 만료 시간 설정을 위함
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiUsageService {

    private final StringRedisTemplate redisTemplate;
    private static final DateTimeFormatter HOURLY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final long EXPIRATION_MINUTES = 70;

    /**
     * 주어진 API 키에 대해 현재 시간 기준 API 사용량을 1 증가시킵니다.
     * Redis의 INCR 명령어를 사용하며, 해당 시간대의 첫 요청 시 만료 시간을 설정합니다.
     *
     * @param apiKey API 요청에 사용된 키
     * @return 해당 시간대의 현재 누적 사용량 (증가 후 값)
     */
    public long recordUsage(String apiKey) {
        // 1. 현재 시간 기준으로 Redis 키 생성
        String hourlyKey = buildHourlyKey(apiKey);

        // 2. Redis의 INCR 명령 실행 (값 1 증가 및 현재 값 반환)
        Long currentCount = redisTemplate.opsForValue().increment(hourlyKey);

        // INCR 결과 null 체크 (일반적으로 발생하지 않음)
        if (currentCount == null) {
            log.error("Redis INCR returned null for key: {}", hourlyKey);
            throw new RuntimeException("Failed to record API usage in Redis");
        }

        // 3. 만약 이번 증가로 인해 카운트가 1이 되었다면 (해당 시간 첫 요청)
        if (currentCount == 1) {
            log.debug("Setting expiration ({}) for new hourly usage key: {}", EXPIRATION_MINUTES, hourlyKey);
            redisTemplate.expire(hourlyKey, Duration.ofMinutes(EXPIRATION_MINUTES));
        }

        String maskedApiKey = apiKey.length() > 8 ? apiKey.substring(0, 8) + "..." : apiKey;
        log.debug("API usage recorded for key [{}]. Current hourly count: {}. (Redis Key: {})",
                maskedApiKey, currentCount, hourlyKey);

        return currentCount;
    }

    /**
     * API 키와 현재 시간을 조합하여 시간별 Redis 키를 생성합니다.
     * 형식: "api_usage:{apiKey}:{yyyyMMddHH}"
     */
    private String buildHourlyKey(String apiKey) {
        String currentHour = LocalDateTime.now().format(HOURLY_FORMATTER);
        return String.format("api_usage:%s:%s", apiKey, currentHour);
    }
}
