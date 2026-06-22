package com.showdown.backend.api;

import com.showdown.backend.api.dto.MatchDtos.MatchResponse;
import com.showdown.backend.domain.MatchStatus;
import com.showdown.backend.service.PublicQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/player")
@Tag(name = "Player Portal", description = "선수 롤 전용 조회 API")
public class PlayerPortalController {
    private final PublicQueryService publicQueryService;

    public PlayerPortalController(PublicQueryService publicQueryService) {
        this.publicQueryService = publicQueryService;
    }

    @GetMapping("/tournaments/{tournamentCode}/scheduled-matches")
    @Operation(summary = "선수용 예정 경기 조회")
    public List<MatchResponse> scheduledMatches(@PathVariable String tournamentCode) {
        return publicQueryService.getMatches(tournamentCode, MatchStatus.SCHEDULED).stream()
                .map(match -> ApiMapper.toMatchResponse(match, publicQueryService.getMatchSets(match.getId())))
                .toList();
    }
}
