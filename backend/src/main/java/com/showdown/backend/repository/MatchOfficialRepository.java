package com.showdown.backend.repository;

import com.showdown.backend.domain.MatchOfficial;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchOfficialRepository extends JpaRepository<MatchOfficial, UUID> {
    List<MatchOfficial> findByMatchIdOrderByPositionNoAsc(UUID matchId);

    void deleteByMatchId(UUID matchId);
}
