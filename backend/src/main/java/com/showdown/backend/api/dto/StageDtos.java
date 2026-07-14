package com.showdown.backend.api.dto;

import com.showdown.backend.domain.StageStatus;
import com.showdown.backend.domain.StageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class StageDtos {
    private StageDtos() {
    }

    public record StageRequest(
            @NotNull UUID divisionId,
            @NotBlank String name,
            @NotNull StageType stageType,
            Integer sortOrder,
            StageStatus status
    ) {
    }

    public record StageResponse(
            UUID id,
            UUID tournamentId,
            UUID divisionId,
            String name,
            StageType stageType,
            Integer sortOrder,
            StageStatus status
    ) {
    }
}
