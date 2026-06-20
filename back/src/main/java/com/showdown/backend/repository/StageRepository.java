package com.showdown.backend.repository;

import com.showdown.backend.domain.Stage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageRepository extends JpaRepository<Stage, UUID> {
    List<Stage> findByTournamentIdOrderByDivisionSortOrderAscSortOrderAsc(UUID tournamentId);
}
