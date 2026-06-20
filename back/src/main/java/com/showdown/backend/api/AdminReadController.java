package com.showdown.backend.api;

import com.showdown.backend.api.dto.DivisionDtos.DivisionResponse;
import com.showdown.backend.api.dto.GroupDtos.GroupResponse;
import com.showdown.backend.api.dto.MatchDtos.MatchResponse;
import com.showdown.backend.api.dto.OfficialDtos.OfficialResponse;
import com.showdown.backend.api.dto.PlayerDtos.TournamentPlayerResponse;
import com.showdown.backend.api.dto.StageDtos.StageResponse;
import com.showdown.backend.api.dto.TournamentDtos.TournamentResponse;
import com.showdown.backend.repository.DivisionRepository;
import com.showdown.backend.repository.GroupRepository;
import com.showdown.backend.repository.MatchRepository;
import com.showdown.backend.repository.MatchSetRepository;
import com.showdown.backend.repository.OfficialRepository;
import com.showdown.backend.repository.StageRepository;
import com.showdown.backend.repository.TournamentPlayerRepository;
import com.showdown.backend.repository.TournamentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Read", description = "관리자 전용 조회 API")
public class AdminReadController {
    private final TournamentRepository tournaments;
    private final DivisionRepository divisions;
    private final TournamentPlayerRepository tournamentPlayers;
    private final OfficialRepository officials;
    private final StageRepository stages;
    private final GroupRepository groups;
    private final MatchRepository matches;
    private final MatchSetRepository matchSets;

    public AdminReadController(
            TournamentRepository tournaments,
            DivisionRepository divisions,
            TournamentPlayerRepository tournamentPlayers,
            OfficialRepository officials,
            StageRepository stages,
            GroupRepository groups,
            MatchRepository matches,
            MatchSetRepository matchSets
    ) {
        this.tournaments = tournaments;
        this.divisions = divisions;
        this.tournamentPlayers = tournamentPlayers;
        this.officials = officials;
        this.stages = stages;
        this.groups = groups;
        this.matches = matches;
        this.matchSets = matchSets;
    }

    @GetMapping("/tournaments")
    @Operation(summary = "관리자 대회 목록 조회")
    public List<TournamentResponse> tournaments() {
        return tournaments.findAll().stream().map(ApiMapper::toTournamentResponse).toList();
    }

    @GetMapping("/tournaments/{tournamentId}/divisions")
    @Operation(summary = "관리자 부문 목록 조회")
    public List<DivisionResponse> divisions(@PathVariable UUID tournamentId) {
        return divisions.findByTournamentIdOrderBySortOrderAsc(tournamentId).stream()
                .map(ApiMapper::toDivisionResponse)
                .toList();
    }

    @GetMapping("/tournaments/{tournamentId}/players")
    @Operation(summary = "관리자 참가 선수 목록 조회")
    public List<TournamentPlayerResponse> players(@PathVariable UUID tournamentId) {
        return tournamentPlayers.findByTournamentIdOrderByDivisionSortOrderAscEntryNoAsc(tournamentId).stream()
                .map(ApiMapper::toTournamentPlayerResponse)
                .toList();
    }

    @GetMapping("/tournaments/{tournamentId}/officials")
    @Operation(summary = "관리자 심판 목록 조회")
    public List<OfficialResponse> officials(@PathVariable UUID tournamentId) {
        return officials.findByTournamentIdOrderByNameAsc(tournamentId).stream()
                .map(ApiMapper::toOfficialResponse)
                .toList();
    }

    @GetMapping("/tournaments/{tournamentId}/stages")
    @Operation(summary = "관리자 단계 목록 조회")
    public List<StageResponse> stages(@PathVariable UUID tournamentId) {
        return stages.findByTournamentIdOrderByDivisionSortOrderAscSortOrderAsc(tournamentId).stream()
                .map(ApiMapper::toStageResponse)
                .toList();
    }

    @GetMapping("/tournaments/{tournamentId}/groups")
    @Operation(summary = "관리자 조 목록 조회")
    public List<GroupResponse> groups(@PathVariable UUID tournamentId) {
        return groups.findByTournamentIdOrderByDivisionSortOrderAscSortOrderAsc(tournamentId).stream()
                .map(ApiMapper::toGroupResponse)
                .toList();
    }

    @GetMapping("/tournaments/{tournamentId}/matches")
    @Operation(summary = "관리자 경기 목록 조회")
    public List<MatchResponse> matches(@PathVariable UUID tournamentId) {
        return matches.findByTournamentIdOrderByScheduledAtAscMatchNoAsc(tournamentId).stream()
                .map(match -> ApiMapper.toMatchResponse(match, matchSets.findByMatchIdOrderBySetNoAsc(match.getId())))
                .toList();
    }
}
