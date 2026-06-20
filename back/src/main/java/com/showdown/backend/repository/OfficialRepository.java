package com.showdown.backend.repository;

import com.showdown.backend.domain.Official;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialRepository extends JpaRepository<Official, UUID> {
    List<Official> findByTournamentIdOrderByNameAsc(UUID tournamentId);
}
