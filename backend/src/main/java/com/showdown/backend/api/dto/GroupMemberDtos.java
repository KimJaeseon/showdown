package com.showdown.backend.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public final class GroupMemberDtos {
    private GroupMemberDtos() {}
    public record GroupMemberRequest(@NotNull UUID tournamentPlayerId, @Positive Integer slotNo) {}
    public record GroupMemberResponse(UUID id, UUID groupId, UUID tournamentPlayerId, String playerName, Integer slotNo, String sourceRule) {}
}
