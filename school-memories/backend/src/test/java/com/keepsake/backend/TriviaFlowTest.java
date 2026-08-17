package com.keepsake.backend;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TriviaFlowTest extends BaseIntegrationTest {

    private String adminToken() throws Exception {
        return login("admin@greenfield.demo", "password123");
    }

    @Test
    void admin_creates_trivia_and_answers_are_checked() throws Exception {
        String token = registerAndLogin("trivia@test.dev", "Trivia Player", "Triv");

        // admin creates a question
        JsonNode created = objectMapper.readTree(mockMvc.perform(post("/api/admin/games/trivia")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "question", "What colour is the sky?",
                                "options", java.util.List.of("Blue", "Green", "Red", "Yellow"),
                                "correctIndex", 0))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        long questionId = created.get("id").asLong();

        // a player can fetch a round (any active question) and answer the created one
        mockMvc.perform(get("/api/games/trivia/next").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").isNotEmpty())
                .andExpect(jsonPath("$.options.length()").value(4));

        mockMvc.perform(post("/api/games/trivia/" + questionId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of("optionIndex", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));

        mockMvc.perform(post("/api/games/trivia/" + questionId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of("optionIndex", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false));

        mockMvc.perform(get("/api/games/score").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triviaCorrect").value(1));
    }

    @Test
    void regular_users_cannot_manage_trivia() throws Exception {
        String token = registerAndLogin("trivia2@test.dev", "Trivia Player Two", "Triv2");
        mockMvc.perform(post("/api/admin/games/trivia")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "question", "Nope",
                                "options", java.util.List.of("A", "B", "C", "D"),
                                "correctIndex", 0))))
                .andExpect(status().isForbidden());
    }

    @Test
    void seeded_trivia_questions_are_playable() throws Exception {
        String token = registerAndLogin("trivia3@test.dev", "Trivia Player Three", "Triv3");

        JsonNode round = objectMapper.readTree(mockMvc.perform(get("/api/games/trivia/next")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertThat(round.get("options").size()).isEqualTo(4);
        assertThat(round.get("question").asText()).isNotBlank();
    }
}
