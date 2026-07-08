package com.showdown.backend.api.dto;

import com.showdown.backend.api.dto.MatchDtos.MatchResponse;
import com.showdown.backend.api.dto.PlayerDtos.TournamentPlayerResponse;
import java.util.List;

public final class PlayerPortalDtos {
    private PlayerPortalDtos() {
    }

    public record PlayerPortalResponse(
            TournamentPlayerResponse player,
            MatchResponse nextMatch,
            List<MatchResponse> scheduledMatches,
            List<MatchResponse> completedMatches,
            PlayerStatsResponse stats
    ) {
    }

    public record PlayerStatsResponse(
            int matchesPlayed,
            int wins,
            int losses,
            int setsWon,
            int setsLost,
            int pointsFor,
            int pointsAgainst
    ) {
    }
}
