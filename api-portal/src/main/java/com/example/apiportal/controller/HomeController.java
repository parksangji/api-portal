package com.example.apiportal.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        model.addAttribute("username", currentPrincipalName);
        return "home";
    }

    @GetMapping("/")
    public String root() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }
        return "redirect:/home";
    }

    @GetMapping("/api-docs")
    public String showApiDocsPage(Model model) {
        model.addAttribute("apiVersion", "v1.0");
        return "api-docs";
    }

}
