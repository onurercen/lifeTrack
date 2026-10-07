package com.lifetrack.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Refresh tokens that live zero days are already expired when issued. */
@SpringBootTest(properties = "jwt.refresh-expiration-days=0")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshTokenExpiryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void refresh_shouldRejectExpiredToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Expired", "email": "expired@test.com", "password": "123456"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(body).get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
            .andExpect(status().isUnauthorized());
    }
}
