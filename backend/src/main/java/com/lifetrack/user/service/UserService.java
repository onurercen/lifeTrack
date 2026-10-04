package com.lifetrack.user.service;

import com.lifetrack.auth.dto.UserResponse;
import com.lifetrack.common.exception.ApiException;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> ApiException.notFound("Kullanıcı bulunamadı"));
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
