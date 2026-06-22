package com.showdown.backend.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ShowdownApiIntegrationTests {
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "admin1234";
    private static final String REFEREE_USER = "referee";
    private static final String REFEREE_PASSWORD = "referee1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCrudPublicReadAndScoringFlowWorks() throws Exception {
        mockMvc.perform(get("/api/admin/tournaments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/tournaments").with(httpBasic(ADMIN_USER, "wrong-password")))
                .andExpect(status().isUnauthorized());

        String suffix = Long.toString(System.nanoTime());
        JsonNode tournament = postAdmin("/api/admin/tournaments", Map.of(
                "code", "api-test-" + suffix,
                "name", "API Test Open " + suffix,
                "location", "Seoul",
                "startDate", "2026-07-01",
                "endDate", "2026-07-02",
                "timezone", "Asia/Seoul",
                "status", "PUBLISHED",
                "defaultLanguage", "ko"
        ));
        String tournamentId = tournament.get("id").asText();
        String tournamentCode = tournament.get("code").asText();

        JsonNode division = postAdmin("/api/admin/tournaments/" + tournamentId + "/divisions", Map.of(
                "name", "Open Singles",
                "code", "OPEN",
                "category", "OPEN",
                "sortOrder", 1,
                "active", true
        ));
        String divisionId = division.get("id").asText();

        JsonNode player1 = postAdmin("/api/admin/tournaments/" + tournamentId + "/players", Map.of(
                "displayName", "Alice API",
                "countryCode", "KOR",
                "divisionId", divisionId,
                "seedNo", 1,
                "entryNo", 1,
                "clubName", "Blue Club",
                "status", "ACTIVE"
        ));
        JsonNode player2 = postAdmin("/api/admin/tournaments/" + tournamentId + "/players", Map.of(
                "displayName", "Bob API",
                "countryCode", "USA",
                "divisionId", divisionId,
                "seedNo", 2,
                "entryNo", 2,
                "clubName", "Red Club",
                "status", "ACTIVE"
        ));

        JsonNode stage = postAdmin("/api/admin/tournaments/" + tournamentId + "/stages", Map.of(
                "divisionId", divisionId,
                "name", "Preliminary",
                "stageType", "ROUND_ROBIN",
                "sortOrder", 1
        ));
        String stageId = stage.get("id").asText();

        JsonNode group = postAdmin("/api/admin/tournaments/" + tournamentId + "/groups", Map.of(
                "divisionId", divisionId,
                "stageId", stageId,
                "code", "A",
                "name", "Group A",
                "groupType", "LEAGUE",
                "sortOrder", 1
        ));
        String groupId = group.get("id").asText();

        JsonNode referee1 = postAdmin("/api/admin/tournaments/" + tournamentId + "/officials", Map.of(
                "name", "Ref API 1",
                "shortCode", "RA1",
                "roleName", "REFEREE",
                "active", true
        ));
        JsonNode referee2 = postAdmin("/api/admin/tournaments/" + tournamentId + "/officials", Map.of(
                "name", "Ref API 2",
                "shortCode", "RA2",
                "roleName", "REFEREE",
                "active", true
        ));

        Map<String, Object> matchRequest = new HashMap<>();
        matchRequest.put("divisionId", divisionId);
        matchRequest.put("stageId", stageId);
        matchRequest.put("groupId", groupId);
        matchRequest.put("matchNo", 1);
        matchRequest.put("scheduledAt", "2026-07-01T10:00:00+09:00");
        matchRequest.put("courtName", "Court 1");
        matchRequest.put("durationMinutes", 30);
        matchRequest.put("refereeOfficialIds", new String[] {referee1.get("id").asText(), referee2.get("id").asText()});
        matchRequest.put("player1TournamentPlayerId", player1.get("id").asText());
        matchRequest.put("player2TournamentPlayerId", player2.get("id").asText());
        matchRequest.put("status", "SCHEDULED");
        JsonNode match = postAdmin("/api/admin/tournaments/" + tournamentId + "/matches", matchRequest);
        String matchId = match.get("id").asText();
        int version = match.get("version").asInt();

        mockMvc.perform(get("/api/public/tournaments/" + tournamentCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tournamentId))
                .andExpect(jsonPath("$.code").value(tournamentCode));

        mockMvc.perform(get("/api/public/tournaments/" + tournamentCode + "/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].displayName").value("Alice API"));

        mockMvc.perform(put("/api/scoring/matches/" + matchId + "/sets")
                        .with(httpBasic(REFEREE_USER, REFEREE_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "version", version,
                                "sets", new Object[] {
                                        Map.of("setNo", 1, "player1Score", 11, "player2Score", 7),
                                        Map.of("setNo", 2, "player1Score", 9, "player2Score", 11),
                                        Map.of("setNo", 3, "player1Score", 11, "player2Score", 8)
                                }
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.winnerTournamentPlayerId").value(player1.get("id").asText()))
                .andExpect(jsonPath("$.player1SetsWon").value(2))
                .andExpect(jsonPath("$.player2SetsWon").value(1))
                .andExpect(jsonPath("$.sets", hasSize(3)));

        mockMvc.perform(get("/api/public/tournaments/" + tournamentCode + "/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].courtName").value("Court 1"))
                .andExpect(jsonPath("$[0].refereeNames", hasSize(2)))
                .andExpect(jsonPath("$[0].sets", hasSize(3)));
    }

    private JsonNode postAdmin(String path, Object body) throws Exception {
        String response = mockMvc.perform(post(path)
                        .with(httpBasic(ADMIN_USER, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }
}
