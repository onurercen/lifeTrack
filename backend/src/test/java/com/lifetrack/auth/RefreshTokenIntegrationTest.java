package com.lifetrack.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifetrack.auth.repository.RefreshTokenRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.findByEmail("refresh@test.com").ifPresent(userRepository::delete);
    }

    @Test
    void login_shouldReturnRefreshTokenThatIsStoredOnlyAsHash() throws Exception {
        JsonNode auth = register();

        String refreshToken = auth.get("refreshToken").asText();
        assertThat(refreshToken).hasSizeGreaterThan(40);
        assertThat(refreshTokenRepository.findAll())
            .singleElement()
            .satisfies(stored -> assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo(refreshToken));
    }

    @Test
    void refresh_shouldRotateTokensAndReturnWorkingAccessToken() throws Exception {
        String firstRefresh = register().get("refreshToken").asText();

        JsonNode refreshed = body(refresh(firstRefresh)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value("refresh@test.com")));
        String newRefresh = refreshed.get("refreshToken").asText();
        assertThat(newRefresh).isNotEqualTo(firstRefresh);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + refreshed.get("token").asText()))
            .andExpect(status().isOk());

        // The new token works once more; the old one does not.
        refresh(newRefresh).andExpect(status().isOk());
    }

    @Test
    void reusingRotatedToken_shouldRevokeEveryActiveSession() throws Exception {
        String stolen = register().get("refreshToken").asText();
        String legit = body(refresh(stolen)).get("refreshToken").asText();

        refresh(stolen)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Oturumunuzun süresi doldu, lütfen tekrar giriş yapın"));

        // The legitimate client's token was revoked too, so both parties must log in again.
        refresh(legit).andExpect(status().isUnauthorized());
        assertThat(refreshTokenRepository.countByUserAndRevokedAtIsNull(
            userRepository.findByEmail("refresh@test.com").orElseThrow())).isZero();
    }

    @Test
    void refresh_shouldRejectUnknownOrMissingToken() throws Exception {
        refresh("not-a-real-token").andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.refreshToken").exists());
    }

    @Test
    void loggedOutToken_shouldBeRejectedWithoutEndingOtherSessions() throws Exception {
        String phone = register().get("refreshToken").asText();
        String tablet = body(mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "refresh@test.com", "password": "123456"}
                    """))
            .andExpect(status().isOk())).get("refreshToken").asText();

        logout(phone).andExpect(status().isNoContent());
        refresh(phone).andExpect(status().isUnauthorized());

        refresh(tablet).andExpect(status().isOk());
    }

    @Test
    void logout_shouldRevokeTokenAndBeIdempotent() throws Exception {
        String refreshToken = register().get("refreshToken").asText();

        logout(refreshToken).andExpect(status().isNoContent());
        logout(refreshToken).andExpect(status().isNoContent());
        logout("unknown").andExpect(status().isNoContent());

        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    private JsonNode register() throws Exception {
        return body(mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Refresh", "email": "refresh@test.com", "password": "123456"}
                    """))
            .andExpect(status().isCreated()));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))));
    }

    private ResultActions logout(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
