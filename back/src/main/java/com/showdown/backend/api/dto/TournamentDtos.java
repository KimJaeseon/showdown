package com.showdown.backend.api.dto;

import com.showdown.backend.domain.TournamentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public final class TournamentDtos {
    private TournamentDtos() {
    }

    public record TournamentRequest(
            @NotBlank String code,
            @NotBlank String name,
            String location,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotBlank String timezone,
            @NotNull TournamentStatus status,
            @NotBlank String defaultLanguage
    ) {
    }

    public record TournamentResponse(
            UUID id,
            String code,
            String name,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            String timezone,
            TournamentStatus status,
            String defaultLanguage
    ) {
    }
}
