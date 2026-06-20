package com.showdown.backend.repository;

import com.showdown.backend.domain.TournamentPlayer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentPlayerRepository extends JpaRepository<TournamentPlayer, UUID> {
    List<TournamentPlayer> findByTournamentIdOrderByDivisionSortOrderAscEntryNoAsc(UUID tournamentId);
}
