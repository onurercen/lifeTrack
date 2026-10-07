package com.lifetrack.user.controller;

import com.lifetrack.auth.dto.AuthResponse;
import com.lifetrack.auth.dto.UserResponse;
import com.lifetrack.user.dto.ChangePasswordRequest;
import com.lifetrack.user.dto.DeleteAccountRequest;
import com.lifetrack.user.dto.UpdateProfileRequest;
import com.lifetrack.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getUsername()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
        @Valid @RequestBody UpdateProfileRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(userService.updateProfile(request, principal.getUsername()));
    }

    @PutMapping("/me/password")
    public ResponseEntity<AuthResponse> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(userService.changePassword(request, principal.getUsername()));
    }

    // The password is sent in the body so a stolen access token alone can't delete the account.
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
        @Valid @RequestBody DeleteAccountRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        userService.deleteAccount(request.password(), principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
