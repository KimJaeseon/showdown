package com.showdown.backend.repository;
import com.showdown.backend.domain.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByTournamentIdOrderByCreatedAtDesc(UUID tournamentId);
}
