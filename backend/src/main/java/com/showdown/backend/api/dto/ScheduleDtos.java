package com.showdown.backend.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ScheduleDtos {
    private ScheduleDtos() {
    }

    public record RoundRobinGenerateRequest(
            UUID divisionId,
            UUID stageId,
            int groupCount,
            List<String> courtNames,
            OffsetDateTime startAt,
            int matchDurationMinutes,
            int matchNoStart,
            List<UUID> officialIds
    ) {
    }

    public record KnockoutGenerateRequest(
            UUID divisionId,
            UUID stageId,
            int entrantCount,
            List<String> courtNames,
            OffsetDateTime startAt,
            int matchDurationMinutes,
            int matchNoStart,
            List<UUID> playerIds,
            List<UUID> officialIds
    ) {
    }

    public record ScheduleGenerateResponse(
            int groupCount,
            int matchCount,
            List<UUID> matchIds
    ) {
    }

    public record ScheduleReportResponse(
            UUID tournamentId,
            long totalMatches,
            Map<String, Long> matchesByCourt,
            long courtConflictCount,
            long officialConflictCount,
            long missingOfficialAssignmentCount
    ) {
    }
}
