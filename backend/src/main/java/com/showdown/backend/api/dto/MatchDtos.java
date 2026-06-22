package com.showdown.backend.api.dto;

import com.showdown.backend.domain.MatchSide;
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
            @Positive Integer durationMinutes,
            String refereeName,
            List<UUID> refereeOfficialIds,
            @NotNull UUID player1TournamentPlayerId,
            @NotNull UUID player2TournamentPlayerId,
            MatchStatus status
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
            Integer durationMinutes,
            String refereeName,
            List<UUID> refereeOfficialIds,
            List<String> refereeNames,
            UUID player1TournamentPlayerId,
            String player1Name,
            UUID player2TournamentPlayerId,
            String player2Name,
            UUID winnerTournamentPlayerId,
            MatchStatus status,
            Integer player1SetsWon,
            Integer player2SetsWon,
            Integer player1TotalPoints,
            Integer player2TotalPoints,
            Integer version,
            List<MatchSetResponse> sets
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
            @Valid List<MatchSetRequest> sets
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
