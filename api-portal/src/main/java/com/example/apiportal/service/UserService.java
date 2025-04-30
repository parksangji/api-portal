package com.example.apiportal.service;

import com.example.apiportal.domain.User;
import com.example.apiportal.dto.UserRegistrationRequest;
import com.example.apiportal.exception.UserNotFoundException;
import com.example.apiportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(UserRegistrationRequest requestDto) {
        if (userRepository.existsByUsername(requestDto.username())) {
            throw new IllegalArgumentException("Username already exists: " + requestDto.username());
        }
        User newUser = User.builder()
                .username(requestDto.username())
                .password(passwordEncoder.encode(requestDto.password()))
                .build();
        newUser.generateApiKey();
        return userRepository.save(newUser);
    }

    @Transactional()
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }

    @Transactional
    public void regenerateApiKey(String username) {
        User user = getUserByUsername(username);
        user.generateApiKey();
        userRepository.save(user);
        log.info("API Key regenerated for user: {}", username);
    }
}
