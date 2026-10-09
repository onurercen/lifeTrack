package com.lifetrack.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifetrack.support.TestMailbox;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** E-mail verification and password reset with codes sent by e-mail. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountEmailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestMailbox mailbox;

    @Test
    void register_shouldSendCodeAndBlockDataUntilVerified() throws Exception {
        String email = "verify@test.com";
        JsonNode session = register(email);
        String access = session.get("token").asText();
        assertThat(session.at("/user/emailVerified").asBoolean()).isFalse();
        assertThat(mailbox.mailsTo(email)).hasSize(1);

        mockMvc.perform(get("/api/runs").header("Authorization", "Bearer " + access))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        // Account endpoints stay open.
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.emailVerified").value(false));

        String code = mailbox.lastCode(email);
        authed(post("/api/users/me/verify-email"), access, Map.of("code", wrong(code)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.code").value("Kod hatalı veya süresi dolmuş"));
        authed(post("/api/users/me/verify-email"), access, Map.of("code", code))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.emailVerified").value(true));

        // The same access token works now: authorities are loaded per request.
        mockMvc.perform(get("/api/runs").header("Authorization", "Bearer " + access))
            .andExpect(status().isOk());
        assertThat(login(email, "123456").at("/user/emailVerified").asBoolean()).isTrue();
    }

    @Test
    void resendVerification_shouldWaitForCooldownAndRefuseVerifiedAccounts() throws Exception {
        String email = "resend@test.com";
        String access = register(email).get("token").asText();

        authed(post("/api/users/me/verify-email/resend"), access, null)
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"));

        authed(post("/api/users/me/verify-email"), access, Map.of("code", mailbox.lastCode(email)))
            .andExpect(status().isOk());
        authed(post("/api/users/me/verify-email/resend"), access, null)
            .andExpect(status().isBadRequest());
    }

    @Test
    void verificationCode_shouldBeDroppedAfterTooManyWrongGuesses() throws Exception {
        String email = "guess@test.com";
        String access = register(email).get("token").asText();
        String code = mailbox.lastCode(email);

        for (int i = 0; i < 4; i++) {
            authed(post("/api/users/me/verify-email"), access, Map.of("code", wrong(code)))
                .andExpect(jsonPath("$.errors.code").value("Kod hatalı veya süresi dolmuş"));
        }
        authed(post("/api/users/me/verify-email"), access, Map.of("code", wrong(code)))
            .andExpect(jsonPath("$.errors.code").value("Çok fazla hatalı deneme. Lütfen yeni kod isteyin."));
        authed(post("/api/users/me/verify-email"), access, Map.of("code", code))
            .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPassword_shouldAnswerTheSameForUnknownAddresses() throws Exception {
        postJson("/api/auth/forgot-password", Map.of("email", "nobody@test.com"))
            .andExpect(status().isNoContent());
        assertThat(mailbox.mailsTo("nobody@test.com")).isEmpty();

        postJson("/api/auth/reset-password", Map.of("email", "nobody@test.com", "code", "123456", "newPassword", "yeni-sifre"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.code").value("Kod hatalı veya süresi dolmuş"));
    }

    @Test
    void resetPassword_shouldSetNewPasswordAndSignOutEverywhere() throws Exception {
        String email = "reset@test.com";
        String oldRefresh = register(email).get("refreshToken").asText();

        postJson("/api/auth/forgot-password", Map.of("email", " Reset@Test.com "))
            .andExpect(status().isNoContent());
        // A second request within the cooldown sends nothing but answers the same.
        postJson("/api/auth/forgot-password", Map.of("email", email))
            .andExpect(status().isNoContent());
        assertThat(mailbox.mailsTo(email)).hasSize(2); // verification + one reset
        String code = mailbox.lastCode(email);

        postJson("/api/auth/reset-password", Map.of("email", email, "code", wrong(code), "newPassword", "yeni-sifre"))
            .andExpect(status().isBadRequest());
        postJson("/api/auth/reset-password", Map.of("email", email, "code", code, "newPassword", "123"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.newPassword").value("Şifre en az 6 karakter olmalıdır"));

        postJson("/api/auth/reset-password", Map.of("email", email, "code", code, "newPassword", "yeni-sifre"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            // The code proved the address.
            .andExpect(jsonPath("$.user.emailVerified").value(true));

        postJson("/api/auth/refresh", Map.of("refreshToken", oldRefresh)).andExpect(status().isUnauthorized());
        postJson("/api/auth/login", Map.of("email", email, "password", "123456")).andExpect(status().isUnauthorized());
        login(email, "yeni-sifre");

        // Single use.
        postJson("/api/auth/reset-password", Map.of("email", email, "code", code, "newPassword", "baska-sifre"))
            .andExpect(status().isBadRequest());
    }

    private static String wrong(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    private JsonNode register(String email) throws Exception {
        return body(postJson("/api/auth/register", Map.of("name", "Deniz", "email", email, "password", "123456"))
            .andExpect(status().isCreated()));
    }

    private JsonNode login(String email, String password) throws Exception {
        return body(postJson("/api/auth/login", Map.of("email", email, "password", password))
            .andExpect(status().isOk()));
    }

    private ResultActions postJson(String path, Map<String, ?> payload) throws Exception {
        return mockMvc.perform(post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)));
    }

    private ResultActions authed(
        MockHttpServletRequestBuilder request,
        String access,
        Map<String, ?> payload
    ) throws Exception {
        request.header("Authorization", "Bearer " + access);
        if (payload != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload));
        }
        return mockMvc.perform(request);
    }

    private JsonNode body(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
