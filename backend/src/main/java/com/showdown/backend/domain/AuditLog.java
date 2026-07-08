package com.showdown.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "tournament_id") private Tournament tournament;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private AppUser user;
    @Column(nullable = false, length = 120) private String action;
    @Column(name = "entity_type", nullable = false, length = 120) private String entityType;
    @Column(name = "entity_id") private UUID entityId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "before_json") private Map<String, Object> beforeJson;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "after_json") private Map<String, Object> afterJson;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    public UUID getId() { return id; } public Tournament getTournament() { return tournament; } public void setTournament(Tournament v) { tournament = v; }
    public AppUser getUser() { return user; } public void setUser(AppUser v) { user = v; } public String getAction() { return action; } public void setAction(String v) { action = v; }
    public String getEntityType() { return entityType; } public void setEntityType(String v) { entityType = v; } public UUID getEntityId() { return entityId; } public void setEntityId(UUID v) { entityId = v; }
    public Map<String, Object> getBeforeJson() { return beforeJson; } public void setBeforeJson(Map<String, Object> v) { beforeJson = v; }
    public Map<String, Object> getAfterJson() { return afterJson; } public void setAfterJson(Map<String, Object> v) { afterJson = v; } public OffsetDateTime getCreatedAt() { return createdAt; }
}
