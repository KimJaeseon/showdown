package com.showdown.backend.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class RankingDtos {
    private RankingDtos() {}
    public record RankingResponse(UUID id, UUID tournamentId, UUID divisionId, UUID stageId, UUID groupId,
            UUID tournamentPlayerId, Integer rankNo, Integer matchesPlayed, Integer wins, Integer losses,
            Integer matchPoints, Integer setsWon, Integer setsLost, Integer setDifference, Integer pointsFor,
            Integer pointsAgainst, Integer pointDifference, String tieBreakNote, OffsetDateTime calculatedAt) {}
}
