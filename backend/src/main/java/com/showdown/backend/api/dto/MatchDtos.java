package com.showdown.backend.api.dto;

import com.showdown.backend.domain.MatchSide;
import com.showdown.backend.domain.MatchEndReason;
import com.showdown.backend.domain.MatchStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class MatchDtos {
    private MatchDtos() {
    }

    public record MatchRequest(
            @NotNull UUID divisionId,
            @NotNull UUID stageId,
            UUID groupId,
            @Positive Integer matchNo,
            OffsetDateTime scheduledAt,
            String courtName,
            UUID courtId,
            @Positive Integer durationMinutes,
            Integer maxSets,
            String refereeName,
            List<UUID> refereeOfficialIds,
            UUID player1TournamentPlayerId,
            UUID player2TournamentPlayerId,
            MatchStatus status,
            UUID player1SourceMatchId,
            UUID player2SourceMatchId
    ) {
    }

    public record MatchResponse(
            UUID id,
            UUID tournamentId,
            UUID divisionId,
            UUID stageId,
            UUID groupId,
            Integer matchNo,
            OffsetDateTime scheduledAt,
            String courtName,
            UUID courtId,
            Integer durationMinutes,
            Integer maxSets,
            String refereeName,
            List<UUID> refereeOfficialIds,
            List<String> refereeNames,
            UUID player1TournamentPlayerId,
            String player1Name,
            UUID player2TournamentPlayerId,
            String player2Name,
            UUID winnerTournamentPlayerId,
            MatchStatus status,
            MatchEndReason endReason,
            String resultNote,
            Integer player1SetsWon,
            Integer player2SetsWon,
            Integer player1TotalPoints,
            Integer player2TotalPoints,
            Integer version,
            List<MatchSetResponse> sets,
            UUID player1SourceMatchId,
            UUID player2SourceMatchId
    ) {
    }

    public record MatchSetRequest(
            @Positive Integer setNo,
            @NotNull Integer player1Score,
            @NotNull Integer player2Score
    ) {
    }

    public record MatchSetsUpdateRequest(
            @NotNull Integer version,
            @Valid List<MatchSetRequest> sets,
            String changeReason
    ) {
    }

    public record MatchConfirmRequest(
            @NotNull Integer version,
            String changeReason
    ) {
    }

    public record MatchSpecialFinishRequest(
            @NotNull Integer version,
            @NotNull MatchEndReason reason,
            @NotNull MatchSide winnerSide,
            String note
    ) {
    }

    public record MatchSetResponse(
            UUID id,
            Integer setNo,
            Integer player1Score,
            Integer player2Score,
            MatchSide winnerSide
    ) {
    }
}
