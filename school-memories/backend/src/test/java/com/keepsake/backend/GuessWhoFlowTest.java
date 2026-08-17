package com.keepsake.backend;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GuessWhoFlowTest extends BaseIntegrationTest {

    @Test
    void round_offers_four_classmate_options() throws Exception {
        String token = registerAndLogin("gw@test.dev", "Gw Player", "Gw");

        JsonNode round = objectMapper.readTree(mockMvc.perform(get("/api/games/guess-who")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty())
                .andExpect(jsonPath("$.options").isArray())
                .andReturn().getResponse().getContentAsString());

        JsonNode options = round.get("options");
        assertThat(options.size()).isEqualTo(4);
        for (JsonNode option : options) {
            assertThat(option.get("userId").asLong()).isPositive();
            assertThat(option.get("name").asText()).isNotBlank();
        }
    }

    @Test
    void exactly_one_option_is_correct_and_the_score_updates() throws Exception {
        String token = registerAndLogin("gw2@test.dev", "Gw Player Two", "Gw2");

        JsonNode round = objectMapper.readTree(mockMvc.perform(get("/api/games/guess-who")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        long memoryId = round.get("memoryId").asLong();

        int correctCount = 0;
        for (JsonNode option : round.get("options")) {
            long guessed = option.get("userId").asLong();
            JsonNode result = objectMapper.readTree(mockMvc.perform(post("/api/games/guess-who/" + memoryId + "/guess")
                            .header("Authorization", "Bearer " + token)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(java.util.Map.of("userId", guessed))))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString());
            if (result.get("correct").asBoolean()) {
                correctCount++;
            }
        }
        assertThat(correctCount).isEqualTo(1);

        mockMvc.perform(get("/api/games/score").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guessWhoCorrect").value(1))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void guessing_a_missing_memory_is_404() throws Exception {
        String token = registerAndLogin("gw3@test.dev", "Gw Player Three", "Gw3");
        mockMvc.perform(post("/api/games/guess-who/999999/guess")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of("userId", 1L))))
                .andExpect(status().isNotFound());
    }
}
