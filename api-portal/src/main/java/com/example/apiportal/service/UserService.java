package com.example.apiportal.service;

import com.example.apiportal.domain.User;
import com.example.apiportal.dto.UserRegistrationRequest;
import com.example.apiportal.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(UserRegistrationRequest requestDto) {
        // 1. 사용자 이름 중복 확인
        if (userRepository.existsByUsername(requestDto.username())) {
            // TODO: 좀 더 구체적인 예외 타입 정의 (예: UsernameAlreadyExistsException)
            throw new IllegalArgumentException("Username already exists: " + requestDto.username());
        }

        // 2. User 객체 생성 및 비밀번호 암호화
        User newUser = User.builder()
                .username(requestDto.username())
                .password(passwordEncoder.encode(requestDto.password()))
                .build();

        // 3. API 키 생성
        newUser.generateApiKey();

        // 4. 사용자 정보 저장
        return userRepository.save(newUser);
    }
}
