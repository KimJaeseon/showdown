package com.showdown.backend.api.dto;

import com.showdown.backend.domain.ParticipantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public final class PlayerDtos {
    private PlayerDtos() {
    }

    public record TournamentPlayerRequest(
            @NotBlank String displayName,
            String familyName,
            String givenName,
            String countryCode,
            @NotNull UUID divisionId,
            @Positive Integer seedNo,
            @Positive Integer entryNo,
            String displayNameOverride,
            String clubName,
            ParticipantStatus status
    ) {
    }

    public record TournamentPlayerResponse(
            UUID id,
            UUID tournamentId,
            UUID divisionId,
            UUID playerId,
            String displayName,
            String countryCode,
            Integer seedNo,
            Integer entryNo,
            String displayNameOverride,
            String clubName,
            ParticipantStatus status
    ) {
    }
}
