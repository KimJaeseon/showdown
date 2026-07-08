package com.showdown.backend.service;

import com.showdown.backend.api.dto.MatchDtos.MatchRequest;
import com.showdown.backend.api.dto.RoundRobinDtos.RoundRobinMatchPreview;
import com.showdown.backend.api.dto.RoundRobinDtos.RoundRobinRequest;
import com.showdown.backend.api.dto.RoundRobinDtos.RoundRobinResponse;
import com.showdown.backend.domain.GroupMember;
import com.showdown.backend.domain.Match;
import com.showdown.backend.domain.MatchStatus;
import com.showdown.backend.domain.TournamentGroup;
import com.showdown.backend.domain.TournamentPlayer;
import com.showdown.backend.repository.GroupMemberRepository;
import com.showdown.backend.repository.MatchRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoundRobinService {
    private final GroupMemberRepository members;
    private final MatchRepository matches;
    private final TournamentAdminService adminService;

    public RoundRobinService(GroupMemberRepository members, MatchRepository matches, TournamentAdminService adminService) {
        this.members = members; this.matches = matches; this.adminService = adminService;
    }

    @Transactional(readOnly = true)
    public RoundRobinResponse preview(UUID groupId) {
        TournamentGroup group = adminService.getGroup(groupId);
        return new RoundRobinResponse(pairs(group).size(), 0, pairs(group));
    }

    public RoundRobinResponse generate(UUID groupId, RoundRobinRequest request) {
        TournamentGroup group = adminService.getGroup(groupId);
        List<RoundRobinMatchPreview> planned = pairs(group);
        if (planned.isEmpty()) throw new IllegalArgumentException("라운드로빈 생성에는 조 구성원 2명 이상이 필요합니다.");
        if (request.officialIds().size() < 2) throw new IllegalArgumentException("심판은 최소 2명이 필요합니다.");
        List<Match> tournamentMatches = matches.findByTournamentIdOrderByScheduledAtAscMatchNoAsc(group.getTournament().getId());
        Set<String> existingPairs = new HashSet<>();
        for (Match match : tournamentMatches) {
            if (match.getGroup() != null && match.getGroup().getId().equals(groupId)) {
                existingPairs.add(pairKey(match.getPlayer1().getId(), match.getPlayer2().getId()));
            }
        }
        int nextMatchNo = tournamentMatches.stream().mapToInt(Match::getMatchNo).max().orElse(0) + 1;
        int created = 0;
        for (RoundRobinMatchPreview pair : planned) {
            if (existingPairs.contains(pairKey(pair.player1Id(), pair.player2Id()))) continue;
            int sequence = created;
            String court = request.courtNames().get(sequence % request.courtNames().size());
            var at = request.startAt().plusMinutes((long) request.matchDurationMinutes() * (sequence / request.courtNames().size()));
            int officialOffset = (sequence * 2) % request.officialIds().size();
            List<UUID> assigned = List.of(request.officialIds().get(officialOffset), request.officialIds().get((officialOffset + 1) % request.officialIds().size()));
            adminService.createMatch(group.getTournament().getId(), new MatchRequest(group.getDivision().getId(), group.getStage().getId(),
                    groupId, nextMatchNo++, at, court, request.matchDurationMinutes(), 3, null, assigned,
                    pair.player1Id(), pair.player2Id(), MatchStatus.SCHEDULED));
            created++;
        }
        return new RoundRobinResponse(planned.size(), created, planned);
    }

    private List<RoundRobinMatchPreview> pairs(TournamentGroup group) {
        List<TournamentPlayer> players = members.findByGroupIdOrderBySlotNoAsc(group.getId()).stream().map(GroupMember::getTournamentPlayer).toList();
        List<RoundRobinMatchPreview> result = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) for (int j = i + 1; j < players.size(); j++) {
            TournamentPlayer a = players.get(i), b = players.get(j);
            result.add(new RoundRobinMatchPreview(a.getId(), name(a), b.getId(), name(b)));
        }
        return result;
    }

    private String name(TournamentPlayer p) { return p.getDisplayNameOverride() == null || p.getDisplayNameOverride().isBlank() ? p.getPlayer().getDisplayName() : p.getDisplayNameOverride(); }
    private String pairKey(UUID a, UUID b) { return a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a; }
}
