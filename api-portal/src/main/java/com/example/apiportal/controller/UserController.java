package com.example.apiportal.controller;

import com.example.apiportal.domain.User;
import com.example.apiportal.dto.UserRegistrationRequest;
import com.example.apiportal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userRegistrationRequest", new UserRegistrationRequest("", ""));
        return "users/register";
    }
    @PostMapping("/register")
    public String handleRegistration(@Valid @ModelAttribute("userRegistrationRequest") UserRegistrationRequest requestDto,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "users/register";
        }
        try {
            userService.registerUser(requestDto); // Service 호출
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "users/register";
        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "An unexpected error occurred.");
            return "users/register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        return "login";
    }

    @GetMapping("/my-profile")
    public String showProfilePage(@AuthenticationPrincipal UserDetails userDetails, Model model, RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            String username = userDetails.getUsername();
            User user = userService.getUserByUsername(username);
            model.addAttribute("user", user);
            return "users/profile";
        } catch (UsernameNotFoundException e) {
            log.warn("Attempted to access profile for non-existent user: {}", userDetails.getUsername());
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
            return "redirect:/home";
        }
    }

    @PostMapping("/my-profile/regenerate-key")
    public String regenerateApiKey(@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            String username = userDetails.getUsername();
            userService.regenerateApiKey(username); // Service 메서드 호출
            redirectAttributes.addFlashAttribute("successMessage", "API Key regenerated successfully.");
        } catch (UsernameNotFoundException e) { // Service에서 던진 예외 처리
            log.warn("Attempted to regenerate key for non-existent user: {}", userDetails.getUsername());
            redirectAttributes.addFlashAttribute("errorMessage", "User not found. Could not regenerate key.");
        } catch (Exception e) {
            log.error("Error regenerating API key for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to regenerate API key. Please try again.");
        }
        return "redirect:/users/my-profile"; // 리다이렉트 경로 수정
    }
}