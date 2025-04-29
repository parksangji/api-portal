package com.example.apiportal.repository;

import com.example.apiportal.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByApiKey(String apiKey);

    boolean existsByUsername(String username);
}