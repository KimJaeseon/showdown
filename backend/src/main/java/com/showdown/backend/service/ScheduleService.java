package com.showdown.backend.service;

import com.showdown.backend.api.dto.MatchDtos.MatchRequest;
import com.showdown.backend.api.dto.ScheduleDtos.KnockoutGenerateRequest;
import com.showdown.backend.api.dto.ScheduleDtos.RoundRobinGenerateRequest;
import com.showdown.backend.api.dto.ScheduleDtos.ScheduleGenerateResponse;
import com.showdown.backend.api.dto.ScheduleDtos.ScheduleReportResponse;
import com.showdown.backend.domain.Match;
import com.showdown.backend.domain.Official;
import com.showdown.backend.domain.Stage;
import com.showdown.backend.domain.TournamentGroup;
import com.showdown.backend.domain.TournamentPlayer;
import com.showdown.backend.repository.MatchRepository;
import com.showdown.backend.repository.OfficialRepository;
import com.showdown.backend.repository.StageRepository;
import com.showdown.backend.repository.TournamentPlayerRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ScheduleService {
    private final TournamentAdminService adminService;
    private final TournamentPlayerRepository tournamentPlayers;
    private final OfficialRepository officials;
    private final StageRepository stages;
    private final MatchRepository matches;

    public ScheduleService(
            TournamentAdminService adminService,
            TournamentPlayerRepository tournamentPlayers,
            OfficialRepository officials,
            StageRepository stages,
            MatchRepository matches
    ) {
        this.adminService = adminService;
        this.tournamentPlayers = tournamentPlayers;
        this.officials = officials;
        this.stages = stages;
        this.matches = matches;
    }

    public ScheduleGenerateResponse generateRoundRobin(UUID tournamentId, RoundRobinGenerateRequest request) {
        validateGenerationRequest(request.groupCount(), request.courtNames(), request.matchDurationMinutes(), request.officialIds());
        Stage stage = stages.findById(request.stageId()).orElseThrow(() -> new EntityNotFoundException("단계를 찾을 수 없습니다."));
        List<TournamentPlayer> players = tournamentPlayers.findByTournamentIdOrderByDivisionSortOrderAscEntryNoAsc(tournamentId).stream()
                .filter(player -> player.getDivision().getId().equals(request.divisionId()))
                .toList();
        List<List<TournamentPlayer>> groupedPlayers = splitPlayers(players, request.groupCount());
        List<TournamentGroup> groups = createMissingGroups(tournamentId, stage, groupedPlayers.size());

        List<UUID> createdMatchIds = new ArrayList<>();
        int matchNo = request.matchNoStart();
        int sequence = 0;
        for (int groupIndex = 0; groupIndex < groupedPlayers.size(); groupIndex++) {
            List<TournamentPlayer> groupPlayers = groupedPlayers.get(groupIndex);
            for (int i = 0; i < groupPlayers.size(); i++) {
                for (int j = i + 1; j < groupPlayers.size(); j++) {
                    String court = request.courtNames().get(sequence % request.courtNames().size());
                    OffsetDateTime scheduledAt = request.startAt().plusMinutes((long) request.matchDurationMinutes() * (sequence / request.courtNames().size()));
                    List<UUID> refereeIds = rotatingOfficials(request.officialIds(), sequence);
                    Match match = adminService.createMatch(tournamentId, new MatchRequest(
                            request.divisionId(),
                            request.stageId(),
                            groups.get(groupIndex).getId(),
                            matchNo++,
                            scheduledAt,
                            court,
                            null,
                            request.matchDurationMinutes(),
                            3,
                            null,
                            refereeIds,
                            groupPlayers.get(i).getId(),
                            groupPlayers.get(j).getId(),
                            null,
                            null,
                            null
                    ));
                    createdMatchIds.add(match.getId());
                    sequence++;
                }
            }
        }
        return new ScheduleGenerateResponse(groups.size(), createdMatchIds.size(), createdMatchIds);
    }

    /**
     * TDD-23: 단일 엘리미네이션 토너먼트 전체 라운드를 생성한다.
     * 참가자 수가 2의 거듭제곱이 아니면 상위 시드부터 부전승을 받아 1라운드를 건너뛰고,
     * 이후 라운드는 이전 라운드 경기의 승자를 자동으로 이어받는 미정(TBD) 슬롯으로 생성한다.
     */
    public ScheduleGenerateResponse generateKnockout(UUID tournamentId, KnockoutGenerateRequest request) {
        validateGenerationRequest(1, request.courtNames(), request.matchDurationMinutes(), request.officialIds());
        List<UUID> entrantIds = request.playerIds() == null || request.playerIds().isEmpty()
                ? tournamentPlayers.findByTournamentIdOrderByDivisionSortOrderAscEntryNoAsc(tournamentId).stream()
                        .filter(player -> player.getDivision().getId().equals(request.divisionId()))
                        .limit(request.entrantCount())
                        .map(TournamentPlayer::getId)
                        .toList()
                : request.playerIds();
        if (entrantIds.size() != request.entrantCount() || entrantIds.size() < 2) {
            throw new IllegalArgumentException("토너먼트 참가자는 2명 이상이며 entrantCount와 일치해야 합니다.");
        }

        List<TournamentPlayer> sortedEntrants = entrantIds.stream()
                .map(adminService::getTournamentPlayer)
                .sorted(Comparator.comparingInt(player -> player.getSeedNo() == null ? Integer.MAX_VALUE : player.getSeedNo()))
                .toList();
        int bracketSize = Integer.highestOneBit(sortedEntrants.size() - 1) * 2;
        int byeCount = bracketSize - sortedEntrants.size();
        List<TournamentPlayer> byeRecipients = sortedEntrants.subList(0, byeCount);
        List<TournamentPlayer> playing = sortedEntrants.subList(byeCount, sortedEntrants.size());

        List<UUID> createdMatchIds = new ArrayList<>();
        int[] matchNo = {request.matchNoStart()};
        int[] globalSequence = {0};
        OffsetDateTime roundStart = request.startAt();

        List<Slot> nextRoundSlots = new ArrayList<>();
        for (TournamentPlayer bye : byeRecipients) {
            nextRoundSlots.add(Slot.ofPlayer(bye));
        }
        int withinRound = 0;
        for (int i = 0; i < playing.size(); i += 2) {
            Match match = createBracketMatch(tournamentId, request, matchNo, globalSequence, withinRound++, roundStart,
                    playing.get(i).getId(), null, playing.get(i + 1).getId(), null);
            createdMatchIds.add(match.getId());
            nextRoundSlots.add(Slot.ofMatch(match));
        }

        List<Slot> currentSlots = nextRoundSlots;
        int roundCount = currentSlots.isEmpty() ? 0 : 1;
        int matchesInRound = playing.size() / 2;
        int courtCount = request.courtNames().size();
        while (currentSlots.size() > 1) {
            int waves = matchesInRound == 0 ? 0 : (matchesInRound + courtCount - 1) / courtCount;
            roundStart = roundStart.plusMinutes((long) request.matchDurationMinutes() * waves);
            List<Slot> next = new ArrayList<>();
            withinRound = 0;
            for (int i = 0; i < currentSlots.size(); i += 2) {
                Slot left = currentSlots.get(i);
                Slot right = currentSlots.get(i + 1);
                Match match = createBracketMatch(tournamentId, request, matchNo, globalSequence, withinRound++, roundStart,
                        left.player == null ? null : left.player.getId(),
                        left.sourceMatch == null ? null : left.sourceMatch.getId(),
                        right.player == null ? null : right.player.getId(),
                        right.sourceMatch == null ? null : right.sourceMatch.getId());
                createdMatchIds.add(match.getId());
                next.add(Slot.ofMatch(match));
            }
            matchesInRound = currentSlots.size() / 2;
            currentSlots = next;
            roundCount++;
        }
        return new ScheduleGenerateResponse(roundCount, createdMatchIds.size(), createdMatchIds);
    }

    private Match createBracketMatch(UUID tournamentId, KnockoutGenerateRequest request, int[] matchNo, int[] globalSequence,
            int withinRoundIndex, OffsetDateTime roundStart, UUID player1Id, UUID player1SourceMatchId, UUID player2Id, UUID player2SourceMatchId) {
        String court = request.courtNames().get(withinRoundIndex % request.courtNames().size());
        OffsetDateTime matchTime = roundStart.plusMinutes((long) request.matchDurationMinutes() * (withinRoundIndex / request.courtNames().size()));
        List<UUID> refereeIds = rotatingOfficials(request.officialIds(), globalSequence[0]);
        globalSequence[0]++;
        return adminService.createMatch(tournamentId, new MatchRequest(
                request.divisionId(),
                request.stageId(),
                null,
                matchNo[0]++,
                matchTime,
                court,
                null,
                request.matchDurationMinutes(),
                3,
                null,
                refereeIds,
                player1Id,
                player2Id,
                null,
                player1SourceMatchId,
                player2SourceMatchId
        ));
    }

    /** 다음 라운드의 한 슬롯: 확정된 선수(player) 또는 아직 승자가 정해지지 않은 경기(sourceMatch) 중 하나를 가진다. */
    private static final class Slot {
        private final TournamentPlayer player;
        private final Match sourceMatch;

        private Slot(TournamentPlayer player, Match sourceMatch) {
            this.player = player;
            this.sourceMatch = sourceMatch;
        }

        private static Slot ofPlayer(TournamentPlayer player) {
            return new Slot(player, null);
        }

        private static Slot ofMatch(Match match) {
            return new Slot(null, match);
        }
    }

    @Transactional(readOnly = true)
    public ScheduleReportResponse report(UUID tournamentId) {
        List<Match> tournamentMatches = matches.findByTournamentIdOrderByScheduledAtAscMatchNoAsc(tournamentId);
        Map<String, Long> matchesByCourt = tournamentMatches.stream()
                .filter(match -> match.getCourtName() != null && !match.getCourtName().isBlank())
                .collect(Collectors.groupingBy(Match::getCourtName, LinkedHashMap::new, Collectors.counting()));

        long missingOfficials = tournamentMatches.stream()
                .filter(match -> match.getMatchOfficials().size() < 2)
                .count();
        long courtConflicts = countCourtConflicts(tournamentMatches);
        long officialConflicts = countOfficialConflicts(tournamentMatches);
        return new ScheduleReportResponse(tournamentId, tournamentMatches.size(), matchesByCourt, courtConflicts, officialConflicts, missingOfficials);
    }

    private void validateGenerationRequest(int groupCount, List<String> courtNames, int matchDurationMinutes, List<UUID> officialIds) {
        if (groupCount <= 0) {
            throw new IllegalArgumentException("조 수는 1 이상이어야 합니다.");
        }
        if (courtNames == null || courtNames.isEmpty()) {
            throw new IllegalArgumentException("코트 목록이 필요합니다.");
        }
        if (matchDurationMinutes <= 0) {
            throw new IllegalArgumentException("경기 시간은 1분 이상이어야 합니다.");
        }
        if (officialIds == null || officialIds.size() < 2) {
            throw new IllegalArgumentException("스케줄 생성을 위해 최소 2명의 심판이 필요합니다.");
        }
    }

    private List<TournamentGroup> createMissingGroups(UUID tournamentId, Stage stage, int groupCount) {
        List<TournamentGroup> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            char code = (char) ('A' + i);
            TournamentGroup group = adminService.createGroup(tournamentId, new com.showdown.backend.api.dto.GroupDtos.GroupRequest(
                    stage.getDivision().getId(),
                    stage.getId(),
                    stage.getDivision().getCode() + "-" + code,
                    stage.getDivision().getCode() + " Group " + code,
                    com.showdown.backend.domain.GroupType.LEAGUE,
                    i + 1
            ));
            groups.add(group);
        }
        return groups;
    }

    private List<List<TournamentPlayer>> splitPlayers(List<TournamentPlayer> players, int groupCount) {
        List<List<TournamentPlayer>> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            groups.add(new ArrayList<>());
        }
        for (int i = 0; i < players.size(); i++) {
            groups.get(i % groupCount).add(players.get(i));
        }
        return groups;
    }

    private List<UUID> rotatingOfficials(List<UUID> officialIds, int sequence) {
        if (officialIds.size() < 2) {
            throw new IllegalArgumentException("심판 2명이 필요합니다.");
        }
        int first = (sequence * 2) % officialIds.size();
        int second = (first + 1) % officialIds.size();
        if (first == second) {
            second = (second + 1) % officialIds.size();
        }
        return List.of(officialIds.get(first), officialIds.get(second));
    }

    private long countCourtConflicts(List<Match> matchesToCheck) {
        long conflicts = 0;
        List<Match> ordered = matchesToCheck.stream()
                .filter(match -> match.getScheduledAt() != null && match.getCourtName() != null)
                .sorted(Comparator.comparing(Match::getScheduledAt))
                .toList();
        for (int i = 0; i < ordered.size(); i++) {
            for (int j = i + 1; j < ordered.size(); j++) {
                if (ordered.get(i).getCourtName().equalsIgnoreCase(ordered.get(j).getCourtName()) && overlaps(ordered.get(i), ordered.get(j))) {
                    conflicts++;
                }
            }
        }
        return conflicts;
    }

    private long countOfficialConflicts(List<Match> matchesToCheck) {
        long conflicts = 0;
        for (int i = 0; i < matchesToCheck.size(); i++) {
            for (int j = i + 1; j < matchesToCheck.size(); j++) {
                if (!overlaps(matchesToCheck.get(i), matchesToCheck.get(j))) {
                    continue;
                }
                List<UUID> leftOfficials = matchesToCheck.get(i).getMatchOfficials().stream()
                        .map(matchOfficial -> matchOfficial.getOfficial().getId())
                        .toList();
                boolean conflict = matchesToCheck.get(j).getMatchOfficials().stream()
                        .map(matchOfficial -> matchOfficial.getOfficial().getId())
                        .anyMatch(leftOfficials::contains);
                if (conflict) {
                    conflicts++;
                }
            }
        }
        return conflicts;
    }

    private boolean overlaps(Match left, Match right) {
        if (left.getScheduledAt() == null || right.getScheduledAt() == null) {
            return false;
        }
        OffsetDateTime leftEnd = left.getScheduledAt().plusMinutes(left.getDurationMinutes() == null ? 30 : left.getDurationMinutes());
        OffsetDateTime rightEnd = right.getScheduledAt().plusMinutes(right.getDurationMinutes() == null ? 30 : right.getDurationMinutes());
        return left.getScheduledAt().isBefore(rightEnd) && right.getScheduledAt().isBefore(leftEnd);
    }
}
