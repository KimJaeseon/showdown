package com.showdown.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.showdown.backend.api.dto.DivisionDtos.DivisionRequest;
import com.showdown.backend.api.dto.GroupDtos.GroupRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchSetsUpdateRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchSetRequest;
import com.showdown.backend.api.dto.OfficialDtos.OfficialRequest;
import com.showdown.backend.api.dto.PlayerDtos.TournamentPlayerRequest;
import com.showdown.backend.api.dto.StageDtos.StageRequest;
import com.showdown.backend.api.dto.TournamentDtos.TournamentRequest;
import com.showdown.backend.domain.Division;
import com.showdown.backend.domain.DivisionCategory;
import com.showdown.backend.domain.GroupType;
import com.showdown.backend.domain.Match;
import com.showdown.backend.domain.MatchStatus;
import com.showdown.backend.domain.Official;
import com.showdown.backend.domain.RankingSnapshot;
import com.showdown.backend.domain.Stage;
import com.showdown.backend.domain.StageType;
import com.showdown.backend.domain.Tournament;
import com.showdown.backend.domain.TournamentGroup;
import com.showdown.backend.domain.TournamentPlayer;
import com.showdown.backend.domain.TournamentStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * TDD-25: 순위 계산이 대회별 ranking_rules 설정(승점/패점/기권승점)을 실제로 반영하는지 검증한다.
 */
@SpringBootTest
class RankingRuleTests {

    @Autowired
    private TournamentAdminService adminService;

    @Autowired
    private RankingService rankingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(authorities = "ROLE_SYSTEM_ADMIN")
    void rankingUsesTournamentSpecificWinAndLossPoints() {
        String suffix = Long.toString(System.nanoTime());
        Tournament tournament = adminService.createTournament(new TournamentRequest(
                "tdd25-" + suffix, "TDD-25 Open " + suffix, "Seoul",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), "Asia/Seoul", TournamentStatus.PUBLISHED, "ko"));

        // 이 대회는 승점 3점, 패점 1점 규칙을 사용한다 (기본값 1/0과 다름).
        jdbcTemplate.update(
                "INSERT INTO ranking_rules (id, tournament_id, win_points, loss_points, walkover_win_points, is_default) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), tournament.getId(), 3, 1, 3, true);

        Division division = adminService.createDivision(tournament.getId(),
                new DivisionRequest("Open Singles", "OPEN", DivisionCategory.OPEN, 1, true));
        TournamentPlayer player1 = adminService.createTournamentPlayer(tournament.getId(),
                new TournamentPlayerRequest("Alice", null, null, "KOR", division.getId(), 1, 1, null, null, null));
        TournamentPlayer player2 = adminService.createTournamentPlayer(tournament.getId(),
                new TournamentPlayerRequest("Bob", null, null, "USA", division.getId(), 2, 2, null, null, null));
        Official referee1 = adminService.createOfficial(tournament.getId(),
                new OfficialRequest("Ref 1", "R1", "REFEREE", true));
        Official referee2 = adminService.createOfficial(tournament.getId(),
                new OfficialRequest("Ref 2", "R2", "REFEREE", true));
        Stage stage = adminService.createStage(tournament.getId(),
                new StageRequest(division.getId(), "Preliminary", StageType.ROUND_ROBIN, 1, null));
        TournamentGroup group = adminService.createGroup(tournament.getId(),
                new GroupRequest(division.getId(), stage.getId(), "A", "Group A", GroupType.LEAGUE, 1));

        Match match = adminService.createMatch(tournament.getId(), new MatchRequest(
                division.getId(), stage.getId(), group.getId(), 1,
                OffsetDateTime.parse("2026-07-01T10:00:00+09:00"), "Court 1", null, 30, 3, null,
                List.of(referee1.getId(), referee2.getId()),
                player1.getId(), player2.getId(), MatchStatus.SCHEDULED));

        adminService.updateMatchSets(match.getId(), new MatchSetsUpdateRequest(match.getVersion(), List.of(
                new MatchSetRequest(1, 11, 7),
                new MatchSetRequest(2, 11, 8)
        ), null));

        List<RankingSnapshot> rankings = rankingService.findByTournament(tournament.getId());
        RankingSnapshot winner = rankings.stream()
                .filter(r -> r.getTournamentPlayer().getId().equals(player1.getId())).findFirst().orElseThrow();
        RankingSnapshot loser = rankings.stream()
                .filter(r -> r.getTournamentPlayer().getId().equals(player2.getId())).findFirst().orElseThrow();

        assertThat(winner.getMatchPoints()).isEqualTo(3);
        assertThat(loser.getMatchPoints()).isEqualTo(1);
    }
}
