package com.showdown.backend.repository;

import com.showdown.backend.domain.Match;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, UUID> {
    List<Match> findByTournamentIdOrderByScheduledAtAscMatchNoAsc(UUID tournamentId);

    List<Match> findByTournamentIdAndScheduledAtIsNotNullOrderByScheduledAtAscMatchNoAsc(UUID tournamentId);

    List<Match> findByMatchPlayers_TournamentPlayer_IdOrderByScheduledAtAscMatchNoAsc(UUID tournamentPlayerId);

    List<Match> findByPlayer1SourceMatch_Id(UUID matchId);

    List<Match> findByPlayer2SourceMatch_Id(UUID matchId);
}
