package com.lifetrack.user.controller;

import com.lifetrack.auth.dto.AuthResponse;
import com.lifetrack.auth.dto.UserResponse;
import com.lifetrack.user.dto.ChangePasswordRequest;
import com.lifetrack.user.dto.DeleteAccountRequest;
import com.lifetrack.user.dto.DataExport;
import com.lifetrack.user.dto.UpdateProfileRequest;
import com.lifetrack.user.dto.VerifyEmailRequest;
import com.lifetrack.user.service.DataExportService;
import com.lifetrack.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final DataExportService dataExportService;

    public UserController(UserService userService, DataExportService dataExportService) {
        this.userService = userService;
        this.dataExportService = dataExportService;
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

    @PostMapping("/me/verify-email")
    public ResponseEntity<UserResponse> verifyEmail(
        @Valid @RequestBody VerifyEmailRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(userService.verifyEmail(request.code(), principal.getUsername()));
    }

    @PostMapping("/me/verify-email/resend")
    public ResponseEntity<Void> resendVerificationCode(@AuthenticationPrincipal UserDetails principal) {
        userService.resendVerificationCode(principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    /** Everything the user stored, as a JSON file download. */
    @GetMapping("/me/export")
    public ResponseEntity<DataExport> exportData(@AuthenticationPrincipal UserDetails principal) {
        DataExport export = dataExportService.export(principal.getUsername());
        String filename = "lifetrack-" + export.exportedAt().toLocalDate() + ".json";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
            .body(export);
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
