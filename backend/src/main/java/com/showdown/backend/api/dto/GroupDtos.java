package com.showdown.backend.api.dto;

import com.showdown.backend.domain.GroupType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class GroupDtos {
    private GroupDtos() {
    }

    public record GroupRequest(
            @NotNull UUID divisionId,
            @NotNull UUID stageId,
            @NotBlank String code,
            @NotBlank String name,
            @NotNull GroupType groupType,
            Integer sortOrder
    ) {
    }

    public record GroupResponse(
            UUID id,
            UUID tournamentId,
            UUID divisionId,
            UUID stageId,
            String code,
            String name,
            GroupType groupType,
            Integer sortOrder
    ) {
    }
}
