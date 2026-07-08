package com.showdown.backend.api;

import com.showdown.backend.api.dto.MatchDtos.MatchConfirmRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchResponse;
import com.showdown.backend.api.dto.MatchDtos.MatchSetsUpdateRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchSpecialFinishRequest;
import com.showdown.backend.service.TournamentAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scoring")
@Tag(name = "Scoring", description = "심판과 관리자가 사용하는 점수 입력 API")
public class ScoringController {
    private final TournamentAdminService adminService;

    public ScoringController(TournamentAdminService adminService) {
        this.adminService = adminService;
    }

    @PutMapping("/matches/{matchId}/sets")
    @Operation(summary = "세트 점수 저장 및 경기 결과 확정")
    public MatchResponse updateSets(@PathVariable UUID matchId, @Valid @RequestBody MatchSetsUpdateRequest request) {
        var match = adminService.updateMatchSets(matchId, request);
        return ApiMapper.toMatchResponse(match, adminService.getMatchSets(match.getId()));
    }

    @PutMapping("/matches/{matchId}/draft")
    @Operation(summary = "세트 점수 임시 저장")
    public MatchResponse saveDraft(@PathVariable UUID matchId, @Valid @RequestBody MatchSetsUpdateRequest request) {
        var match = adminService.saveMatchSetDraft(matchId, request);
        return ApiMapper.toMatchResponse(match, adminService.getMatchSets(match.getId()));
    }

    @PostMapping("/matches/{matchId}/confirm")
    @Operation(summary = "경기 결과 확정 및 순위 반영")
    public MatchResponse confirm(@PathVariable UUID matchId, @Valid @RequestBody MatchConfirmRequest request) {
        var match = adminService.confirmMatchResult(matchId, request.version(), request.changeReason());
        return ApiMapper.toMatchResponse(match, adminService.getMatchSets(match.getId()));
    }

    @PostMapping("/matches/{matchId}/finish-special")
    @Operation(summary = "기권승, 몰수패 또는 BYE로 경기 종료")
    public MatchResponse finishSpecial(@PathVariable UUID matchId, @Valid @RequestBody MatchSpecialFinishRequest request) {
        var match = adminService.finishMatchSpecial(matchId, request.version(), request.reason(), request.winnerSide(), request.note());
        return ApiMapper.toMatchResponse(match, adminService.getMatchSets(match.getId()));
    }
}
