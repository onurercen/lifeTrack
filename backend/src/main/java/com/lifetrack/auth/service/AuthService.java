package com.lifetrack.auth.service;

import com.lifetrack.auth.dto.AuthResponse;
import com.lifetrack.auth.dto.LoginRequest;
import com.lifetrack.auth.dto.RegisterRequest;
import com.lifetrack.auth.dto.UserResponse;
import com.lifetrack.common.exception.ApiException;
import com.lifetrack.common.security.JwtService;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Bu e-posta ile kayıtlı kullanıcı mevcut");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
            .username(savedUser.getEmail())
            .password(savedUser.getPassword())
            .roles("USER")
            .build();

        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail()));
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new ApiException("Giriş bilgileri hatalı", ex);
        }

        if (!authentication.isAuthenticated()) {
            throw new ApiException("Giriş bilgileri hatalı");
        }

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ApiException("Kullanıcı bulunamadı"));

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .roles("USER")
            .build();

        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, new UserResponse(user.getId(), user.getName(), user.getEmail()));
    }
}
