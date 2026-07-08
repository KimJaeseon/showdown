package com.showdown.backend.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class RoundRobinDtos {
    private RoundRobinDtos() {}
    public record RoundRobinRequest(@NotNull OffsetDateTime startAt, @Positive Integer matchDurationMinutes,
            @NotEmpty List<String> courtNames, @NotEmpty List<UUID> officialIds) {}
    public record RoundRobinMatchPreview(UUID player1Id, String player1Name, UUID player2Id, String player2Name) {}
    public record RoundRobinResponse(int expectedMatchCount, int createdMatchCount, List<RoundRobinMatchPreview> matches) {}
}
