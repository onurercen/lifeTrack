package com.lifetrack.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/health")
    public String health() {
        return "User service is running";
    }

    @GetMapping("/me")
    public String getCurrentUserProfile() {
        return "User profile";
    }
}
