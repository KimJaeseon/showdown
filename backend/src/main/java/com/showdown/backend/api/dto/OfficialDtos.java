package com.showdown.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public final class OfficialDtos {
    private OfficialDtos() {
    }

    public record OfficialRequest(
            @NotBlank String name,
            String shortCode,
            @NotBlank String roleName,
            Boolean active
    ) {
    }

    public record OfficialResponse(
            UUID id,
            UUID tournamentId,
            String name,
            String shortCode,
            String roleName,
            Boolean active
    ) {
    }
}
