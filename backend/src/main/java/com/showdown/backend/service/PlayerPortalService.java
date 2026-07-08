package com.showdown.backend.service;

import com.showdown.backend.api.ApiMapper;
import com.showdown.backend.api.dto.MatchDtos.MatchResponse;
import com.showdown.backend.api.dto.PlayerPortalDtos.PlayerPortalResponse;
import com.showdown.backend.api.dto.PlayerPortalDtos.PlayerStatsResponse;
import com.showdown.backend.domain.AppUser;
import com.showdown.backend.domain.Match;
import com.showdown.backend.domain.MatchEndReason;
import com.showdown.backend.domain.MatchStatus;
import com.showdown.backend.domain.TournamentPlayer;
import com.showdown.backend.repository.AppUserRepository;
import com.showdown.backend.repository.MatchRepository;
import com.showdown.backend.repository.MatchSetRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlayerPortalService {
    private final AppUserRepository users;
    private final MatchRepository matches;
    private final MatchSetRepository matchSets;

    public PlayerPortalService(AppUserRepository users, MatchRepository matches, MatchSetRepository matchSets) {
        this.users = users;
        this.matches = matches;
        this.matchSets = matchSets;
    }

    public PlayerPortalResponse getPortal(String username) {
        AppUser user = users.findByEmailAndActiveTrue(username)
                .orElseThrow(() -> new EntityNotFoundException("선수 계정을 찾을 수 없습니다."));
        TournamentPlayer player = user.getTournamentPlayer();
        if (player == null) {
            throw new EntityNotFoundException("선수 계정에 연결된 참가 선수가 없습니다.");
        }

        var allMatches = matches.findByMatchPlayers_TournamentPlayer_IdOrderByScheduledAtAscMatchNoAsc(player.getId());
        var scheduled = allMatches.stream()
                .filter(match -> match.getStatus() == MatchStatus.SCHEDULED || match.getStatus() == MatchStatus.RUNNING)
                .map(this::toMatchResponse)
                .toList();
        var completed = allMatches.stream()
                .filter(match -> match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.WALKOVER)
                .map(this::toMatchResponse)
                .toList();
        MatchResponse nextMatch = scheduled.stream()
                .min(Comparator.comparing(MatchResponse::scheduledAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        return new PlayerPortalResponse(
                ApiMapper.toTournamentPlayerResponse(player),
                nextMatch,
                scheduled,
                completed,
                calculateStats(player, allMatches)
        );
    }

    private MatchResponse toMatchResponse(Match match) {
        return ApiMapper.toMatchResponse(match, matchSets.findByMatchIdOrderBySetNoAsc(match.getId()));
    }

    private PlayerStatsResponse calculateStats(TournamentPlayer player, Iterable<Match> playerMatches) {
        int played = 0;
        int wins = 0;
        int losses = 0;
        int setsWon = 0;
        int setsLost = 0;
        int pointsFor = 0;
        int pointsAgainst = 0;

        for (Match match : playerMatches) {
            if (match.getStatus() != MatchStatus.COMPLETED
                    && !(match.getStatus() == MatchStatus.WALKOVER && match.getEndReason() != MatchEndReason.BYE)) {
                continue;
            }
            played++;
            boolean isPlayer1 = match.getPlayer1().getId().equals(player.getId());
            boolean won = match.getWinner() != null && match.getWinner().getId().equals(player.getId());
            if (won) wins++; else losses++;
            setsWon += isPlayer1 ? match.getPlayer1SetsWon() : match.getPlayer2SetsWon();
            setsLost += isPlayer1 ? match.getPlayer2SetsWon() : match.getPlayer1SetsWon();
            pointsFor += isPlayer1 ? match.getPlayer1TotalPoints() : match.getPlayer2TotalPoints();
            pointsAgainst += isPlayer1 ? match.getPlayer2TotalPoints() : match.getPlayer1TotalPoints();
        }

        return new PlayerStatsResponse(played, wins, losses, setsWon, setsLost, pointsFor, pointsAgainst);
    }
}
