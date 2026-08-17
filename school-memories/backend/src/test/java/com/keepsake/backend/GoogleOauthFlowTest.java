package com.keepsake.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MvcResult;

import com.keepsake.backend.auth.GoogleIdTokenVerifier;
import com.keepsake.backend.auth.GoogleIdTokenVerifier.GoogleUser;
import com.keepsake.backend.common.ApiException;
import com.fasterxml.jackson.databind.JsonNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoogleOauthFlowTest extends BaseIntegrationTest {

    /**
     * Replaces the real (network-backed) Google verifier with a stub so the whole
     * flow is testable offline. The real verifier is exercised by manual testing.
     */
    @TestConfiguration
    static class StubGoogleConfig {

        @Bean
        @Primary
        GoogleIdTokenVerifier googleIdTokenVerifier() {
            return token -> switch (token) {
                case "good-token" -> new GoogleUser("google-sub-1", "google.user@test.dev", true,
                        "Google User", "https://example.com/pic.jpg");
                case "joiner-token" -> new GoogleUser("google-sub-2", "joiner.google@test.dev", true,
                        "Joiner Google", null);
                default -> throw ApiException.unauthorized("Invalid Google sign-in");
            };
        }
    }

    @Test
    void google_login_creates_user_without_school() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("idToken", "good-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("google.user@test.dev"))
                .andExpect(jsonPath("$.user.fullName").value("Google User"))
                .andExpect(jsonPath("$.user.avatarUrl").value("https://example.com/pic.jpg"))
                .andExpect(jsonPath("$.user.verified").value(true))
                .andExpect(jsonPath("$.user.schoolId").doesNotExist());
    }

    @Test
    void google_login_is_idempotent_for_same_email() throws Exception {
        long firstId = googleLoginUserId();
        long secondId = googleLoginUserId();
        org.assertj.core.api.Assertions.assertThat(firstId).isEqualTo(secondId);
    }

    @Test
    void google_login_joins_existing_password_account() throws Exception {
        // Someone already registered with a password and the same email as the Google account.
        registerAndLogin("joiner.google@test.dev", "Ada Obi", "Ada");

        long googleUserId = googleUserId("joiner-token");

        // Password login still works and resolves to the same account.
        String passwordToken = login("joiner.google@test.dev", "password123");
        JsonNode me = objectMapper.readTree(mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + passwordToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(me.get("id").asLong()).isEqualTo(googleUserId);
    }

    @Test
    void invalid_google_token_is_rejected() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("idToken", "forged-token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oauth_config_reports_enabled_and_client_id() throws Exception {
        mockMvc.perform(get("/api/auth/oauth-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.clientId").value("test-google-client-id"));
    }

    private long googleLoginUserId() throws Exception {
        return googleUserId("good-token");
    }

    private long googleUserId(String token) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("idToken", token))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString())
                .path("user").path("id").asLong();
    }
}
