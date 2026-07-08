package com.showdown.backend.service;

import com.showdown.backend.api.dto.GroupMemberDtos.GroupMemberRequest;
import com.showdown.backend.domain.GroupMember;
import com.showdown.backend.domain.ParticipantStatus;
import com.showdown.backend.domain.TournamentGroup;
import com.showdown.backend.domain.TournamentPlayer;
import com.showdown.backend.repository.GroupMemberRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GroupMemberService {
    private final GroupMemberRepository members;
    private final TournamentAdminService adminService;

    public GroupMemberService(GroupMemberRepository members, TournamentAdminService adminService) {
        this.members = members;
        this.adminService = adminService;
    }

    @Transactional(readOnly = true)
    public List<GroupMember> findByGroup(UUID groupId) {
        adminService.getGroup(groupId);
        return members.findByGroupIdOrderBySlotNoAsc(groupId);
    }

    public GroupMember add(UUID groupId, GroupMemberRequest request) {
        TournamentGroup group = adminService.getGroup(groupId);
        TournamentPlayer player = adminService.getTournamentPlayer(request.tournamentPlayerId());
        if (!group.getTournament().getId().equals(player.getTournament().getId())
                || !group.getDivision().getId().equals(player.getDivision().getId())) {
            throw new IllegalArgumentException("조와 같은 대회·부문의 선수만 배정할 수 있습니다.");
        }
        if (player.getStatus() != ParticipantStatus.ACTIVE) {
            throw new IllegalArgumentException("기권 또는 실격 선수는 조에 배정할 수 없습니다.");
        }
        if (members.existsByGroupIdAndTournamentPlayerId(groupId, player.getId())) {
            throw new IllegalArgumentException("이미 조에 배정된 선수입니다.");
        }
        if (members.existsByGroupIdAndSlotNo(groupId, request.slotNo())) {
            throw new IllegalArgumentException("이미 사용 중인 조 슬롯입니다.");
        }
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setTournamentPlayer(player);
        member.setSlotNo(request.slotNo());
        member.setSourceRule("manual");
        return members.save(member);
    }

    public void remove(UUID groupId, UUID memberId) {
        GroupMember member = members.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("조 구성원을 찾을 수 없습니다."));
        if (!member.getGroup().getId().equals(groupId)) {
            throw new IllegalArgumentException("요청한 조의 구성원이 아닙니다.");
        }
        members.delete(member);
    }
}
