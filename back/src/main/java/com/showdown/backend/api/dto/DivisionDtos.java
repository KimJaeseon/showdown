package com.showdown.backend.api.dto;

import com.showdown.backend.domain.DivisionCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class DivisionDtos {
    private DivisionDtos() {
    }

    public record DivisionRequest(
            @NotBlank String name,
            @NotBlank String code,
            @NotNull DivisionCategory category,
            Integer sortOrder,
            Boolean active
    ) {
    }

    public record DivisionResponse(
            UUID id,
            UUID tournamentId,
            String name,
            String code,
            DivisionCategory category,
            Integer sortOrder,
            Boolean active
    ) {
    }
}
