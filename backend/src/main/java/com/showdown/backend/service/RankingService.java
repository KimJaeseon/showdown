package com.showdown.backend.service;

import com.showdown.backend.domain.GroupMember;
import com.showdown.backend.domain.Match;
import com.showdown.backend.domain.MatchEndReason;
import com.showdown.backend.domain.MatchStatus;
import com.showdown.backend.domain.RankingSnapshot;
import com.showdown.backend.domain.TournamentGroup;
import com.showdown.backend.domain.TournamentPlayer;
import com.showdown.backend.repository.GroupMemberRepository;
import com.showdown.backend.repository.MatchRepository;
import com.showdown.backend.repository.RankingSnapshotRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RankingService {
    private final RankingSnapshotRepository rankings;
    private final GroupMemberRepository members;
    private final MatchRepository matches;

    public RankingService(RankingSnapshotRepository rankings, GroupMemberRepository members, MatchRepository matches) {
        this.rankings = rankings;
        this.members = members;
        this.matches = matches;
    }

    public List<RankingSnapshot> recalculate(TournamentGroup group) {
        Map<UUID, Stats> stats = new HashMap<>();
        for (GroupMember member : members.findByGroupIdOrderBySlotNoAsc(group.getId())) {
            stats.put(member.getTournamentPlayer().getId(), new Stats(member.getTournamentPlayer()));
        }
        List<Match> completed = matches.findByTournamentIdOrderByScheduledAtAscMatchNoAsc(group.getTournament().getId()).stream()
                .filter(match -> group.equals(match.getGroup()) && isRankingMatch(match))
                .toList();
        for (Match match : completed) {
            Stats left = stats.computeIfAbsent(match.getPlayer1().getId(), ignored -> new Stats(match.getPlayer1()));
            Stats right = stats.computeIfAbsent(match.getPlayer2().getId(), ignored -> new Stats(match.getPlayer2()));
            left.add(match.getPlayer1SetsWon(), match.getPlayer2SetsWon(), match.getPlayer1TotalPoints(), match.getPlayer2TotalPoints(), match.getWinner() == match.getPlayer1());
            right.add(match.getPlayer2SetsWon(), match.getPlayer1SetsWon(), match.getPlayer2TotalPoints(), match.getPlayer1TotalPoints(), match.getWinner() == match.getPlayer2());
        }
        List<Stats> ordered = new ArrayList<>(stats.values());
        ordered.sort(Comparator.comparingInt(Stats::matchPoints).reversed()
                .thenComparing(Comparator.comparingInt(Stats::wins).reversed())
                .thenComparing(Comparator.comparingInt(Stats::setDifference).reversed())
                .thenComparing(Comparator.comparingInt(Stats::pointDifference).reversed())
                .thenComparing(Comparator.comparingInt(Stats::pointsFor).reversed())
                .thenComparing(s -> s.player().getSeedNo() == null ? Integer.MAX_VALUE : s.player().getSeedNo()));
        applyHeadToHead(ordered, completed);

        rankings.deleteByGroupId(group.getId());
        List<RankingSnapshot> result = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            Stats s = ordered.get(i);
            RankingSnapshot row = new RankingSnapshot();
            row.setTournament(group.getTournament()); row.setDivision(group.getDivision()); row.setStage(group.getStage()); row.setGroup(group);
            row.setTournamentPlayer(s.player()); row.setRankNo(i + 1); row.setMatchesPlayed(s.played); row.setWins(s.wins); row.setLosses(s.losses);
            row.setMatchPoints(s.matchPoints()); row.setSetsWon(s.setsWon); row.setSetsLost(s.setsLost); row.setSetDifference(s.setDifference());
            row.setPointsFor(s.pointsFor); row.setPointsAgainst(s.pointsAgainst); row.setPointDifference(s.pointDifference());
            row.setTieBreakNote(s.note); row.setCalculatedAt(OffsetDateTime.now()); result.add(rankings.save(row));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<RankingSnapshot> findByTournament(UUID tournamentId) {
        return rankings.findByTournamentIdOrderByDivisionSortOrderAscRankNoAsc(tournamentId);
    }

    private void applyHeadToHead(List<Stats> ordered, List<Match> completed) {
        for (int i = 0; i + 1 < ordered.size(); i++) {
            Stats a = ordered.get(i), b = ordered.get(i + 1);
            if (!a.baseEquals(b)) continue;
            Match direct = completed.stream().filter(m -> involves(m, a.player) && involves(m, b.player)).findFirst().orElse(null);
            if (direct != null && direct.getWinner() != null) {
                Stats winner = direct.getWinner().getId().equals(a.player.getId()) ? a : b;
                Stats loser = winner == a ? b : a;
                winner.note = "2인 동률 상대 전적 우선"; loser.note = "2인 동률 상대 전적 적용";
                if (winner == b) { ordered.set(i, b); ordered.set(i + 1, a); }
            }
        }
    }

    private boolean involves(Match match, TournamentPlayer player) {
        return match.getPlayer1().getId().equals(player.getId()) || match.getPlayer2().getId().equals(player.getId());
    }

    private boolean isRankingMatch(Match match) {
        return match.getStatus() == MatchStatus.COMPLETED
                || (match.getStatus() == MatchStatus.WALKOVER && match.getEndReason() != MatchEndReason.BYE);
    }

    private static final class Stats {
        private final TournamentPlayer player; private int played; private int wins; private int losses;
        private int setsWon; private int setsLost; private int pointsFor; private int pointsAgainst; private String note;
        private Stats(TournamentPlayer player) { this.player = player; }
        private void add(int sw, int sl, int pf, int pa, boolean win) { played++; setsWon += sw; setsLost += sl; pointsFor += pf; pointsAgainst += pa; if (win) wins++; else losses++; }
        private TournamentPlayer player() { return player; } private int wins() { return wins; }
        private int matchPoints() { return wins; } private int setDifference() { return setsWon - setsLost; }
        private int pointDifference() { return pointsFor - pointsAgainst; } private int pointsFor() { return pointsFor; }
        private boolean baseEquals(Stats o) { return matchPoints() == o.matchPoints() && wins == o.wins && setDifference() == o.setDifference() && pointDifference() == o.pointDifference() && pointsFor == o.pointsFor; }
    }
}
