package com.showdown.backend.api;

import com.showdown.backend.api.dto.MatchDtos.MatchResponse;
import com.showdown.backend.api.dto.PlayerPortalDtos.PlayerPortalResponse;
import com.showdown.backend.domain.MatchStatus;
import com.showdown.backend.service.PlayerPortalService;
import com.showdown.backend.service.PublicQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/player")
@Tag(name = "Player Portal", description = "선수 개인 포털 조회 API")
public class PlayerPortalController {
    private final PublicQueryService publicQueryService;
    private final PlayerPortalService playerPortalService;

    public PlayerPortalController(PublicQueryService publicQueryService, PlayerPortalService playerPortalService) {
        this.publicQueryService = publicQueryService;
        this.playerPortalService = playerPortalService;
    }

    @GetMapping("/me")
    @Operation(summary = "로그인 선수 개인 포털 조회")
    public PlayerPortalResponse me(Principal principal) {
        return playerPortalService.getPortal(principal.getName());
    }

    @GetMapping("/tournaments/{tournamentCode}/scheduled-matches")
    @Operation(summary = "대회 예정 경기 조회")
    public List<MatchResponse> scheduledMatches(@PathVariable String tournamentCode) {
        return publicQueryService.getMatches(tournamentCode, MatchStatus.SCHEDULED).stream()
                .map(match -> ApiMapper.toMatchResponse(match, publicQueryService.getMatchSets(match.getId())))
                .toList();
    }
}
