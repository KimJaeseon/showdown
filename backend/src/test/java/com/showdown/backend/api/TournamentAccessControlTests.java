package com.showdown.backend.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showdown.backend.api.dto.DivisionDtos.DivisionRequest;
import com.showdown.backend.api.dto.GroupDtos.GroupRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchRequest;
import com.showdown.backend.api.dto.OfficialDtos.OfficialRequest;
import com.showdown.backend.api.dto.PlayerDtos.TournamentPlayerRequest;
import com.showdown.backend.api.dto.StageDtos.StageRequest;
import com.showdown.backend.api.dto.TournamentDtos.TournamentRequest;
import com.showdown.backend.domain.AppUser;
import com.showdown.backend.domain.Division;
import com.showdown.backend.domain.DivisionCategory;
import com.showdown.backend.domain.GroupType;
import com.showdown.backend.domain.Match;
import com.showdown.backend.domain.MatchStatus;
import com.showdown.backend.domain.Official;
import com.showdown.backend.domain.Role;
import com.showdown.backend.domain.Stage;
import com.showdown.backend.domain.StageType;
import com.showdown.backend.domain.Tournament;
import com.showdown.backend.domain.TournamentGroup;
import com.showdown.backend.domain.TournamentPlayer;
import com.showdown.backend.domain.TournamentStatus;
import com.showdown.backend.repository.AppUserRepository;
import com.showdown.backend.repository.RoleRepository;
import com.showdown.backend.service.TournamentAdminService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * TDD-20: 대회 범위 권한 검증. showdown.security.mode=database로 전환해 실제 DB 기반
 * TOURNAMENT_ADMIN 계정이 자기 대회 밖의 데이터를 수정하지 못하는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "showdown.security.mode=database")
class TournamentAccessControlTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TournamentAdminService adminService;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(authorities = "ROLE_SYSTEM_ADMIN")
    void tournamentAdminCannotModifyAnotherTournamentsData() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO roles (id, code, name, description, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), "tournament_admin", "Tournament administrator", "특정 대회를 관리할 수 있는 권한");

        String suffix = Long.toString(System.nanoTime());
        Tournament tournamentA = adminService.createTournament(new TournamentRequest(
                "tac-a-" + suffix, "Tournament A " + suffix, "Seoul",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), "Asia/Seoul", TournamentStatus.PUBLISHED, "ko"));
        Tournament tournamentB = adminService.createTournament(new TournamentRequest(
                "tac-b-" + suffix, "Tournament B " + suffix, "Busan",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), "Asia/Seoul", TournamentStatus.PUBLISHED, "ko"));

        Role tournamentAdminRole = roles.findByCode("tournament_admin").orElseThrow();
        String email = "scoped-admin-" + suffix + "@example.com";
        String password = "Password1!";
        AppUser scopedAdmin = new AppUser();
        scopedAdmin.setEmail(email);
        scopedAdmin.setDisplayName("Scoped Admin");
        scopedAdmin.setPasswordHash(passwordEncoder.encode(password));
        scopedAdmin.setActive(true);
        scopedAdmin.replaceRole(tournamentAdminRole, tournamentA);
        users.save(scopedAdmin);

        Map<String, Object> divisionRequest = Map.of(
                "name", "Open", "code", "OPEN", "category", "OPEN", "sortOrder", 1, "active", true);

        // 자기 대회(A)는 수정할 수 있다.
        mockMvc.perform(post("/api/admin/tournaments/" + tournamentA.getId() + "/divisions")
                        .with(httpBasic(email, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(divisionRequest)))
                .andExpect(status().isOk());

        // 다른 대회(B)는 403이어야 한다.
        mockMvc.perform(post("/api/admin/tournaments/" + tournamentB.getId() + "/divisions")
                        .with(httpBasic(email, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(divisionRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_SYSTEM_ADMIN")
    void scorerCannotScoreUnassignedMatch() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO roles (id, code, name, description, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), "scorer", "Scorer", "경기 점수와 결과를 입력할 수 있는 권한");

        String suffix = Long.toString(System.nanoTime());
        Tournament tournament = adminService.createTournament(new TournamentRequest(
                "tdd21-" + suffix, "TDD-21 Open " + suffix, "Seoul",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), "Asia/Seoul", TournamentStatus.PUBLISHED, "ko"));
        Division division = adminService.createDivision(tournament.getId(),
                new DivisionRequest("Open Singles", "OPEN", DivisionCategory.OPEN, 1, true));
        TournamentPlayer player1 = adminService.createTournamentPlayer(tournament.getId(),
                new TournamentPlayerRequest("Alice", null, null, "KOR", division.getId(), 1, 1, null, null, null));
        TournamentPlayer player2 = adminService.createTournamentPlayer(tournament.getId(),
                new TournamentPlayerRequest("Bob", null, null, "USA", division.getId(), 2, 2, null, null, null));
        Official assignedReferee1 = adminService.createOfficial(tournament.getId(),
                new OfficialRequest("Assigned Ref 1", "AR1", "REFEREE", true));
        Official assignedReferee2 = adminService.createOfficial(tournament.getId(),
                new OfficialRequest("Assigned Ref 2", "AR2", "REFEREE", true));
        Official unassignedReferee = adminService.createOfficial(tournament.getId(),
                new OfficialRequest("Unassigned Ref", "UR1", "REFEREE", true));
        Stage stage = adminService.createStage(tournament.getId(),
                new StageRequest(division.getId(), "Preliminary", StageType.ROUND_ROBIN, 1, null));
        TournamentGroup group = adminService.createGroup(tournament.getId(),
                new GroupRequest(division.getId(), stage.getId(), "A", "Group A", GroupType.LEAGUE, 1));

        Match match = adminService.createMatch(tournament.getId(), new MatchRequest(
                division.getId(), stage.getId(), group.getId(), 1,
                java.time.OffsetDateTime.parse("2026-07-01T10:00:00+09:00"), "Court 1", null, 30, 3, null,
                List.of(assignedReferee1.getId(), assignedReferee2.getId()),
                player1.getId(), player2.getId(), MatchStatus.SCHEDULED, null, null));

        String assignedEmail = "assigned-scorer-" + suffix + "@example.com";
        AppUser assignedUser = new AppUser();
        assignedUser.setEmail(assignedEmail);
        assignedUser.setDisplayName("Assigned Scorer");
        assignedUser.setPasswordHash(passwordEncoder.encode("Password1!"));
        assignedUser.setActive(true);
        assignedUser.setOfficial(assignedReferee1);
        assignedUser.replaceGlobalRole(roles.findByCode("scorer").orElseThrow());
        users.save(assignedUser);

        String unassignedEmail = "unassigned-scorer-" + suffix + "@example.com";
        AppUser unassignedUser = new AppUser();
        unassignedUser.setEmail(unassignedEmail);
        unassignedUser.setDisplayName("Unassigned Scorer");
        unassignedUser.setPasswordHash(passwordEncoder.encode("Password1!"));
        unassignedUser.setActive(true);
        unassignedUser.setOfficial(unassignedReferee);
        unassignedUser.replaceGlobalRole(roles.findByCode("scorer").orElseThrow());
        users.save(unassignedUser);

        Map<String, Object> draftRequest = Map.of(
                "version", match.getVersion(),
                "sets", List.of(Map.of("setNo", 1, "player1Score", 11, "player2Score", 7)));

        // 배정된 심판은 점수를 저장할 수 있다.
        mockMvc.perform(put("/api/scoring/matches/" + match.getId() + "/draft")
                        .with(httpBasic(assignedEmail, "Password1!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftRequest)))
                .andExpect(status().isOk());

        // 배정되지 않은 심판은 403이어야 한다.
        mockMvc.perform(put("/api/scoring/matches/" + match.getId() + "/draft")
                        .with(httpBasic(unassignedEmail, "Password1!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
