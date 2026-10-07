package com.lifetrack;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the prod profile on a real Tomcat, since forwarded headers are handled by
 * Tomcat's RemoteIpValve, which MockMvc bypasses.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "security.failed-attempts.max-per-account=2"
)
@ActiveProfiles({"test", "prod"})
class ProductionProfileTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void healthCheck_shouldBePublic() {
        ResponseEntity<String> response = rest.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void apiDocs_shouldBeDisabled() {
        assertThat(rest.getForEntity("/v3/api-docs", String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rest.getForEntity("/swagger-ui.html", String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void loginLimit_shouldUseClientIpForwardedByTheProxy() {
        rest.postForEntity("/api/auth/register",
            json(Map.of("name", "Prod", "email", "prod@test.com", "password", "123456"), null), String.class);

        for (int i = 0; i < 2; i++) {
            assertThat(login("203.0.113.5", "wrong1").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        // The proxy (127.0.0.1 here) is shared; the client behind it is not.
        assertThat(login("203.0.113.5", "123456").getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(login("203.0.113.6", "123456").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<String> login(String clientIp, String password) {
        return rest.postForEntity("/api/auth/login",
            json(Map.of("email", "prod@test.com", "password", password), clientIp), String.class);
    }

    private static HttpEntity<Map<String, String>> json(Map<String, String> body, String forwardedFor) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (forwardedFor != null) {
            headers.set("X-Forwarded-For", forwardedFor);
        }
        return new HttpEntity<>(body, headers);
    }
}
