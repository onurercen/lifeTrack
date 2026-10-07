package com.lifetrack.auth.service;

import com.lifetrack.auth.dto.AuthResponse;
import com.lifetrack.auth.dto.LoginRequest;
import com.lifetrack.auth.dto.RegisterRequest;
import com.lifetrack.auth.dto.UserResponse;
import com.lifetrack.common.exception.ApiException;
import com.lifetrack.common.security.FailedAttemptLimiter;
import com.lifetrack.common.security.JwtService;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final FailedAttemptLimiter attemptLimiter;
    private final int maxFailuresPerAccount;
    private final int maxFailuresPerIp;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        RefreshTokenService refreshTokenService,
        FailedAttemptLimiter attemptLimiter,
        @Value("${security.failed-attempts.max-per-account:5}") int maxFailuresPerAccount,
        @Value("${security.failed-attempts.max-per-ip:20}") int maxFailuresPerIp
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.attemptLimiter = attemptLimiter;
        this.maxFailuresPerAccount = maxFailuresPerAccount;
        this.maxFailuresPerIp = maxFailuresPerIp;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("Bu e-posta ile kayıtlı kullanıcı mevcut");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        return toAuthResponse(savedUser);
    }

    /**
     * Failed logins are limited per IP and e-mail pair (one account) and per IP
     * (many accounts). Keying the account limit by IP too means nobody can lock
     * someone else out just by knowing their e-mail address.
     */
    public AuthResponse login(LoginRequest request, String clientIp) {
        String ipKey = "login-ip:" + clientIp;
        String accountKey = "login-account:" + clientIp + "|" + request.getEmail();
        attemptLimiter.check(ipKey, maxFailuresPerIp);
        attemptLimiter.check(accountKey, maxFailuresPerAccount);

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            attemptLimiter.recordFailure(ipKey);
            attemptLimiter.recordFailure(accountKey);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Giriş bilgileri hatalı", ex);
        }
        attemptLimiter.reset(accountKey);

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> ApiException.unauthorized("Giriş bilgileri hatalı"));

        return toAuthResponse(user);
    }

    /** Exchanges a refresh token for a new access token and a new refresh token. */
    public AuthResponse refresh(String refreshToken) {
        return toAuthResponse(refreshTokenService.consume(refreshToken));
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    /** Builds a fresh token pair; also used after a password change. */
    public AuthResponse toAuthResponse(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .roles("USER")
            .build();

        String token = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(token, refreshToken, new UserResponse(user.getId(), user.getName(), user.getEmail()));
    }
}
