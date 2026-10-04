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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
            .andExpect(status().isUnauthorized())
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
    void cors_shouldAllowLocalDevServerOnAnyPortButNotOtherOrigins() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:54321")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:54321"));

        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "https://evil.example.com")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden());
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
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Bu e-posta ile kayıtlı kullanıcı mevcut"));
    }

    @Test
    void token_fromLogin_shouldAuthenticateProtectedEndpoints() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Token User");
        request.setEmail("token@test.com");
        request.setPassword("123456");

        String body = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("token").asText();

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("token@test.com"));
    }

    @Test
    void emails_shouldBeCaseInsensitive() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Ayşe");
        request.setEmail("  Ayse.Case@Test.COM ");
        request.setPassword("123456");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.email").value("ayse.case@test.com"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "AYSE.CASE@test.com", "password": "123456"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value("ayse.case@test.com"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Kopya", "email": "ayse.case@TEST.com", "password": "654321"}
                    """))
            .andExpect(status().isConflict());
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
