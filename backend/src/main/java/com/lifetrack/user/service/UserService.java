package com.lifetrack.user.service;

import com.lifetrack.auth.dto.AuthResponse;
import com.lifetrack.auth.dto.UserResponse;
import com.lifetrack.auth.entity.EmailCodePurpose;
import com.lifetrack.auth.service.EmailCodeService;
import com.lifetrack.auth.service.AuthService;
import com.lifetrack.auth.service.RefreshTokenService;
import com.lifetrack.book.repository.BookRepository;
import com.lifetrack.common.exception.ApiException;
import com.lifetrack.common.security.FailedAttemptLimiter;
import com.lifetrack.media.repository.MediaRepository;
import com.lifetrack.run.repository.RunRepository;
import com.lifetrack.user.dto.ChangePasswordRequest;
import com.lifetrack.user.dto.UpdateProfileRequest;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RunRepository runRepository;
    private final BookRepository bookRepository;
    private final MediaRepository mediaRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuthService authService;
    private final FailedAttemptLimiter attemptLimiter;
    private final EmailCodeService emailCodeService;
    private final int maxFailures;

    public UserService(
        UserRepository userRepository,
        RunRepository runRepository,
        BookRepository bookRepository,
        MediaRepository mediaRepository,
        PasswordEncoder passwordEncoder,
        RefreshTokenService refreshTokenService,
        AuthService authService,
        FailedAttemptLimiter attemptLimiter,
        EmailCodeService emailCodeService,
        @Value("${security.failed-attempts.max-per-account:5}") int maxFailures
    ) {
        this.userRepository = userRepository;
        this.runRepository = runRepository;
        this.bookRepository = bookRepository;
        this.mediaRepository = mediaRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.authService = authService;
        this.attemptLimiter = attemptLimiter;
        this.emailCodeService = emailCodeService;
        this.maxFailures = maxFailures;
    }

    public UserResponse getCurrentUser(String email) {
        return toResponse(findUser(email));
    }

    public UserResponse updateProfile(UpdateProfileRequest request, String email) {
        User user = findUser(email);
        user.setName(request.name().trim());
        return toResponse(userRepository.save(user));
    }

    /** Confirms the address with the code sent at registration (or by {@link #resendVerificationCode}). */
    public UserResponse verifyEmail(String code, String email) {
        User user = findUser(email);
        if (!user.isEmailVerified()) {
            emailCodeService.consume(user, EmailCodePurpose.VERIFY_EMAIL, code.trim());
            user.setEmailVerified(true);
            user = userRepository.save(user);
        }
        return toResponse(user);
    }

    public void resendVerificationCode(String email) {
        User user = findUser(email);
        if (user.isEmailVerified()) {
            throw ApiException.badRequest("E-posta adresin zaten doğrulanmış");
        }
        authService.sendVerificationCode(user);
    }

    /**
     * Signs out every other device by revoking all refresh tokens, and returns a
     * fresh pair so the caller stays signed in.
     */
    @Transactional
    public AuthResponse changePassword(ChangePasswordRequest request, String email) {
        User user = findUser(email);
        requirePassword(user, request.currentPassword(), "currentPassword");
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAll(user);
        return authService.toAuthResponse(user);
    }

    /** Deletes the account and everything it owns; refresh tokens go with the user row. */
    @Transactional
    public void deleteAccount(String password, String email) {
        User user = findUser(email);
        requirePassword(user, password, "password");
        runRepository.deleteAllByOwner(user);
        bookRepository.deleteAllByOwner(user);
        mediaRepository.deleteAllByOwner(user);
        userRepository.delete(user);
    }

    // Limited per account: someone holding a stolen access token could otherwise
    // guess the password here.
    private void requirePassword(User user, String password, String field) {
        String key = "password-check:" + user.getId();
        attemptLimiter.check(key, maxFailures);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            attemptLimiter.recordFailure(key);
            throw ApiException.invalidField(field, "Şifre hatalı");
        }
        attemptLimiter.reset(key);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> ApiException.notFound("Kullanıcı bulunamadı"));
    }

    private static UserResponse toResponse(User user) {
        return UserResponse.from(user);
    }
}
