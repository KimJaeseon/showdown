package com.showdown.backend.service;

import com.showdown.backend.domain.AuditLog;
import com.showdown.backend.domain.Tournament;
import com.showdown.backend.repository.AppUserRepository;
import com.showdown.backend.repository.AuditLogRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private final AuditLogRepository logs; private final AppUserRepository users;
    public AuditLogService(AuditLogRepository logs, AppUserRepository users) { this.logs = logs; this.users = users; }
    public void record(Tournament tournament, String action, String entityType, UUID entityId, Map<String, Object> before, Map<String, Object> after) {
        AuditLog log = new AuditLog(); log.setTournament(tournament); log.setAction(action); log.setEntityType(entityType); log.setEntityId(entityId);
        log.setBeforeJson(before); log.setAfterJson(after);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) users.findByEmailAndActiveTrue(authentication.getName()).ifPresent(log::setUser);
        logs.save(log);
    }
    public List<AuditLog> find(UUID tournamentId) { return logs.findByTournamentIdOrderByCreatedAtDesc(tournamentId); }
}
