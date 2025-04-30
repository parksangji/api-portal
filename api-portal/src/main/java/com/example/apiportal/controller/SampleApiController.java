package com.example.apiportal.controller;

import com.example.apiportal.dto.sample.SampleGreetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sample")
public class SampleApiController {

    private static final Logger log = LoggerFactory.getLogger(SampleApiController.class);

    /**
     * GET /api/sample/greet 엔드포인트
     * 인증된 사용자에게 인사말을 반환합니다.
     * 'name' 쿼리 파라미터가 있으면 해당 이름을 사용하고, 없으면 인증된 사용자 이름을 사용합니다.
     */
    @GetMapping("/greet")
    public ResponseEntity<SampleGreetResponse> greetUser(
            @RequestParam(value = "name", required = false) String nameParam,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            log.warn("Access attempt to /api/sample/greet without authentication principal.");
            return ResponseEntity.status(401).build();
        }

        String authenticatedUsername = userDetails.getUsername();
        log.info("API request to /greet received from user: {}", authenticatedUsername);

        String nameToGreet = StringUtils.hasText(nameParam) ? nameParam : authenticatedUsername;

        String message = String.format("Hello, %s! Welcome to our API.", nameToGreet);

        SampleGreetResponse response = new SampleGreetResponse(message);

        return ResponseEntity.ok(response);
    }

}
