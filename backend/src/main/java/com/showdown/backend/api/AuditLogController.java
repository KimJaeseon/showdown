package com.showdown.backend.api;
import com.showdown.backend.service.AuditLogService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/admin/tournaments/{tournamentId}/audit-logs")
public class AuditLogController {
    private final AuditLogService service; public AuditLogController(AuditLogService service) { this.service = service; }
    @GetMapping public List<AuditLogResponse> list(@PathVariable UUID tournamentId) { return service.find(tournamentId).stream().map(log -> new AuditLogResponse(log.getId(), log.getAction(), log.getEntityType(), log.getEntityId(), log.getBeforeJson(), log.getAfterJson(), log.getCreatedAt())).toList(); }
    public record AuditLogResponse(UUID id, String action, String entityType, UUID entityId, Map<String,Object> before, Map<String,Object> after, OffsetDateTime createdAt) {}
}
