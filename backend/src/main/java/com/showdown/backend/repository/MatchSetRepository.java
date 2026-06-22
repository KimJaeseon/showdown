package com.showdown.backend.repository;

import com.showdown.backend.domain.MatchSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchSetRepository extends JpaRepository<MatchSet, UUID> {
    List<MatchSet> findByMatchIdOrderBySetNoAsc(UUID matchId);

    void deleteByMatchId(UUID matchId);
}
