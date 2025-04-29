package com.example.apiportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record UserRegistrationRequest(
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
        String username,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password
) {
}
