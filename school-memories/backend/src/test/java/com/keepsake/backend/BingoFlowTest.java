package com.keepsake.backend;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BingoFlowTest extends BaseIntegrationTest {

    @Test
    void fresh_card_has_25_cells_and_regenerates() throws Exception {
        String token = registerAndLogin("bingo@test.dev", "Bingo Player", "Bing");

        JsonNode card = getBingoCard(token);
        assertThat(card.get("cells").size()).isEqualTo(25);
        for (JsonNode cell : card.get("cells")) {
            assertThat(cell.get("rule").asText()).isNotBlank();
            assertThat(cell.get("prompt").asText()).isNotBlank();
        }

        regenerate(token);
        JsonNode again = getBingoCard(token);
        assertThat(again.get("cells").size()).isEqualTo(25);
    }

    @Test
    void claim_verifies_rules_server_side() throws Exception {
        String token = registerAndLogin("bingo2@test.dev", "Bingo Player Two", "Bing2");

        // Cards are random 5x5 draws from the rule pool, so retry until NEW_FRIEND appears.
        JsonNode card = getBingoCard(token);
        for (int i = 0; i < 25 && !hasRule(card, "NEW_FRIEND"); i++) {
            card = regenerate(token);
        }
        assertThat(hasRule(card, "NEW_FRIEND")).as("card contains NEW_FRIEND after retries").isTrue();

        // seeded classmates exist and a fresh user follows nobody -> NEW_FRIEND verifies true
        claim(token, "NEW_FRIEND")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.matched.name").isNotEmpty());

        // every other rule is impossible for a fresh user (no follows/likes/comments/
        // memories/set/year) — at least one of them is on the card and must come back false
        boolean sawFalse = false;
        for (JsonNode cell : card.get("cells")) {
            String rule = cell.get("rule").asText();
            if ("NEW_FRIEND".equals(rule)) {
                continue;
            }
            JsonNode result = objectMapper.readTree(claim(token, rule).andReturn().getResponse().getContentAsString());
            if (!result.get("done").asBoolean()) {
                sawFalse = true;
                break;
            }
        }
        assertThat(sawFalse).isTrue();

        // unknown squares are rejected
        claim(token, "NOT_A_RULE").andExpect(status().isBadRequest());

        // the verified cell is persisted on the card
        mockMvc.perform(get("/api/games/bingo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedCount").value(1));
    }

    private JsonNode getBingoCard(String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/games/bingo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode regenerate(String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/games/bingo/regenerate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.ResultActions claim(String token, String rule) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("rule", rule);
        return mockMvc.perform(post("/api/games/bingo/claim")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(body)));
    }

    private static boolean hasRule(JsonNode card, String rule) {
        for (JsonNode cell : card.get("cells")) {
            if (rule.equals(cell.get("rule").asText())) {
                return true;
            }
        }
        return false;
    }
}
