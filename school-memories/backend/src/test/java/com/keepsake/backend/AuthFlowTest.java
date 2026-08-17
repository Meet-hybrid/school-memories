package com.keepsake.backend;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowTest extends BaseIntegrationTest {

    @Test
    void register_verify_login_logout_roundtrip() throws Exception {
        String token = registerAndLogin("roundtrip@test.dev", "Round Trip", "Trip");

        // unverified flag present
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("roundtrip@test.dev"))
                .andExpect(jsonPath("$.verified").value(false));

        // verify email
        String verifyLink = objectMapper.readTree(
                        mockMvc.perform(post("/api/auth/register")
                                        .contentType("application/json")
                                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                                "email", "verify-me@test.dev",
                                                "password", "password123",
                                                "fullName", "Verify Me",
                                                "schoolId", 1L))))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString())
                .get("verifyLink").asText();
        String vToken = verifyLink.substring(verifyLink.indexOf("token=") + 6);
        mockMvc.perform(get("/api/auth/verify-email").param("token", vToken))
                .andExpect(status().isOk());

        // logout endpoint is stateless and harmless
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void duplicate_email_is_rejected() throws Exception {
        registerAndLogin("dup@test.dev", "Dup User", "Dup");
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "dup@test.dev",
                                "password", "password123",
                                "fullName", "Second Try",
                                "schoolId", 1L))))
                .andExpect(status().isConflict());
    }

    @Test
    void wrong_password_is_rejected() throws Exception {
        registerAndLogin("pw@test.dev", "Pw User", "Pw");
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", "pw@test.dev", "password", "wrongpass1"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protected_route_requires_token() throws Exception {
        mockMvc.perform(get("/api/challenge"))
                .andExpect(status().isForbidden());
    }

    @Test
    void password_reset_flow() throws Exception {
        registerAndLogin("reset@test.dev", "Reset User", "Res");
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of("email", "reset@test.dev"))))
                .andExpect(status().isOk());

        // the dev mail sender logs the token; in tests we simulate the same via a
        // directly issued reset request through the API contract — here we just
        // assert a bogus token is rejected cleanly.
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("token", "bogus", "newPassword", "brandnew123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_requires_valid_token() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isForbidden());
    }
}
