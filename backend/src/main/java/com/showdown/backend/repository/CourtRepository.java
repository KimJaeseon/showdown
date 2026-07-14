package com.showdown.backend.repository;

import com.showdown.backend.domain.Court;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtRepository extends JpaRepository<Court, UUID> {
    List<Court> findByTournamentIdOrderBySortOrderAsc(UUID tournamentId);
}
