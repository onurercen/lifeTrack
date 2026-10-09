package com.lifetrack.support;

import com.lifetrack.user.service.UserDetailsServiceImpl;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

public final class TestUsers {

    private TestUsers() {
    }

    /** A signed-in user whose e-mail is verified, as the JWT filter would set up. */
    public static RequestPostProcessor verifiedUser(String email) {
        return user(email).authorities(
            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"),
            new org.springframework.security.core.authority.SimpleGrantedAuthority(UserDetailsServiceImpl.EMAIL_VERIFIED)
        );
    }
}
