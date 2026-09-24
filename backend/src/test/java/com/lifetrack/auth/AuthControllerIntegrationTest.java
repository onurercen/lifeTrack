package com.lifetrack.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifetrack.auth.dto.LoginRequest;
import com.lifetrack.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_shouldCreateUserAndReturnToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Ahmet");
        request.setEmail("ahmet@test.com");
        request.setPassword("123456");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.user.email").value("ahmet@test.com"));
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() throws Exception {
        register("login@test.com", "123456");

        LoginRequest request = new LoginRequest();
        request.setEmail("login@test.com");
        request.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.user.email").value("login@test.com"));
    }

    @Test
    void login_shouldRejectInvalidPassword() throws Exception {
        register("wrong-password@test.com", "123456");

        LoginRequest request = new LoginRequest();
        request.setEmail("wrong-password@test.com");
        request.setPassword("654321");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Giriş bilgileri hatalı"));
    }

    @Test
    void authEndpoints_shouldAllowCorsPreflightFromLocalFrontend() throws Exception {
        mockMvc.perform(options("/api/auth/register")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS"));
    }

    @Test
    void register_shouldRejectDuplicateEmail() throws Exception {
        register("duplicate@test.com", "123456");

        RegisterRequest request = new RegisterRequest();
        request.setName("Mehmet");
        request.setEmail("duplicate@test.com");
        request.setPassword("654321");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Bu e-posta ile kayıtlı kullanıcı mevcut"));
    }

    private void register(String email, String password) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail(email);
        request.setPassword(password);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }
}
