package com.showdown.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.showdown.backend.api.dto.CourtDtos.CourtRequest;
import com.showdown.backend.api.dto.DivisionDtos.DivisionRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchRequest;
import com.showdown.backend.api.dto.OfficialDtos.OfficialRequest;
import com.showdown.backend.api.dto.PlayerDtos.TournamentPlayerRequest;
import com.showdown.backend.api.dto.StageDtos.StageRequest;
import com.showdown.backend.api.dto.TournamentDtos.TournamentRequest;
import com.showdown.backend.domain.Court;
import com.showdown.backend.domain.Division;
import com.showdown.backend.domain.DivisionCategory;
import com.showdown.backend.domain.MatchStatus;
import com.showdown.backend.domain.Official;
import com.showdown.backend.domain.Stage;
import com.showdown.backend.domain.StageType;
import com.showdown.backend.domain.Tournament;
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
 * TDD-24: 코트 충돌 검사는 court_id(관계) 기준이어야 한다. 운영자가 경기마다 코트 이름 표기를
 * 다르게 입력해도(예: "Court 1" vs "코트1"), 같은 코트 엔티티를 배정했다면 충돌로 감지해야 한다.
 * 문자열 비교만으로는 이런 표기 차이를 잡아내지 못한다.
 */
@SpringBootTest
class CourtConflictTests {

    @Autowired
    private TournamentAdminService adminService;

    @Test
    @WithMockUser(authorities = "ROLE_SYSTEM_ADMIN")
    void sameCourtEntityConflictsEvenWithDifferentCourtNameText() {
        String suffix = Long.toString(System.nanoTime());
        Tournament tournament = adminService.createTournament(new TournamentRequest(
                "tdd24-" + suffix, "TDD-24 Open " + suffix, "Seoul",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), "Asia/Seoul", TournamentStatus.PUBLISHED, "ko"));
        Division division = adminService.createDivision(tournament.getId(),
                new DivisionRequest("Open Singles", "OPEN", DivisionCategory.OPEN, 1, true));
        Official referee1 = adminService.createOfficial(tournament.getId(), new OfficialRequest("Ref 1", "R1", "REFEREE", true));
        Official referee2 = adminService.createOfficial(tournament.getId(), new OfficialRequest("Ref 2", "R2", "REFEREE", true));
        Stage stage = adminService.createStage(tournament.getId(),
                new StageRequest(division.getId(), "Preliminary", StageType.ROUND_ROBIN, 1, null));
        Court court = adminService.createCourt(tournament.getId(), new CourtRequest("Court 1", 1, true));

        TournamentPlayer p1 = createPlayer(tournament, division, "P1", 1, 1);
        TournamentPlayer p2 = createPlayer(tournament, division, "P2", 2, 2);
        TournamentPlayer p3 = createPlayer(tournament, division, "P3", 3, 3);
        TournamentPlayer p4 = createPlayer(tournament, division, "P4", 4, 4);

        OffsetDateTime time = OffsetDateTime.parse("2026-07-01T09:00:00+09:00");

        adminService.createMatch(tournament.getId(), new MatchRequest(
                division.getId(), stage.getId(), null, 1, time, "Court 1", court.getId(), 30, 3, null,
                List.of(referee1.getId(), referee2.getId()), p1.getId(), p2.getId(), MatchStatus.SCHEDULED, null, null));

        // 같은 court_id를 배정하지만 courtName 표기는 다르게 입력한다.
        assertThatThrownBy(() -> adminService.createMatch(tournament.getId(), new MatchRequest(
                        division.getId(), stage.getId(), null, 2, time, "코트1", court.getId(), 30, 3, null,
                        List.of(referee1.getId(), referee2.getId()), p3.getId(), p4.getId(), MatchStatus.SCHEDULED, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("코트");
    }

    @Test
    @WithMockUser(authorities = "ROLE_SYSTEM_ADMIN")
    void inactiveCourtCannotBeAssigned() {
        String suffix = Long.toString(System.nanoTime());
        Tournament tournament = adminService.createTournament(new TournamentRequest(
                "tdd24b-" + suffix, "TDD-24b Open " + suffix, "Seoul",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), "Asia/Seoul", TournamentStatus.PUBLISHED, "ko"));
        Division division = adminService.createDivision(tournament.getId(),
                new DivisionRequest("Open Singles", "OPEN", DivisionCategory.OPEN, 1, true));
        Official referee1 = adminService.createOfficial(tournament.getId(), new OfficialRequest("Ref 1", "R1", "REFEREE", true));
        Official referee2 = adminService.createOfficial(tournament.getId(), new OfficialRequest("Ref 2", "R2", "REFEREE", true));
        Stage stage = adminService.createStage(tournament.getId(),
                new StageRequest(division.getId(), "Preliminary", StageType.ROUND_ROBIN, 1, null));
        Court inactiveCourt = adminService.createCourt(tournament.getId(), new CourtRequest("Court 2", 1, false));

        TournamentPlayer p1 = createPlayer(tournament, division, "P1", 1, 1);
        TournamentPlayer p2 = createPlayer(tournament, division, "P2", 2, 2);

        assertThatThrownBy(() -> adminService.createMatch(tournament.getId(), new MatchRequest(
                        division.getId(), stage.getId(), null, 1,
                        OffsetDateTime.parse("2026-07-01T09:00:00+09:00"), "Court 2", inactiveCourt.getId(), 30, 3, null,
                        List.of(referee1.getId(), referee2.getId()), p1.getId(), p2.getId(), MatchStatus.SCHEDULED, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비활성 코트");
    }

    private TournamentPlayer createPlayer(Tournament tournament, Division division, String name, int seedNo, int entryNo) {
        return adminService.createTournamentPlayer(tournament.getId(),
                new TournamentPlayerRequest(name, null, null, "KOR", division.getId(), seedNo, entryNo, null, null, null));
    }
}
