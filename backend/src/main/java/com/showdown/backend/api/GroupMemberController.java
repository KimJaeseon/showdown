package com.showdown.backend.api;

import com.showdown.backend.api.dto.GroupMemberDtos.GroupMemberRequest;
import com.showdown.backend.api.dto.GroupMemberDtos.GroupMemberResponse;
import com.showdown.backend.domain.GroupMember;
import com.showdown.backend.service.GroupMemberService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/groups/{groupId}/members")
public class GroupMemberController {
    private final GroupMemberService service;
    public GroupMemberController(GroupMemberService service) { this.service = service; }

    @GetMapping
    public List<GroupMemberResponse> list(@PathVariable UUID groupId) {
        return service.findByGroup(groupId).stream().map(this::response).toList();
    }

    @PostMapping
    public GroupMemberResponse add(@PathVariable UUID groupId, @Valid @RequestBody GroupMemberRequest request) {
        return response(service.add(groupId, request));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> remove(@PathVariable UUID groupId, @PathVariable UUID memberId) {
        service.remove(groupId, memberId);
        return ResponseEntity.noContent().build();
    }

    private GroupMemberResponse response(GroupMember member) {
        var entry = member.getTournamentPlayer();
        String name = entry.getDisplayNameOverride() == null || entry.getDisplayNameOverride().isBlank()
                ? entry.getPlayer().getDisplayName() : entry.getDisplayNameOverride();
        return new GroupMemberResponse(member.getId(), member.getGroup().getId(), entry.getId(), name,
                member.getSlotNo(), member.getSourceRule());
    }
}
