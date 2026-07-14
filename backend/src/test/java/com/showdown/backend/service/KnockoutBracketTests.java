package com.showdown.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.showdown.backend.api.dto.DivisionDtos.DivisionRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchSetRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchSetsUpdateRequest;
import com.showdown.backend.api.dto.OfficialDtos.OfficialRequest;
import com.showdown.backend.api.dto.PlayerDtos.TournamentPlayerRequest;
import com.showdown.backend.api.dto.ScheduleDtos.KnockoutGenerateRequest;
import com.showdown.backend.api.dto.ScheduleDtos.ScheduleGenerateResponse;
import com.showdown.backend.api.dto.StageDtos.StageRequest;
import com.showdown.backend.api.dto.TournamentDtos.TournamentRequest;
import com.showdown.backend.domain.Division;
import com.showdown.backend.domain.DivisionCategory;
import com.showdown.backend.domain.Match;
import com.showdown.backend.domain.Official;
import com.showdown.backend.domain.Stage;
import com.showdown.backend.domain.StageType;
import com.showdown.backend.domain.Tournament;
import com.showdown.backend.domain.TournamentPlayer;
import com.showdown.backend.domain.TournamentStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * TDD-23: 참가자 수가 2의 거듭제곱이 아닌 토너먼트는 상위 시드가 부전승을 받아 1라운드를
 * 건너뛰고, 다음 라운드는 이전 라운드 경기의 승자를 자동으로 이어받는지 검증한다.
 */
@SpringBootTest
class KnockoutBracketTests {

    @Autowired
    private TournamentAdminService adminService;

    @Autowired
    private ScheduleService scheduleService;

    @Test
    @WithMockUser(authorities = "ROLE_SYSTEM_ADMIN")
    void fiveEntrantBracketGivesTopSeedsByesAndAdvancesWinnersAutomatically() {
        String suffix = Long.toString(System.nanoTime());
        Tournament tournament = adminService.createTournament(new TournamentRequest(
                "tdd23-" + suffix, "TDD-23 Open " + suffix, "Seoul",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), "Asia/Seoul", TournamentStatus.PUBLISHED, "ko"));
        Division division = adminService.createDivision(tournament.getId(),
                new DivisionRequest("Open Singles", "OPEN", DivisionCategory.OPEN, 1, true));
        Official referee1 = adminService.createOfficial(tournament.getId(), new OfficialRequest("Ref 1", "R1", "REFEREE", true));
        Official referee2 = adminService.createOfficial(tournament.getId(), new OfficialRequest("Ref 2", "R2", "REFEREE", true));
        Stage stage = adminService.createStage(tournament.getId(),
                new StageRequest(division.getId(), "Knockout", StageType.KNOCKOUT, 1, null));

        TournamentPlayer seed1 = createPlayer(tournament, division, "Seed1", 1, 1);
        TournamentPlayer seed2 = createPlayer(tournament, division, "Seed2", 2, 2);
        TournamentPlayer seed3 = createPlayer(tournament, division, "Seed3", 3, 3);
        TournamentPlayer seed4 = createPlayer(tournament, division, "Seed4", 4, 4);
        TournamentPlayer seed5 = createPlayer(tournament, division, "Seed5", 5, 5);

        ScheduleGenerateResponse response = scheduleService.generateKnockout(tournament.getId(), new KnockoutGenerateRequest(
                division.getId(), stage.getId(), 5,
                List.of("Court 1"), OffsetDateTime.parse("2026-07-01T09:00:00+09:00"), 45, 1,
                List.of(seed5.getId(), seed3.getId(), seed1.getId(), seed4.getId(), seed2.getId()),
                List.of(referee1.getId(), referee2.getId())));

        // 8강 브래킷(참가자 5명) = 부전승 3명 + 1라운드 1경기 + 2라운드 2경기 + 결승 1경기 = 4경기, 3라운드.
        assertThat(response.matchCount()).isEqualTo(4);
        assertThat(response.groupCount()).isEqualTo(3);

        List<Match> matches = response.matchIds().stream()
                .map(adminService::getMatch)
                .sorted(Comparator.comparing(Match::getMatchNo))
                .toList();
        Match round1Match = matches.get(0);
        Match round2MatchA = matches.get(1);
        Match round2MatchB = matches.get(2);
        Match finalMatch = matches.get(3);

        // 1라운드: 시드 4번과 5번만 경기한다 (1,2,3번은 부전승).
        assertThat(round1Match.getPlayer1().getId()).isEqualTo(seed4.getId());
        assertThat(round1Match.getPlayer2().getId()).isEqualTo(seed5.getId());

        // 2라운드 두 경기 중 하나는 부전승 선수끼리, 다른 하나는 부전승 선수 대 1라운드 승자(미정) 대진이다.
        assertThat(round2MatchA.getPlayer1().getId()).isEqualTo(seed1.getId());
        assertThat(round2MatchA.getPlayer2().getId()).isEqualTo(seed2.getId());
        assertThat(round2MatchB.getPlayer1().getId()).isEqualTo(seed3.getId());
        assertThat(round2MatchB.getPlayer2()).isNull();
        assertThat(round2MatchB.getPlayer2SourceMatch().getId()).isEqualTo(round1Match.getId());

        // 결승은 두 2라운드 경기 모두 승자 미정 상태로 시작한다.
        assertThat(finalMatch.getPlayer1()).isNull();
        assertThat(finalMatch.getPlayer2()).isNull();
        assertThat(finalMatch.getPlayer1SourceMatch().getId()).isEqualTo(round2MatchA.getId());
        assertThat(finalMatch.getPlayer2SourceMatch().getId()).isEqualTo(round2MatchB.getId());

        // 1라운드 경기를 시드4 승리로 확정하면, 2라운드 B 경기의 미정 슬롯에 시드4가 자동으로 채워져야 한다.
        adminService.updateMatchSets(round1Match.getId(), new MatchSetsUpdateRequest(round1Match.getVersion(), List.of(
                new MatchSetRequest(1, 11, 7),
                new MatchSetRequest(2, 11, 8)
        ), null));

        Match refreshedRound2MatchB = adminService.getMatch(round2MatchB.getId());
        assertThat(refreshedRound2MatchB.getPlayer2()).isNotNull();
        assertThat(refreshedRound2MatchB.getPlayer2().getId()).isEqualTo(seed4.getId());
        assertThat(refreshedRound2MatchB.getPlayer2SourceMatch()).isNull();
    }

    private TournamentPlayer createPlayer(Tournament tournament, Division division, String name, int seedNo, int entryNo) {
        return adminService.createTournamentPlayer(tournament.getId(),
                new TournamentPlayerRequest(name, null, null, "KOR", division.getId(), seedNo, entryNo, null, null, null));
    }
}
