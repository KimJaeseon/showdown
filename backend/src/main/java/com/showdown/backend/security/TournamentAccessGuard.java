package com.showdown.backend.security;

import com.showdown.backend.domain.AppUser;
import com.showdown.backend.domain.Match;
import com.showdown.backend.domain.Tournament;
import com.showdown.backend.repository.AppUserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TournamentAccessGuard {
    private static final String ROLE_SYSTEM_ADMIN = "ROLE_SYSTEM_ADMIN";
    private static final String ROLE_LEGACY_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_TOURNAMENT_ADMIN = "ROLE_TOURNAMENT_ADMIN";
    private static final String ROLE_SCORER = "ROLE_SCORER";
    private static final String TOURNAMENT_ADMIN_CODE = "tournament_admin";

    private final AppUserRepository users;

    public TournamentAccessGuard(AppUserRepository users) {
        this.users = users;
    }

    public void requireTournamentAccess(Tournament tournament) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || hasGlobalAdminAuthority(authentication)) {
            return;
        }
        if (!hasAuthority(authentication, ROLE_TOURNAMENT_ADMIN)) {
            return;
        }
        AppUser user = users.findByEmailAndActiveTrue(authentication.getName()).orElse(null);
        boolean scopedToTournament = user != null && user.getUserRoles().stream()
                .anyMatch(userRole -> TOURNAMENT_ADMIN_CODE.equals(userRole.getRole().getCode())
                        && userRole.getTournament() != null
                        && userRole.getTournament().getId().equals(tournament.getId()));
        if (!scopedToTournament) {
            throw new AccessDeniedException("다른 대회의 데이터는 수정할 수 없습니다.");
        }
    }

    public void requireMatchScoringAccess(Match match) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || hasGlobalAdminAuthority(authentication)) {
            return;
        }
        if (hasAuthority(authentication, ROLE_TOURNAMENT_ADMIN)) {
            requireTournamentAccess(match.getTournament());
            return;
        }
        if (!hasAuthority(authentication, ROLE_SCORER)) {
            return;
        }
        AppUser user = users.findByEmailAndActiveTrue(authentication.getName()).orElse(null);
        boolean assigned = user != null && user.getOfficial() != null && match.getMatchOfficials().stream()
                .anyMatch(matchOfficial -> matchOfficial.getOfficial().getId().equals(user.getOfficial().getId()));
        if (!assigned) {
            throw new AccessDeniedException("배정된 경기만 점수를 입력할 수 있습니다.");
        }
    }

    public void requireSystemAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && hasGlobalAdminAuthority(authentication)) {
            return;
        }
        throw new AccessDeniedException("시스템 관리자만 수행할 수 있는 작업입니다.");
    }

    private boolean hasGlobalAdminAuthority(Authentication authentication) {
        return hasAuthority(authentication, ROLE_SYSTEM_ADMIN) || hasAuthority(authentication, ROLE_LEGACY_ADMIN);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream().anyMatch(granted -> granted.getAuthority().equals(authority));
    }
}
