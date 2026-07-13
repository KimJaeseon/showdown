package com.showdown.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "users")
public class AppUser extends BaseEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tournament_player_id")
    private TournamentPlayer tournamentPlayer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "official_id")
    private Official official;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<UserRole> userRoles = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public AppRole getRole() {
        return userRoles.stream()
                .map(UserRole::getRole)
                .map(Role::getCode)
                .map(AppRole::fromRoleCode)
                .findFirst()
                .orElse(null);
    }

    public List<UserRole> getUserRoles() {
        return userRoles;
    }

    public Optional<UserRole> getGlobalUserRole() {
        return userRoles.stream()
                .filter(userRole -> userRole.getTournament() == null)
                .findFirst();
    }

    public void replaceGlobalRole(Role role) {
        replaceRole(role, null);
    }

    public void replaceRole(Role role, Tournament tournament) {
        userRoles.clear();
        UserRole userRole = new UserRole();
        userRole.setUser(this);
        userRole.setRole(role);
        userRole.setTournament(tournament);
        userRoles.add(userRole);
    }

    public UUID getRoleTournamentId() {
        return userRoles.isEmpty() || userRoles.get(0).getTournament() == null
                ? null
                : userRoles.get(0).getTournament().getId();
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public TournamentPlayer getTournamentPlayer() {
        return tournamentPlayer;
    }

    public void setTournamentPlayer(TournamentPlayer tournamentPlayer) {
        this.tournamentPlayer = tournamentPlayer;
    }

    public Official getOfficial() {
        return official;
    }

    public void setOfficial(Official official) {
        this.official = official;
    }
}
