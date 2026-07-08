package com.showdown.backend.api;

import com.showdown.backend.api.dto.RoundRobinDtos.RoundRobinRequest;
import com.showdown.backend.api.dto.RoundRobinDtos.RoundRobinResponse;
import com.showdown.backend.service.RoundRobinService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/groups/{groupId}/round-robin")
public class RoundRobinController {
    private final RoundRobinService service;
    public RoundRobinController(RoundRobinService service) { this.service = service; }
    @GetMapping("/preview") public RoundRobinResponse preview(@PathVariable UUID groupId) { return service.preview(groupId); }
    @PostMapping public RoundRobinResponse generate(@PathVariable UUID groupId, @Valid @RequestBody RoundRobinRequest request) { return service.generate(groupId, request); }
}
