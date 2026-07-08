package com.showdown.backend.repository;

import com.showdown.backend.domain.RankingSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingSnapshotRepository extends JpaRepository<RankingSnapshot, UUID> {
    List<RankingSnapshot> findByTournamentIdOrderByDivisionSortOrderAscRankNoAsc(UUID tournamentId);
    List<RankingSnapshot> findByGroupIdOrderByRankNoAsc(UUID groupId);
    void deleteByGroupId(UUID groupId);
}
