package com.showdown.backend.service;

import com.showdown.backend.domain.Division;
import com.showdown.backend.domain.Match;
import com.showdown.backend.domain.MatchStatus;
import com.showdown.backend.domain.Tournament;
import com.showdown.backend.domain.TournamentGroup;
import com.showdown.backend.domain.TournamentPlayer;
import com.showdown.backend.domain.TournamentStatus;
import com.showdown.backend.repository.DivisionRepository;
import com.showdown.backend.repository.GroupRepository;
import com.showdown.backend.repository.MatchRepository;
import com.showdown.backend.repository.MatchSetRepository;
import com.showdown.backend.repository.TournamentPlayerRepository;
import com.showdown.backend.repository.TournamentRepository;
import com.showdown.backend.repository.RankingSnapshotRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PublicQueryService {
    private final TournamentRepository tournaments;
    private final DivisionRepository divisions;
    private final TournamentPlayerRepository tournamentPlayers;
    private final GroupRepository groups;
    private final MatchRepository matches;
    private final MatchSetRepository matchSets;
    private final RankingSnapshotRepository rankings;

    public PublicQueryService(
            TournamentRepository tournaments,
            DivisionRepository divisions,
            TournamentPlayerRepository tournamentPlayers,
            GroupRepository groups,
            MatchRepository matches,
            MatchSetRepository matchSets,
            RankingSnapshotRepository rankings
    ) {
        this.tournaments = tournaments;
        this.divisions = divisions;
        this.tournamentPlayers = tournamentPlayers;
        this.groups = groups;
        this.matches = matches;
        this.matchSets = matchSets;
        this.rankings = rankings;
    }

    public List<Tournament> findTournaments() {
        return tournaments.findAll().stream().filter(this::isPublic).toList();
    }

    public Tournament getTournamentByCode(String code) {
        return tournaments.findByCode(code).filter(this::isPublic)
                .orElseThrow(() -> new EntityNotFoundException("공개된 대회를 찾을 수 없습니다."));
    }

    public List<Division> getDivisions(String code) {
        Tournament tournament = getTournamentByCode(code);
        return divisions.findByTournamentIdOrderBySortOrderAsc(tournament.getId());
    }

    public List<TournamentPlayer> getPlayers(String code) {
        Tournament tournament = getTournamentByCode(code);
        return tournamentPlayers.findByTournamentIdOrderByDivisionSortOrderAscEntryNoAsc(tournament.getId());
    }

    public List<TournamentGroup> getGroups(String code) {
        Tournament tournament = getTournamentByCode(code);
        return groups.findByTournamentIdOrderByDivisionSortOrderAscSortOrderAsc(tournament.getId());
    }

    public List<Match> getMatches(String code, MatchStatus status) {
        Tournament tournament = getTournamentByCode(code);
        if (status == null) {
            return matches.findByTournamentIdOrderByScheduledAtAscMatchNoAsc(tournament.getId());
        }
        return matches.findByTournamentIdOrderByScheduledAtAscMatchNoAsc(tournament.getId()).stream()
                .filter(match -> match.getStatus() == status)
                .toList();
    }

    public List<com.showdown.backend.domain.MatchSet> getMatchSets(UUID matchId) {
        return matchSets.findByMatchIdOrderBySetNoAsc(matchId);
    }

    public List<com.showdown.backend.domain.RankingSnapshot> getRankings(String code) {
        Tournament tournament = getTournamentByCode(code);
        return rankings.findByTournamentIdOrderByDivisionSortOrderAscRankNoAsc(tournament.getId());
    }

    private boolean isPublic(Tournament tournament) {
        return tournament.getStatus() == TournamentStatus.PUBLISHED
                || tournament.getStatus() == TournamentStatus.RUNNING
                || tournament.getStatus() == TournamentStatus.FINISHED;
    }
}
