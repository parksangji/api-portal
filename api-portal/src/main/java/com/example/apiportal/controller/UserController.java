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
import org.springframework.web.bind.annotation.*;
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
    // UserController.java 수정 부분
    @PostMapping("/register")
    public String handleRegistration(@Valid @ModelAttribute("userRegistrationRequest") UserRegistrationRequest requestDto, // ModelAttribute 이름 명시
                                     BindingResult bindingResult, // @Valid 결과 확인 (@ModelAttribute 뒤에 와야 함)
                                     Model model, // 모델 추가
                                     RedirectAttributes redirectAttributes) {

        // Validation 오류 확인
        if (bindingResult.hasErrors()) {
            log.warn("Registration validation errors: {}", bindingResult.getAllErrors());
            // 오류가 있으면 모델에 bindingResult가 자동으로 담기므로,
            // 입력된 내용(requestDto)과 함께 다시 회원가입 폼으로 돌아감
            // model.addAttribute("userRegistrationRequest", requestDto); // @ModelAttribute("userRegistrationRequest") 로 자동 추가됨
            return "users/register"; // 리다이렉트 없이 뷰 이름 반환
        }

        try {
            userService.registerUser(requestDto);
            log.info("User registered successfully: {}", requestDto.username());
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
            return "redirect:/login"; // 성공 시 로그인 페이지로 리다이렉트
        } catch (IllegalArgumentException e) {
            log.error("Registration failed for user {}: {}", requestDto.username(), e.getMessage());
            // 사용자 이름 중복과 같은 특정 오류는 필드 오류로 추가 가능
            // bindingResult.rejectValue("username", "error.user", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage()); // 일반 오류 메시지 추가
            // model.addAttribute("userRegistrationRequest", requestDto); // @ModelAttribute("userRegistrationRequest") 로 자동 추가됨
            return "users/register"; // 실패 시 다시 회원가입 폼으로 (입력 유지)
        } catch (Exception e) {
            log.error("Unexpected error during registration for user {}: {}", requestDto.username(), e.getMessage(), e);
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
            // model.addAttribute("userRegistrationRequest", requestDto); // @ModelAttribute("userRegistrationRequest") 로 자동 추가됨
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
}