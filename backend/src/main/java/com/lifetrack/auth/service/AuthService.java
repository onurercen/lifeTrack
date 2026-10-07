package com.lifetrack.auth.service;

import com.lifetrack.auth.dto.AuthResponse;
import com.lifetrack.auth.dto.LoginRequest;
import com.lifetrack.auth.dto.RegisterRequest;
import com.lifetrack.auth.dto.UserResponse;
import com.lifetrack.common.exception.ApiException;
import com.lifetrack.common.security.JwtService;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
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

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
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

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Giriş bilgileri hatalı", ex);
        }

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
