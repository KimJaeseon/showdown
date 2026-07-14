package com.showdown.backend.service;

import com.showdown.backend.domain.Division;
import com.showdown.backend.domain.GroupMember;
import com.showdown.backend.domain.Match;
import com.showdown.backend.domain.MatchEndReason;
import com.showdown.backend.domain.MatchStatus;
import com.showdown.backend.domain.RankingRule;
import com.showdown.backend.domain.RankingSnapshot;
import com.showdown.backend.domain.Stage;
import com.showdown.backend.domain.Tournament;
import com.showdown.backend.domain.TournamentGroup;
import com.showdown.backend.domain.TournamentPlayer;
import com.showdown.backend.repository.GroupMemberRepository;
import com.showdown.backend.repository.GroupRepository;
import com.showdown.backend.repository.MatchRepository;
import com.showdown.backend.repository.RankingRuleRepository;
import com.showdown.backend.repository.RankingSnapshotRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RankingService {
    private final RankingSnapshotRepository rankings;
    private final GroupMemberRepository members;
    private final GroupRepository groups;
    private final MatchRepository matches;
    private final RankingRuleRepository rankingRules;

    public RankingService(RankingSnapshotRepository rankings, GroupMemberRepository members, GroupRepository groups,
            MatchRepository matches, RankingRuleRepository rankingRules) {
        this.rankings = rankings;
        this.members = members;
        this.groups = groups;
        this.matches = matches;
        this.rankingRules = rankingRules;
    }

    public List<RankingSnapshot> recalculate(TournamentGroup group) {
        RankingRule rule = resolveRankingRule(group.getTournament());
        List<TournamentPlayer> participants = members.findByGroupIdOrderBySlotNoAsc(group.getId()).stream()
                .map(GroupMember::getTournamentPlayer)
                .toList();
        List<Match> groupMatches = matches.findByTournamentIdOrderByScheduledAtAscMatchNoAsc(group.getTournament().getId()).stream()
                .filter(match -> group.equals(match.getGroup()) && isRankingMatch(match))
                .toList();
        List<Stats> ordered = computeStandings(participants, groupMatches, rule);

        rankings.deleteByGroupId(group.getId());
        return persist(ordered, group.getTournament(), group.getDivision(), group.getStage(), group);
    }

    /** TDD-27: 단계가 종료되면 조 구분 없는 단계 전체 최종 순위(group_id IS NULL)를 재계산해 저장한다. */
    public List<RankingSnapshot> recalculateStageFinal(Stage stage) {
        RankingRule rule = resolveRankingRule(stage.getTournament());
        List<TournamentGroup> stageGroups = groups.findByStageIdOrderBySortOrderAsc(stage.getId());
        Set<UUID> seenPlayerIds = new HashSet<>();
        List<TournamentPlayer> participants = new ArrayList<>();
        for (TournamentGroup stageGroup : stageGroups) {
            for (GroupMember member : members.findByGroupIdOrderBySlotNoAsc(stageGroup.getId())) {
                if (seenPlayerIds.add(member.getTournamentPlayer().getId())) {
                    participants.add(member.getTournamentPlayer());
                }
            }
        }
        List<Match> stageMatches = matches.findByTournamentIdOrderByScheduledAtAscMatchNoAsc(stage.getTournament().getId()).stream()
                .filter(match -> match.getStage() != null && match.getStage().getId().equals(stage.getId()) && isRankingMatch(match))
                .toList();
        List<Stats> ordered = computeStandings(participants, stageMatches, rule);

        rankings.deleteByStageIdAndGroupIsNull(stage.getId());
        return persist(ordered, stage.getTournament(), stage.getDivision(), stage, null);
    }

    private List<Stats> computeStandings(List<TournamentPlayer> participants, List<Match> rankingMatches, RankingRule rule) {
        Map<UUID, Stats> stats = new HashMap<>();
        for (TournamentPlayer participant : participants) {
            stats.put(participant.getId(), new Stats(participant));
        }
        for (Match match : rankingMatches) {
            Stats left = stats.computeIfAbsent(match.getPlayer1().getId(), ignored -> new Stats(match.getPlayer1()));
            Stats right = stats.computeIfAbsent(match.getPlayer2().getId(), ignored -> new Stats(match.getPlayer2()));
            boolean player1Won = match.getWinner() == match.getPlayer1();
            boolean walkover = match.getStatus() == MatchStatus.WALKOVER;
            int winPoints = walkover ? rule.getWalkoverWinPoints() : rule.getWinPoints();
            int player1Points = player1Won ? winPoints : rule.getLossPoints();
            int player2Points = player1Won ? rule.getLossPoints() : winPoints;
            left.add(match.getPlayer1SetsWon(), match.getPlayer2SetsWon(), match.getPlayer1TotalPoints(), match.getPlayer2TotalPoints(), player1Won, player1Points);
            right.add(match.getPlayer2SetsWon(), match.getPlayer1SetsWon(), match.getPlayer2TotalPoints(), match.getPlayer1TotalPoints(), !player1Won, player2Points);
        }
        List<Stats> ordered = new ArrayList<>(stats.values());
        ordered.sort(Comparator.comparingInt(Stats::matchPoints).reversed()
                .thenComparing(Comparator.comparingInt(Stats::wins).reversed())
                .thenComparing(Comparator.comparingInt(Stats::setDifference).reversed())
                .thenComparing(Comparator.comparingInt(Stats::pointDifference).reversed())
                .thenComparing(Comparator.comparingInt(Stats::pointsFor).reversed())
                .thenComparing(s -> s.player().getSeedNo() == null ? Integer.MAX_VALUE : s.player().getSeedNo()));
        applyTieBreaks(ordered, rankingMatches, rule);
        return ordered;
    }

    private List<RankingSnapshot> persist(List<Stats> ordered, Tournament tournament, Division division,
            Stage stage, TournamentGroup group) {
        List<RankingSnapshot> result = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            Stats s = ordered.get(i);
            RankingSnapshot row = new RankingSnapshot();
            row.setTournament(tournament); row.setDivision(division); row.setStage(stage); row.setGroup(group);
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

    @Transactional(readOnly = true)
    public List<RankingSnapshot> findStageFinal(UUID stageId) {
        return rankings.findByStageIdAndGroupIsNullOrderByRankNoAsc(stageId);
    }

    private void applyTieBreaks(List<Stats> ordered, List<Match> completed, RankingRule rule) {
        int i = 0;
        while (i < ordered.size()) {
            int j = i + 1;
            while (j < ordered.size() && ordered.get(i).baseEquals(ordered.get(j))) {
                j++;
            }
            int tiedCount = j - i;
            if (tiedCount == 2) {
                applyHeadToHead(ordered, completed, i);
            } else if (tiedCount >= 3) {
                applySubgroupTieBreak(ordered, completed, rule, i, j);
            }
            i = j;
        }
    }

    private void applyHeadToHead(List<Stats> ordered, List<Match> completed, int i) {
        Stats a = ordered.get(i), b = ordered.get(i + 1);
        Match direct = completed.stream().filter(m -> involves(m, a.player) && involves(m, b.player)).findFirst().orElse(null);
        if (direct != null && direct.getWinner() != null) {
            Stats winner = direct.getWinner().getId().equals(a.player.getId()) ? a : b;
            Stats loser = winner == a ? b : a;
            winner.note = "2인 동률 상대 전적 우선"; loser.note = "2인 동률 상대 전적 적용";
            if (winner == b) { ordered.set(i, b); ordered.set(i + 1, a); }
        }
    }

    /**
     * 3인 이상 동률은 동률 선수 사이의 경기만으로 부분 승점·세트 득실·점수 득실을 재계산해 정렬한다.
     * 그래도 동률이면 시드 번호로 최종 결정한다.
     */
    private void applySubgroupTieBreak(List<Stats> ordered, List<Match> completed, RankingRule rule, int start, int end) {
        List<Stats> tiedGroup = new ArrayList<>(ordered.subList(start, end));
        Set<UUID> groupIds = new HashSet<>();
        for (Stats s : tiedGroup) groupIds.add(s.player.getId());

        Map<UUID, Stats> subgroupStats = new HashMap<>();
        for (Stats s : tiedGroup) subgroupStats.put(s.player.getId(), new Stats(s.player));
        for (Match match : completed) {
            UUID p1 = match.getPlayer1().getId();
            UUID p2 = match.getPlayer2().getId();
            if (!groupIds.contains(p1) || !groupIds.contains(p2)) continue;
            boolean player1Won = match.getWinner() == match.getPlayer1();
            boolean walkover = match.getStatus() == MatchStatus.WALKOVER;
            int winPoints = walkover ? rule.getWalkoverWinPoints() : rule.getWinPoints();
            int player1Points = player1Won ? winPoints : rule.getLossPoints();
            int player2Points = player1Won ? rule.getLossPoints() : winPoints;
            subgroupStats.get(p1).add(match.getPlayer1SetsWon(), match.getPlayer2SetsWon(), match.getPlayer1TotalPoints(), match.getPlayer2TotalPoints(), player1Won, player1Points);
            subgroupStats.get(p2).add(match.getPlayer2SetsWon(), match.getPlayer1SetsWon(), match.getPlayer2TotalPoints(), match.getPlayer1TotalPoints(), !player1Won, player2Points);
        }

        tiedGroup.sort(Comparator.<Stats>comparingInt(s -> subgroupStats.get(s.player.getId()).matchPoints()).reversed()
                .thenComparing(Comparator.<Stats>comparingInt(s -> subgroupStats.get(s.player.getId()).wins()).reversed())
                .thenComparing(Comparator.<Stats>comparingInt(s -> subgroupStats.get(s.player.getId()).setDifference()).reversed())
                .thenComparing(Comparator.<Stats>comparingInt(s -> subgroupStats.get(s.player.getId()).pointDifference()).reversed())
                .thenComparing(Comparator.<Stats>comparingInt(s -> subgroupStats.get(s.player.getId()).pointsFor()).reversed())
                .thenComparing(s -> s.player().getSeedNo() == null ? Integer.MAX_VALUE : s.player().getSeedNo()));

        for (Stats s : tiedGroup) {
            s.note = (end - start) + "인 이상 동률 부분 전적 적용";
        }
        for (int k = 0; k < tiedGroup.size(); k++) {
            ordered.set(start + k, tiedGroup.get(k));
        }
    }

    private boolean involves(Match match, TournamentPlayer player) {
        return match.getPlayer1().getId().equals(player.getId()) || match.getPlayer2().getId().equals(player.getId());
    }

    private boolean isRankingMatch(Match match) {
        return match.getStatus() == MatchStatus.COMPLETED
                || (match.getStatus() == MatchStatus.WALKOVER && match.getEndReason() != MatchEndReason.BYE);
    }

    private RankingRule resolveRankingRule(Tournament tournament) {
        return rankingRules.findFirstByTournamentIdAndIsDefaultTrue(tournament.getId())
                .or(rankingRules::findFirstByTournamentIdIsNullAndIsDefaultTrue)
                .orElseGet(RankingRule::fallbackDefault);
    }

    private static final class Stats {
        private final TournamentPlayer player; private int played; private int wins; private int losses;
        private int setsWon; private int setsLost; private int pointsFor; private int pointsAgainst; private int matchPoints; private String note;
        private Stats(TournamentPlayer player) { this.player = player; }
        private void add(int sw, int sl, int pf, int pa, boolean win, int pointsEarned) {
            played++; setsWon += sw; setsLost += sl; pointsFor += pf; pointsAgainst += pa; matchPoints += pointsEarned;
            if (win) wins++; else losses++;
        }
        private TournamentPlayer player() { return player; } private int wins() { return wins; }
        private int matchPoints() { return matchPoints; } private int setDifference() { return setsWon - setsLost; }
        private int pointDifference() { return pointsFor - pointsAgainst; } private int pointsFor() { return pointsFor; }
        private boolean baseEquals(Stats o) { return matchPoints() == o.matchPoints() && wins == o.wins && setDifference() == o.setDifference() && pointDifference() == o.pointDifference() && pointsFor == o.pointsFor; }
    }
}
