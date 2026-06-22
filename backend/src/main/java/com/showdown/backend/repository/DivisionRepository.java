package com.showdown.backend.repository;

import com.showdown.backend.domain.Division;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DivisionRepository extends JpaRepository<Division, UUID> {
    List<Division> findByTournamentIdOrderBySortOrderAsc(UUID tournamentId);
}
