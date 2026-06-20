package com.showdown.backend.api;

import com.showdown.backend.api.dto.UserDtos.UserRequest;
import com.showdown.backend.api.dto.UserDtos.UserResponse;
import com.showdown.backend.repository.AppUserRepository;
import com.showdown.backend.service.TournamentAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin Users", description = "관리자, 심판, 선수 계정과 롤 관리 API")
public class UserAdminController {
    private final TournamentAdminService adminService;
    private final AppUserRepository users;

    public UserAdminController(TournamentAdminService adminService, AppUserRepository users) {
        this.adminService = adminService;
        this.users = users;
    }

    @GetMapping
    @Operation(summary = "계정 목록 조회")
    public List<UserResponse> users() {
        return users.findAll().stream().map(ApiMapper::toUserResponse).toList();
    }

    @PostMapping
    @Operation(summary = "계정 생성")
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        return ApiMapper.toUserResponse(adminService.createUser(request));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "계정 수정")
    public UserResponse updateUser(@PathVariable UUID userId, @Valid @RequestBody UserRequest request) {
        return ApiMapper.toUserResponse(adminService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "계정 삭제")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
