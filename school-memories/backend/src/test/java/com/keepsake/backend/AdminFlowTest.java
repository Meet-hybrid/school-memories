package com.keepsake.backend;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminFlowTest extends BaseIntegrationTest {

    @Test
    void admin_can_manage_questions_and_regular_users_cannot() throws Exception {
        String adminToken = login("admin@greenfield.demo", "password123");

        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").value(30));

        mockMvc.perform(post("/api/admin/questions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"dayNumber\":32,\"question\":\"Favourite school food?\",\"hint\":\"Canteen deep lore.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayNumber").value(32));

        // a normal user is blocked from admin endpoints
        String userToken = registerAndLogin("plain@test.dev", "Plain User", "Plain");
        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_stats_and_announcements() throws Exception {
        String adminToken = login("admin@greenfield.demo", "password123");

        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"title\":\"Test announcement\",\"body\":\"Testing announcements.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test announcement"));
    }
}
