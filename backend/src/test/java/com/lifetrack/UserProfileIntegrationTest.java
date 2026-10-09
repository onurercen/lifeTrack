package com.lifetrack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifetrack.auth.repository.RefreshTokenRepository;
import com.lifetrack.book.repository.BookRepository;
import com.lifetrack.media.repository.MediaRepository;
import com.lifetrack.run.repository.RunRepository;
import com.lifetrack.user.repository.UserRepository;
import com.lifetrack.support.TestMailbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileIntegrationTest {

    private static final String EMAIL = "profile@test.com";

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

    private String accessToken;

    @Autowired
    private TestMailbox mailbox;

    @BeforeEach
    void setUp() throws Exception {
        runRepository.deleteAll();
        bookRepository.deleteAll();
        mediaRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        accessToken = register(EMAIL).get("token").asText();
    }

    @Test
    void updateProfile_shouldChangeTrimmedName() throws Exception {
        send(put("/api/users/me"), Map.of("name", "  Yeni İsim "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Yeni İsim"))
            .andExpect(jsonPath("$.email").value(EMAIL));

        send(put("/api/users/me"), Map.of("name", " "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.name").value("Ad soyad zorunludur"));
    }

    @Test
    void changePassword_shouldRequireCurrentPasswordAndSignOutOtherDevices() throws Exception {
        String otherDevice = login(EMAIL, "123456").get("refreshToken").asText();

        send(put("/api/users/me/password"), Map.of("currentPassword", "wrong1", "newPassword", "yeni-sifre"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Şifre hatalı"))
            .andExpect(jsonPath("$.errors.currentPassword").value("Şifre hatalı"));

        JsonNode fresh = body(send(put("/api/users/me/password"),
                Map.of("currentPassword", "123456", "newPassword", "yeni-sifre"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists()));

        // Other devices are signed out; the caller got a working new pair.
        refresh(otherDevice).andExpect(status().isUnauthorized());
        refresh(fresh.get("refreshToken").asText()).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", EMAIL, "password", "123456"))))
            .andExpect(status().isUnauthorized());
        login(EMAIL, "yeni-sifre");
    }

    @Test
    void changePassword_shouldValidateNewPassword() throws Exception {
        send(put("/api/users/me/password"), Map.of("currentPassword", "123456", "newPassword", "123"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.newPassword").value("Şifre en az 6 karakter olmalıdır"));
    }

    @Test
    void deleteAccount_shouldRemoveUserAndAllTheirData() throws Exception {
        String otherAccess = register("other@test.com").get("token").asText();
        verifyEmail(accessToken, EMAIL);
        verifyEmail(otherAccess, "other@test.com");
        for (String path : new String[] {"/api/runs", "/api/books", "/api/media"}) {
            Map<String, Object> payload = switch (path) {
                case "/api/runs" -> Map.of("distanceKm", 5.0, "durationMinutes", 30);
                case "/api/books" -> Map.of("title", "Dune", "author", "Frank Herbert");
                default -> Map.of("title", "Severance", "type", "Dizi");
            };
            send(post(path), payload).andExpect(status().isCreated());
            mockMvc.perform(post(path).header("Authorization", "Bearer " + otherAccess)
                    .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated());
        }

        send(delete("/api/users/me"), Map.of("password", "wrong1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.password").value("Şifre hatalı"));
        assertThat(userRepository.findByEmail(EMAIL)).isPresent();

        send(delete("/api/users/me"), Map.of("password", "123456")).andExpect(status().isNoContent());

        assertThat(userRepository.findByEmail(EMAIL)).isEmpty();
        assertThat(runRepository.count()).isEqualTo(1);
        assertThat(bookRepository.count()).isEqualTo(1);
        assertThat(mediaRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1); // the other user's
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isUnauthorized());
    }

    private JsonNode register(String email) throws Exception {
        return body(mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Profil", "email", email, "password", "123456"))))
            .andExpect(status().isCreated()));
    }

    private void verifyEmail(String access, String email) throws Exception {
        mockMvc.perform(post("/api/users/me/verify-email")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("code", mailbox.lastCode(email)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.emailVerified").value(true));
    }

    private JsonNode login(String email, String password) throws Exception {
        return body(mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))))
            .andExpect(status().isOk()));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("refreshToken", refreshToken))));
    }

    private ResultActions send(MockHttpServletRequestBuilder request, Map<String, ?> payload) throws Exception {
        return mockMvc.perform(request
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(payload)));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode body(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
