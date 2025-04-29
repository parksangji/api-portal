package com.example.apiportal.controller;

import com.example.apiportal.dto.UserRegistrationRequest;
import com.example.apiportal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    // 1. 회원가입 폼 페이지 요청 처리 (GET)
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userRegistrationRequest", new UserRegistrationRequest("", ""));
        return "users/register";
    }

    // 2. 회원가입 요청 처리 (POST)
    @PostMapping("/register")
    public String handleRegistration(@Valid @ModelAttribute UserRegistrationRequest requestDto,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            log.warn("Registration validation errors: {}", bindingResult.getAllErrors());
            // TODO: 오류 메시지를 모델에 담아 폼에 표시하는 로직 추가 필요
            return "users/register";
        }

        try {
            userService.registerUser(requestDto);
            log.info("User registered successfully: {}", requestDto.username());
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            log.error("Registration failed for user {}: {}", requestDto.username(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/users/register";
        } catch (Exception e) {
            log.error("Unexpected error during registration for user {}: {}", requestDto.username(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
            return "redirect:/users/register";
        }
    }
}