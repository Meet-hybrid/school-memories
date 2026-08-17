package com.keepsake.backend;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChallengeFlowTest extends BaseIntegrationTest {

    @Test
    void challenge_has_30_seeded_questions() throws Exception {
        String token = registerAndLogin("challenge@test.dev", "Challenge User", "Chal");
        mockMvc.perform(get("/api/challenge").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(30))
                .andExpect(jsonPath("$.days.length()").value(30))
                .andExpect(jsonPath("$.answeredCount").value(0));
    }

    @Test
    void submit_then_duplicate_is_rejected() throws Exception {
        String token = registerAndLogin("submit@test.dev", "Submit User", "Sub");
        mockMvc.perform(multipart("/api/memories")
                        .param("day", "4")
                        .param("answer", "Mrs. Boateng. She made us believe our accents were beautiful.")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayNumber").value(4));

        // second submission for the same day conflicts
        mockMvc.perform(multipart("/api/memories")
                        .param("day", "4")
                        .param("answer", "again")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        // day view reflects the answer
        mockMvc.perform(get("/api/challenge/day/4").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memory.dayNumber").value(4));
    }

    @Test
    void like_and_comment_update_counts() throws Exception {
        String author = registerAndLogin("author@test.dev", "Author User", "Auth");
        String reader = registerAndLogin("reader@test.dev", "Reader User", "Read");

        String memoryId = String.valueOf(objectMapper.readTree(mockMvc.perform(multipart("/api/memories")
                        .param("day", "9")
                        .param("answer", "The mango tree behind the science block.")
                        .header("Authorization", "Bearer " + author))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asLong());

        // reader likes
        mockMvc.perform(post("/api/memories/" + memoryId + "/reactions")
                        .header("Authorization", "Bearer " + reader)
                        .contentType("application/json")
                        .content("{\"type\":\"LIKE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true));

        // reader comments
        mockMvc.perform(post("/api/memories/" + memoryId + "/comments")
                        .header("Authorization", "Bearer " + reader)
                        .contentType("application/json")
                        .content("{\"body\":\"I remember that tree!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("I remember that tree!"));

        // counts visible on the memory
        mockMvc.perform(get("/api/memories/" + memoryId).header("Authorization", "Bearer " + reader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1))
                .andExpect(jsonPath("$.comments").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));

        // un-like removes it
        mockMvc.perform(post("/api/memories/" + memoryId + "/reactions")
                        .header("Authorization", "Bearer " + reader)
                        .contentType("application/json")
                        .content("{\"type\":\"LIKE\"}"))
                .andExpect(jsonPath("$.liked").value(false));
    }
}
