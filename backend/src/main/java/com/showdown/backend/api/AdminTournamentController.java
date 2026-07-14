package com.showdown.backend.api;

import com.showdown.backend.api.dto.CourtDtos.CourtRequest;
import com.showdown.backend.api.dto.CourtDtos.CourtResponse;
import com.showdown.backend.api.dto.DivisionDtos.DivisionRequest;
import com.showdown.backend.api.dto.DivisionDtos.DivisionResponse;
import com.showdown.backend.api.dto.GroupDtos.GroupRequest;
import com.showdown.backend.api.dto.GroupDtos.GroupResponse;
import com.showdown.backend.api.dto.MatchDtos.MatchRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchResponse;
import com.showdown.backend.api.dto.OfficialDtos.OfficialRequest;
import com.showdown.backend.api.dto.OfficialDtos.OfficialResponse;
import com.showdown.backend.api.dto.PlayerDtos.TournamentPlayerRequest;
import com.showdown.backend.api.dto.PlayerDtos.TournamentPlayerResponse;
import com.showdown.backend.api.dto.StageDtos.StageRequest;
import com.showdown.backend.api.dto.StageDtos.StageResponse;
import com.showdown.backend.api.dto.TournamentDtos.TournamentRequest;
import com.showdown.backend.api.dto.TournamentDtos.TournamentResponse;
import com.showdown.backend.service.TournamentAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin CRUD", description = "관리자 전용 대회 운영 CRUD API")
public class AdminTournamentController {
    private final TournamentAdminService adminService;

    public AdminTournamentController(TournamentAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/tournaments")
    @Operation(summary = "대회 생성")
    public ResponseEntity<TournamentResponse> createTournament(@Valid @RequestBody TournamentRequest request) {
        var response = ApiMapper.toTournamentResponse(adminService.createTournament(request));
        return ResponseEntity.created(URI.create("/api/admin/tournaments/" + response.id())).body(response);
    }

    @PutMapping("/tournaments/{tournamentId}")
    @Operation(summary = "대회 수정")
    public TournamentResponse updateTournament(@PathVariable UUID tournamentId, @Valid @RequestBody TournamentRequest request) {
        return ApiMapper.toTournamentResponse(adminService.updateTournament(tournamentId, request));
    }

    @DeleteMapping("/tournaments/{tournamentId}")
    @Operation(summary = "대회 삭제")
    public ResponseEntity<Void> deleteTournament(@PathVariable UUID tournamentId) {
        adminService.deleteTournament(tournamentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tournaments/{tournamentId}/divisions")
    @Operation(summary = "부문 생성")
    public DivisionResponse createDivision(@PathVariable UUID tournamentId, @Valid @RequestBody DivisionRequest request) {
        return ApiMapper.toDivisionResponse(adminService.createDivision(tournamentId, request));
    }

    @PutMapping("/divisions/{divisionId}")
    @Operation(summary = "부문 수정")
    public DivisionResponse updateDivision(@PathVariable UUID divisionId, @Valid @RequestBody DivisionRequest request) {
        return ApiMapper.toDivisionResponse(adminService.updateDivision(divisionId, request));
    }

    @DeleteMapping("/divisions/{divisionId}")
    @Operation(summary = "부문 삭제")
    public ResponseEntity<Void> deleteDivision(@PathVariable UUID divisionId) {
        adminService.deleteDivision(divisionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tournaments/{tournamentId}/players")
    @Operation(summary = "참가 선수 등록")
    public TournamentPlayerResponse createPlayer(@PathVariable UUID tournamentId, @Valid @RequestBody TournamentPlayerRequest request) {
        return ApiMapper.toTournamentPlayerResponse(adminService.createTournamentPlayer(tournamentId, request));
    }

    @PutMapping("/players/{tournamentPlayerId}")
    @Operation(summary = "참가 선수 수정")
    public TournamentPlayerResponse updatePlayer(@PathVariable UUID tournamentPlayerId, @Valid @RequestBody TournamentPlayerRequest request) {
        return ApiMapper.toTournamentPlayerResponse(adminService.updateTournamentPlayer(tournamentPlayerId, request));
    }

    @DeleteMapping("/players/{tournamentPlayerId}")
    @Operation(summary = "참가 선수 삭제")
    public ResponseEntity<Void> deletePlayer(@PathVariable UUID tournamentPlayerId) {
        adminService.deleteTournamentPlayer(tournamentPlayerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tournaments/{tournamentId}/officials")
    @Operation(summary = "심판 등록")
    public OfficialResponse createOfficial(@PathVariable UUID tournamentId, @Valid @RequestBody OfficialRequest request) {
        return ApiMapper.toOfficialResponse(adminService.createOfficial(tournamentId, request));
    }

    @PutMapping("/officials/{officialId}")
    @Operation(summary = "심판 수정")
    public OfficialResponse updateOfficial(@PathVariable UUID officialId, @Valid @RequestBody OfficialRequest request) {
        return ApiMapper.toOfficialResponse(adminService.updateOfficial(officialId, request));
    }

    @DeleteMapping("/officials/{officialId}")
    @Operation(summary = "심판 삭제")
    public ResponseEntity<Void> deleteOfficial(@PathVariable UUID officialId) {
        adminService.deleteOfficial(officialId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tournaments/{tournamentId}/courts")
    @Operation(summary = "코트 등록")
    public CourtResponse createCourt(@PathVariable UUID tournamentId, @Valid @RequestBody CourtRequest request) {
        return ApiMapper.toCourtResponse(adminService.createCourt(tournamentId, request));
    }

    @PutMapping("/courts/{courtId}")
    @Operation(summary = "코트 수정")
    public CourtResponse updateCourt(@PathVariable UUID courtId, @Valid @RequestBody CourtRequest request) {
        return ApiMapper.toCourtResponse(adminService.updateCourt(courtId, request));
    }

    @DeleteMapping("/courts/{courtId}")
    @Operation(summary = "코트 삭제")
    public ResponseEntity<Void> deleteCourt(@PathVariable UUID courtId) {
        adminService.deleteCourt(courtId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tournaments/{tournamentId}/stages")
    @Operation(summary = "단계 생성")
    public StageResponse createStage(@PathVariable UUID tournamentId, @Valid @RequestBody StageRequest request) {
        return ApiMapper.toStageResponse(adminService.createStage(tournamentId, request));
    }

    @PutMapping("/stages/{stageId}")
    @Operation(summary = "단계 수정")
    public StageResponse updateStage(@PathVariable UUID stageId, @Valid @RequestBody StageRequest request) {
        return ApiMapper.toStageResponse(adminService.updateStage(stageId, request));
    }

    @DeleteMapping("/stages/{stageId}")
    @Operation(summary = "단계 삭제")
    public ResponseEntity<Void> deleteStage(@PathVariable UUID stageId) {
        adminService.deleteStage(stageId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tournaments/{tournamentId}/groups")
    @Operation(summary = "조 생성")
    public GroupResponse createGroup(@PathVariable UUID tournamentId, @Valid @RequestBody GroupRequest request) {
        return ApiMapper.toGroupResponse(adminService.createGroup(tournamentId, request));
    }

    @PutMapping("/groups/{groupId}")
    @Operation(summary = "조 수정")
    public GroupResponse updateGroup(@PathVariable UUID groupId, @Valid @RequestBody GroupRequest request) {
        return ApiMapper.toGroupResponse(adminService.updateGroup(groupId, request));
    }

    @DeleteMapping("/groups/{groupId}")
    @Operation(summary = "조 삭제")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID groupId) {
        adminService.deleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tournaments/{tournamentId}/matches")
    @Operation(summary = "경기 생성")
    public MatchResponse createMatch(@PathVariable UUID tournamentId, @Valid @RequestBody MatchRequest request) {
        var match = adminService.createMatch(tournamentId, request);
        return ApiMapper.toMatchResponse(match, adminService.getMatchSets(match.getId()));
    }

    @PutMapping("/matches/{matchId}")
    @Operation(summary = "경기 수정")
    public MatchResponse updateMatch(@PathVariable UUID matchId, @Valid @RequestBody MatchRequest request) {
        var match = adminService.updateMatch(matchId, request);
        return ApiMapper.toMatchResponse(match, adminService.getMatchSets(match.getId()));
    }

    @DeleteMapping("/matches/{matchId}")
    @Operation(summary = "경기 삭제")
    public ResponseEntity<Void> deleteMatch(@PathVariable UUID matchId) {
        adminService.deleteMatch(matchId);
        return ResponseEntity.noContent().build();
    }
}
