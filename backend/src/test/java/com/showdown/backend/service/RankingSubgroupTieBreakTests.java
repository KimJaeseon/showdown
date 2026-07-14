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
 * TDD-26: 3인 이상 동률은 동률 선수 사이의 경기만으로 부분표를 재계산해야 한다.
 *
 * 5명 라운드로빈에서 A/B/C는 종합 성적(승점/승수/세트득실/점수득실/총득점)이 완전히 동일하게
 * 설계했지만, A-B-C 상호전(부분표)에서는 A가 2승, B가 1승, C가 0승으로 확실한 우열이 있다.
 * 시드 번호는 일부러 반대 순서(C=1번, B=2번, A=3번)로 부여해, 만약 구현이 부분표 대신
 * 시드로 대충 넘어가면 순서가 C,B,A로 뒤집혀 나오게 해 실패를 분명히 드러낸다.
 */
@SpringBootTest
class RankingSubgroupTieBreakTests {

    @Autowired
    private TournamentAdminService adminService;

    @Autowired
    private RankingService rankingService;

    @Test
    @WithMockUser(authorities = "ROLE_SYSTEM_ADMIN")
    void threeWayTieIsBrokenBySubgroupRecordNotSeed() {
        String suffix = Long.toString(System.nanoTime());
        Tournament tournament = adminService.createTournament(new TournamentRequest(
                "tdd26-" + suffix, "TDD-26 Open " + suffix, "Seoul",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), "Asia/Seoul", TournamentStatus.PUBLISHED, "ko"));
        Division division = adminService.createDivision(tournament.getId(),
                new DivisionRequest("Open Singles", "OPEN", DivisionCategory.OPEN, 1, true));

        TournamentPlayer a = createPlayer(tournament, division, "A", 3, 1);
        TournamentPlayer b = createPlayer(tournament, division, "B", 2, 2);
        TournamentPlayer c = createPlayer(tournament, division, "C", 1, 3);
        TournamentPlayer d = createPlayer(tournament, division, "D", 4, 4);
        TournamentPlayer e = createPlayer(tournament, division, "E", 5, 5);

        Official referee1 = adminService.createOfficial(tournament.getId(), new OfficialRequest("Ref 1", "R1", "REFEREE", true));
        Official referee2 = adminService.createOfficial(tournament.getId(), new OfficialRequest("Ref 2", "R2", "REFEREE", true));
        Stage stage = adminService.createStage(tournament.getId(),
                new StageRequest(division.getId(), "Preliminary", StageType.ROUND_ROBIN, 1, null));
        TournamentGroup group = adminService.createGroup(tournament.getId(),
                new GroupRequest(division.getId(), stage.getId(), "A", "Group A", GroupType.LEAGUE, 1));

        int[] matchNo = {1};
        OffsetDateTime[] time = {OffsetDateTime.parse("2026-07-01T09:00:00+09:00")};

        // 부문 내 상호전: A가 B,C를 이기고 B가 C를 이긴다 (전이적 우열, A>B>C).
        playMatch(tournament, division, stage, group, referee1, referee2, matchNo, time, a, b, true);
        playMatch(tournament, division, stage, group, referee1, referee2, matchNo, time, a, c, true);
        playMatch(tournament, division, stage, group, referee1, referee2, matchNo, time, b, c, true);

        // 외부 경기: A는 D,E에 모두 패배 (부문 전적 상쇄), B는 D를 이기고 E에 패배, C는 D,E 모두 승리.
        playMatch(tournament, division, stage, group, referee1, referee2, matchNo, time, a, d, false);
        playMatch(tournament, division, stage, group, referee1, referee2, matchNo, time, a, e, false);
        playMatch(tournament, division, stage, group, referee1, referee2, matchNo, time, b, d, true);
        playMatch(tournament, division, stage, group, referee1, referee2, matchNo, time, b, e, false);
        playMatch(tournament, division, stage, group, referee1, referee2, matchNo, time, c, d, true);
        playMatch(tournament, division, stage, group, referee1, referee2, matchNo, time, c, e, true);
        playMatch(tournament, division, stage, group, referee1, referee2, matchNo, time, d, e, false);

        List<RankingSnapshot> rankings = rankingService.findByTournament(tournament.getId());

        assertThat(rankOf(rankings, e.getId())).isEqualTo(1);
        assertThat(rankOf(rankings, a.getId())).isEqualTo(2);
        assertThat(rankOf(rankings, b.getId())).isEqualTo(3);
        assertThat(rankOf(rankings, c.getId())).isEqualTo(4);
        assertThat(rankOf(rankings, d.getId())).isEqualTo(5);
    }

    private int rankOf(List<RankingSnapshot> rankings, java.util.UUID playerId) {
        return rankings.stream().filter(r -> r.getTournamentPlayer().getId().equals(playerId))
                .findFirst().orElseThrow().getRankNo();
    }

    private TournamentPlayer createPlayer(Tournament tournament, Division division, String name, int seedNo, int entryNo) {
        return adminService.createTournamentPlayer(tournament.getId(),
                new TournamentPlayerRequest(name, null, null, "KOR", division.getId(), seedNo, entryNo, null, null, null));
    }

    /** side1Wins가 true면 player1이 11대6으로 승리, false면 player2가 승리한다. */
    private void playMatch(Tournament tournament, Division division, Stage stage, TournamentGroup group,
            Official referee1, Official referee2, int[] matchNo, OffsetDateTime[] time,
            TournamentPlayer player1, TournamentPlayer player2, boolean side1Wins) {
        Match match = adminService.createMatch(tournament.getId(), new MatchRequest(
                division.getId(), stage.getId(), group.getId(), matchNo[0]++, time[0], "Court 1", null, 30, 1, null,
                List.of(referee1.getId(), referee2.getId()),
                player1.getId(), player2.getId(), MatchStatus.SCHEDULED, null, null));
        time[0] = time[0].plusMinutes(30);

        MatchSetRequest set = side1Wins ? new MatchSetRequest(1, 11, 6) : new MatchSetRequest(1, 6, 11);
        adminService.updateMatchSets(match.getId(), new MatchSetsUpdateRequest(match.getVersion(), List.of(set), null));
    }
}
