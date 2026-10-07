package com.lifetrack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifetrack.auth.repository.RefreshTokenRepository;
import com.lifetrack.book.repository.BookRepository;
import com.lifetrack.media.repository.MediaRepository;
import com.lifetrack.run.repository.RunRepository;
import com.lifetrack.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "security.failed-attempts.max-per-account=3",
    "security.failed-attempts.max-per-ip=5",
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginRateLimitIntegrationTest {

    // Each test uses its own client IPs; the limiter outlives a single test.
    private static final AtomicInteger NEXT_IP = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() throws Exception {
        runRepository.deleteAll();
        bookRepository.deleteAll();
        mediaRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Limit", "email", "limit@test.com", "password", "123456"))))
            .andExpect(status().isCreated());
    }

    @Test
    void login_shouldBlockAccountFromOneIpButNotLockOutOtherIps() throws Exception {
        String attacker = newIp();
        for (int i = 0; i < 3; i++) {
            login(attacker, "limit@test.com", "wrong1").andExpect(status().isUnauthorized());
        }

        // Even the right password is refused now, without being checked.
        login(attacker, "limit@test.com", "123456")
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", matchesPattern("8\\d\\d|900")))
            .andExpect(jsonPath("$.message").value("Çok fazla başarısız deneme. Lütfen 15 dakika sonra tekrar deneyin."));

        login(newIp(), "limit@test.com", "123456").andExpect(status().isOk());
    }

    @Test
    void successfulLogin_shouldResetTheAccountCounter() throws Exception {
        String ip = newIp();
        login(ip, "limit@test.com", "wrong1").andExpect(status().isUnauthorized());
        login(ip, "limit@test.com", "wrong2").andExpect(status().isUnauthorized());
        login(ip, "limit@test.com", "123456").andExpect(status().isOk());

        login(ip, "limit@test.com", "wrong3").andExpect(status().isUnauthorized());
        login(ip, "limit@test.com", "wrong4").andExpect(status().isUnauthorized());
        login(ip, "limit@test.com", "123456").andExpect(status().isOk());
    }

    @Test
    void login_shouldBlockIpTryingManyAccounts() throws Exception {
        String ip = newIp();
        for (int i = 0; i < 5; i++) {
            login(ip, "someone" + i + "@test.com", "wrong1").andExpect(status().isUnauthorized());
        }
        login(ip, "limit@test.com", "123456").andExpect(status().isTooManyRequests());
    }

    @Test
    void passwordChecks_shouldBeLimitedForSignedInUsers() throws Exception {
        String token = objectMapper.readTree(login(newIp(), "limit@test.com", "123456")
            .andReturn().getResponse().getContentAsString()).get("token").asText();

        for (int i = 0; i < 3; i++) {
            changePassword(token, "wrong1").andExpect(status().isBadRequest());
        }
        changePassword(token, "123456").andExpect(status().isTooManyRequests());
    }

    private ResultActions login(String ip, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
            .with(request -> {
                request.setRemoteAddr(ip);
                return request;
            })
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", email, "password", password))));
    }

    private ResultActions changePassword(String token, String currentPassword) throws Exception {
        return mockMvc.perform(put("/api/users/me/password")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("currentPassword", currentPassword, "newPassword", "yeni-sifre"))));
    }

    private static String newIp() {
        return "10.0.0." + NEXT_IP.getAndIncrement();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
