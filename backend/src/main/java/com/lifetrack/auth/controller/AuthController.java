package com.lifetrack.auth.controller;

import com.lifetrack.auth.dto.AuthResponse;
import com.lifetrack.auth.dto.ForgotPasswordRequest;
import com.lifetrack.auth.dto.LoginRequest;
import com.lifetrack.auth.dto.RefreshTokenRequest;
import com.lifetrack.auth.dto.RegisterRequest;
import com.lifetrack.auth.dto.ResetPasswordRequest;
import com.lifetrack.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(authService.login(request, http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    // Public on purpose: a client whose access token already expired must still be able to sign out.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /** Always 204, whether or not the address belongs to an account. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        authService.forgotPassword(request.getEmail(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(authService.resetPassword(request, http.getRemoteAddr()));
    }
}
