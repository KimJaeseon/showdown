package com.showdown.backend.repository;

import com.showdown.backend.domain.TournamentGroup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<TournamentGroup, UUID> {
    List<TournamentGroup> findByTournamentIdOrderByDivisionSortOrderAscSortOrderAsc(UUID tournamentId);
}
