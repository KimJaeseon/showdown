package com.showdown.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.showdown.backend.api.dto.DivisionDtos.DivisionRequest;
import com.showdown.backend.api.dto.GroupDtos.GroupRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchSetRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchSetsUpdateRequest;
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
import com.showdown.backend.domain.StageStatus;
import com.showdown.backend.domain.StageType;
import com.showdown.backend.domain.Tournament;
import com.showdown.backend.domain.TournamentGroup;
import com.showdown.backend.domain.TournamentPlayer;
import com.showdown.backend.domain.TournamentStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * TDD-27: 단계(Stage)가 FINISHED로 전환되면 조 구분 없는(group_id IS NULL) 단계 최종 순위 스냅샷이 생성되어야 한다.
 */
@SpringBootTest
class StageFinalRankingTests {

    @Autowired
    private TournamentAdminService adminService;

    @Autowired
    private RankingService rankingService;

    @Test
    @WithMockUser(authorities = "ROLE_SYSTEM_ADMIN")
    void finishingStageCreatesGroupWideFinalSnapshot() {
        String suffix = Long.toString(System.nanoTime());
        Tournament tournament = adminService.createTournament(new TournamentRequest(
                "tdd27-" + suffix, "TDD-27 Open " + suffix, "Seoul",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), "Asia/Seoul", TournamentStatus.PUBLISHED, "ko"));
        Division division = adminService.createDivision(tournament.getId(),
                new DivisionRequest("Open Singles", "OPEN", DivisionCategory.OPEN, 1, true));
        Official referee1 = adminService.createOfficial(tournament.getId(), new OfficialRequest("Ref 1", "R1", "REFEREE", true));
        Official referee2 = adminService.createOfficial(tournament.getId(), new OfficialRequest("Ref 2", "R2", "REFEREE", true));
        Stage stage = adminService.createStage(tournament.getId(),
                new StageRequest(division.getId(), "Preliminary", StageType.ROUND_ROBIN, 1, null));

        TournamentGroup groupA = adminService.createGroup(tournament.getId(),
                new GroupRequest(division.getId(), stage.getId(), "A", "Group A", GroupType.LEAGUE, 1));
        TournamentGroup groupB = adminService.createGroup(tournament.getId(),
                new GroupRequest(division.getId(), stage.getId(), "B", "Group B", GroupType.LEAGUE, 2));

        TournamentPlayer a1 = createPlayer(tournament, division, "A1", 1, 1);
        TournamentPlayer a2 = createPlayer(tournament, division, "A2", 2, 2);
        TournamentPlayer b1 = createPlayer(tournament, division, "B1", 3, 3);
        TournamentPlayer b2 = createPlayer(tournament, division, "B2", 4, 4);

        OffsetDateTime time = OffsetDateTime.parse("2026-07-01T09:00:00+09:00");
        playMatch(tournament, division, stage, groupA, referee1, referee2, 1, time, a1, a2);
        playMatch(tournament, division, stage, groupB, referee1, referee2, 2, time.plusMinutes(30), b1, b2);

        // 단계는 한 단계씩만 전이할 수 있다.
        adminService.updateStage(stage.getId(), new StageRequest(division.getId(), "Preliminary", StageType.ROUND_ROBIN, 1, StageStatus.PUBLISHED));
        adminService.updateStage(stage.getId(), new StageRequest(division.getId(), "Preliminary", StageType.ROUND_ROBIN, 1, StageStatus.RUNNING));
        adminService.updateStage(stage.getId(), new StageRequest(division.getId(), "Preliminary", StageType.ROUND_ROBIN, 1, StageStatus.FINISHED));

        List<RankingSnapshot> stageFinal = rankingService.findStageFinal(stage.getId());
        assertThat(stageFinal).hasSize(4);
        assertThat(stageFinal).allMatch(row -> row.getGroup() == null);
        assertThat(stageFinal).allMatch(row -> row.getStage().getId().equals(stage.getId()));
        assertThat(stageFinal.get(0).getRankNo()).isEqualTo(1);
    }

    private TournamentPlayer createPlayer(Tournament tournament, Division division, String name, int seedNo, int entryNo) {
        return adminService.createTournamentPlayer(tournament.getId(),
                new TournamentPlayerRequest(name, null, null, "KOR", division.getId(), seedNo, entryNo, null, null, null));
    }

    private void playMatch(Tournament tournament, Division division, Stage stage, TournamentGroup group,
            Official referee1, Official referee2, int matchNo, OffsetDateTime time,
            TournamentPlayer player1, TournamentPlayer player2) {
        Match match = adminService.createMatch(tournament.getId(), new MatchRequest(
                division.getId(), stage.getId(), group.getId(), matchNo, time, "Court 1", null, 30, 1, null,
                List.of(referee1.getId(), referee2.getId()),
                player1.getId(), player2.getId(), MatchStatus.SCHEDULED, null, null));
        adminService.updateMatchSets(match.getId(), new MatchSetsUpdateRequest(match.getVersion(),
                List.of(new MatchSetRequest(1, 11, 6)), null));
    }
}
