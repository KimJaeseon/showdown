package com.showdown.backend.repository;

import com.showdown.backend.domain.Tournament;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository extends JpaRepository<Tournament, UUID> {
    Optional<Tournament> findByCode(String code);
}
