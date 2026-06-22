package com.showdown.backend.api;

import com.showdown.backend.api.dto.DivisionDtos.DivisionResponse;
import com.showdown.backend.api.dto.GroupDtos.GroupResponse;
import com.showdown.backend.api.dto.MatchDtos.MatchResponse;
import com.showdown.backend.api.dto.PlayerDtos.TournamentPlayerResponse;
import com.showdown.backend.api.dto.TournamentDtos.TournamentResponse;
import com.showdown.backend.domain.MatchStatus;
import com.showdown.backend.service.PublicQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/tournaments")
@Tag(name = "Public Tournament", description = "로그인 없이 접근 가능한 대회 공개 조회 API")
public class PublicTournamentController {
    private final PublicQueryService publicQueryService;

    public PublicTournamentController(PublicQueryService publicQueryService) {
        this.publicQueryService = publicQueryService;
    }

    @GetMapping
    @Operation(summary = "공개 대회 목록 조회")
    public List<TournamentResponse> tournaments() {
        return publicQueryService.findTournaments().stream().map(ApiMapper::toTournamentResponse).toList();
    }

    @GetMapping("/{tournamentCode}")
    @Operation(summary = "대회 상세 조회")
    public TournamentResponse tournament(@PathVariable String tournamentCode) {
        return ApiMapper.toTournamentResponse(publicQueryService.getTournamentByCode(tournamentCode));
    }

    @GetMapping("/{tournamentCode}/divisions")
    @Operation(summary = "부문 목록 조회")
    public List<DivisionResponse> divisions(@PathVariable String tournamentCode) {
        return publicQueryService.getDivisions(tournamentCode).stream().map(ApiMapper::toDivisionResponse).toList();
    }

    @GetMapping("/{tournamentCode}/players")
    @Operation(summary = "선수 목록 조회")
    public List<TournamentPlayerResponse> players(@PathVariable String tournamentCode) {
        return publicQueryService.getPlayers(tournamentCode).stream().map(ApiMapper::toTournamentPlayerResponse).toList();
    }

    @GetMapping("/{tournamentCode}/groups")
    @Operation(summary = "조 목록 조회")
    public List<GroupResponse> groups(@PathVariable String tournamentCode) {
        return publicQueryService.getGroups(tournamentCode).stream().map(ApiMapper::toGroupResponse).toList();
    }

    @GetMapping("/{tournamentCode}/matches")
    @Operation(summary = "경기 일정 조회")
    public List<MatchResponse> matches(
            @PathVariable String tournamentCode,
            @RequestParam(required = false) MatchStatus status
    ) {
        return publicQueryService.getMatches(tournamentCode, status).stream()
                .map(match -> ApiMapper.toMatchResponse(match, publicQueryService.getMatchSets(match.getId())))
                .toList();
    }
}
