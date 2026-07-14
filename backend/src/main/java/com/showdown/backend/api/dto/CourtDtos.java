package com.showdown.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public final class CourtDtos {
    private CourtDtos() {
    }

    public record CourtRequest(
            @NotBlank String name,
            Integer sortOrder,
            Boolean active
    ) {
    }

    public record CourtResponse(
            UUID id,
            UUID tournamentId,
            String name,
            Integer sortOrder,
            Boolean active
    ) {
    }
}
