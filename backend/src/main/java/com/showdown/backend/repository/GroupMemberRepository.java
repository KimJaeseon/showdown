package com.showdown.backend.repository;

import com.showdown.backend.domain.GroupMember;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    List<GroupMember> findByGroupIdOrderBySlotNoAsc(UUID groupId);
    boolean existsByGroupIdAndTournamentPlayerId(UUID groupId, UUID playerId);
    boolean existsByGroupIdAndSlotNo(UUID groupId, Integer slotNo);
    void deleteByGroupId(UUID groupId);
}
