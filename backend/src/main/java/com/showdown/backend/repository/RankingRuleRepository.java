package com.showdown.backend.repository;

import com.showdown.backend.domain.RankingRule;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingRuleRepository extends JpaRepository<RankingRule, UUID> {
    Optional<RankingRule> findFirstByTournamentIdAndIsDefaultTrue(UUID tournamentId);

    Optional<RankingRule> findFirstByTournamentIdIsNullAndIsDefaultTrue();
}
