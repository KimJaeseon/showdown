package com.showdown.backend.api.dto;

import com.showdown.backend.domain.AppRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class UserDtos {
    private UserDtos() {
    }

    public record UserRequest(
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotBlank String displayName,
            @NotNull AppRole role,
            Boolean active
    ) {
    }

    public record UserResponse(
            UUID id,
            String email,
            String displayName,
            AppRole role,
            Boolean active
    ) {
    }
}
