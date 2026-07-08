package com.showdown.backend.service;

import com.showdown.backend.api.dto.DivisionDtos.DivisionRequest;
import com.showdown.backend.api.dto.GroupDtos.GroupRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchSetRequest;
import com.showdown.backend.api.dto.MatchDtos.MatchSetsUpdateRequest;
import com.showdown.backend.api.dto.OfficialDtos.OfficialRequest;
import com.showdown.backend.api.dto.PlayerDtos.TournamentPlayerRequest;
import com.showdown.backend.api.dto.StageDtos.StageRequest;
import com.showdown.backend.api.dto.TournamentDtos.TournamentRequest;
import com.showdown.backend.api.dto.UserDtos.UserRequest;
import com.showdown.backend.api.ApiConflictException;
import com.showdown.backend.domain.AppUser;
import com.showdown.backend.domain.Division;
import com.showdown.backend.domain.Match;
import com.showdown.backend.domain.MatchSet;
import com.showdown.backend.domain.MatchSide;
import com.showdown.backend.domain.MatchStatus;
import com.showdown.backend.domain.Official;
import com.showdown.backend.domain.Player;
import com.showdown.backend.domain.Role;
import com.showdown.backend.domain.Stage;
import com.showdown.backend.domain.Tournament;
import com.showdown.backend.domain.TournamentGroup;
import com.showdown.backend.domain.TournamentPlayer;
import com.showdown.backend.repository.AppUserRepository;
import com.showdown.backend.repository.DivisionRepository;
import com.showdown.backend.repository.GroupRepository;
import com.showdown.backend.repository.MatchRepository;
import com.showdown.backend.repository.MatchSetRepository;
import com.showdown.backend.repository.MatchOfficialRepository;
import com.showdown.backend.repository.OfficialRepository;
import com.showdown.backend.repository.PlayerRepository;
import com.showdown.backend.repository.RoleRepository;
import com.showdown.backend.repository.StageRepository;
import com.showdown.backend.repository.TournamentPlayerRepository;
import com.showdown.backend.repository.TournamentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TournamentAdminService {
    private final TournamentRepository tournaments;
    private final DivisionRepository divisions;
    private final PlayerRepository players;
    private final TournamentPlayerRepository tournamentPlayers;
    private final OfficialRepository officials;
    private final StageRepository stages;
    private final GroupRepository groups;
    private final MatchRepository matches;
    private final MatchSetRepository matchSets;
    private final MatchOfficialRepository matchOfficials;
    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public TournamentAdminService(
            TournamentRepository tournaments,
            DivisionRepository divisions,
            PlayerRepository players,
            TournamentPlayerRepository tournamentPlayers,
            OfficialRepository officials,
            StageRepository stages,
            GroupRepository groups,
            MatchRepository matches,
            MatchSetRepository matchSets,
            MatchOfficialRepository matchOfficials,
            AppUserRepository users,
            RoleRepository roles,
            PasswordEncoder passwordEncoder
    ) {
        this.tournaments = tournaments;
        this.divisions = divisions;
        this.players = players;
        this.tournamentPlayers = tournamentPlayers;
        this.officials = officials;
        this.stages = stages;
        this.groups = groups;
        this.matches = matches;
        this.matchSets = matchSets;
        this.matchOfficials = matchOfficials;
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    public Tournament createTournament(TournamentRequest request) {
        Tournament tournament = new Tournament();
        applyTournament(tournament, request);
        return tournaments.save(tournament);
    }

    public Tournament updateTournament(UUID id, TournamentRequest request) {
        Tournament tournament = getTournament(id);
        applyTournament(tournament, request);
        return tournament;
    }

    public void deleteTournament(UUID id) {
        tournaments.delete(getTournament(id));
    }

    public Division createDivision(UUID tournamentId, DivisionRequest request) {
        Division division = new Division();
        division.setTournament(getTournament(tournamentId));
        applyDivision(division, request);
        return divisions.save(division);
    }

    public Division updateDivision(UUID id, DivisionRequest request) {
        Division division = getDivision(id);
        applyDivision(division, request);
        return division;
    }

    public void deleteDivision(UUID id) {
        divisions.delete(getDivision(id));
    }

    public TournamentPlayer createTournamentPlayer(UUID tournamentId, TournamentPlayerRequest request) {
        Tournament tournament = getTournament(tournamentId);
        Division division = getDivision(request.divisionId());
        requireSameTournament(tournament, division.getTournament(), "부문");
        Player player = new Player();
        applyPlayer(player, request);
        players.save(player);

        TournamentPlayer entry = new TournamentPlayer();
        entry.setTournament(tournament);
        entry.setDivision(division);
        entry.setPlayer(player);
        applyTournamentPlayer(entry, request);
        return tournamentPlayers.save(entry);
    }

    public TournamentPlayer updateTournamentPlayer(UUID id, TournamentPlayerRequest request) {
        TournamentPlayer entry = getTournamentPlayer(id);
        applyPlayer(entry.getPlayer(), request);
        entry.setDivision(getDivision(request.divisionId()));
        applyTournamentPlayer(entry, request);
        return entry;
    }

    public void deleteTournamentPlayer(UUID id) {
        tournamentPlayers.delete(getTournamentPlayer(id));
    }

    public Official createOfficial(UUID tournamentId, OfficialRequest request) {
        Official official = new Official();
        official.setTournament(getTournament(tournamentId));
        applyOfficial(official, request);
        return officials.save(official);
    }

    public Official updateOfficial(UUID id, OfficialRequest request) {
        Official official = getOfficial(id);
        applyOfficial(official, request);
        return official;
    }

    public void deleteOfficial(UUID id) {
        officials.delete(getOfficial(id));
    }

    public Stage createStage(UUID tournamentId, StageRequest request) {
        Tournament tournament = getTournament(tournamentId);
        Division division = getDivision(request.divisionId());
        requireSameTournament(tournament, division.getTournament(), "부문");
        Stage stage = new Stage();
        stage.setTournament(tournament);
        applyStage(stage, request);
        return stages.save(stage);
    }

    public Stage updateStage(UUID id, StageRequest request) {
        Stage stage = stages.findById(id).orElseThrow(() -> new EntityNotFoundException("단계를 찾을 수 없습니다."));
        applyStage(stage, request);
        return stage;
    }

    public void deleteStage(UUID id) {
        stages.delete(stages.findById(id).orElseThrow(() -> new EntityNotFoundException("단계를 찾을 수 없습니다.")));
    }

    public TournamentGroup createGroup(UUID tournamentId, GroupRequest request) {
        Tournament tournament = getTournament(tournamentId);
        Division division = getDivision(request.divisionId());
        Stage stage = stages.findById(request.stageId()).orElseThrow(() -> new EntityNotFoundException("단계를 찾을 수 없습니다."));
        requireSameTournament(tournament, division.getTournament(), "부문");
        requireSameTournament(tournament, stage.getTournament(), "단계");
        if (!stage.getDivision().getId().equals(division.getId())) {
            throw new IllegalArgumentException("단계와 조의 부문이 일치하지 않습니다.");
        }
        TournamentGroup group = new TournamentGroup();
        group.setTournament(tournament);
        applyGroup(group, request);
        return groups.save(group);
    }

    public TournamentGroup updateGroup(UUID id, GroupRequest request) {
        TournamentGroup group = getGroup(id);
        applyGroup(group, request);
        return group;
    }

    public void deleteGroup(UUID id) {
        groups.delete(getGroup(id));
    }

    public Match createMatch(UUID tournamentId, MatchRequest request) {
        validateMatchReferences(getTournament(tournamentId), request);
        Match match = new Match();
        match.setTournament(getTournament(tournamentId));
        applyMatch(match, request);
        return matches.save(match);
    }

    public Match updateMatch(UUID id, MatchRequest request) {
        Match match = getMatch(id);
        applyMatch(match, request);
        return match;
    }

    public void deleteMatch(UUID id) {
        matches.delete(getMatch(id));
    }

    public Match updateMatchSets(UUID matchId, MatchSetsUpdateRequest request) {
        Match match = getMatch(matchId);
        if (!match.getVersion().equals(request.version())) {
            throw new ApiConflictException("경기 버전이 다릅니다. 최신 데이터를 다시 조회하세요.");
        }
        if (request.sets() == null || request.sets().isEmpty()) {
            throw new IllegalArgumentException("세트 점수는 최소 1개 이상이어야 합니다.");
        }

        matchSets.deleteByMatchId(matchId);
        int player1Sets = 0;
        int player2Sets = 0;
        int player1Points = 0;
        int player2Points = 0;

        for (MatchSetRequest setRequest : request.sets()) {
            if (setRequest.player1Score().equals(setRequest.player2Score())) {
                throw new IllegalArgumentException(setRequest.setNo() + "세트는 동점일 수 없습니다.");
            }
            MatchSet set = new MatchSet();
            set.setMatch(match);
            set.setSetNo(setRequest.setNo());
            set.setPlayer1Score(setRequest.player1Score());
            set.setPlayer2Score(setRequest.player2Score());
            set.setWinnerSide(setRequest.player1Score() > setRequest.player2Score() ? MatchSide.PLAYER1 : MatchSide.PLAYER2);
            matchSets.save(set);

            player1Points += setRequest.player1Score();
            player2Points += setRequest.player2Score();
            if (set.getWinnerSide() == MatchSide.PLAYER1) {
                player1Sets++;
            } else {
                player2Sets++;
            }
        }

        if (player1Sets == player2Sets) {
            throw new IllegalArgumentException("전체 세트 승수가 동률입니다.");
        }

        match.setPlayer1SetsWon(player1Sets);
        match.setPlayer2SetsWon(player2Sets);
        match.setPlayer1TotalPoints(player1Points);
        match.setPlayer2TotalPoints(player2Points);
        match.setWinner(player1Sets > player2Sets ? match.getPlayer1() : match.getPlayer2());
        match.setStatus(MatchStatus.COMPLETED);
        return match;
    }

    public AppUser createUser(UserRequest request) {
        AppUser user = new AppUser();
        applyUser(user, request);
        return users.save(user);
    }

    public AppUser updateUser(UUID id, UserRequest request) {
        AppUser user = users.findById(id).orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        applyUser(user, request);
        return user;
    }

    public void deleteUser(UUID id) {
        users.deleteById(id);
    }

    public Tournament getTournament(UUID id) {
        return tournaments.findById(id).orElseThrow(() -> new EntityNotFoundException("대회를 찾을 수 없습니다."));
    }

    public Division getDivision(UUID id) {
        return divisions.findById(id).orElseThrow(() -> new EntityNotFoundException("부문을 찾을 수 없습니다."));
    }

    public TournamentPlayer getTournamentPlayer(UUID id) {
        return tournamentPlayers.findById(id).orElseThrow(() -> new EntityNotFoundException("참가 선수를 찾을 수 없습니다."));
    }

    public Official getOfficial(UUID id) {
        return officials.findById(id).orElseThrow(() -> new EntityNotFoundException("심판을 찾을 수 없습니다."));
    }

    public TournamentGroup getGroup(UUID id) {
        return groups.findById(id).orElseThrow(() -> new EntityNotFoundException("조를 찾을 수 없습니다."));
    }

    public Match getMatch(UUID id) {
        return matches.findById(id).orElseThrow(() -> new EntityNotFoundException("경기를 찾을 수 없습니다."));
    }

    public List<MatchSet> getMatchSets(UUID matchId) {
        return matchSets.findByMatchIdOrderBySetNoAsc(matchId);
    }

    private void applyTournament(Tournament tournament, TournamentRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
        tournament.setCode(request.code());
        tournament.setName(request.name());
        tournament.setLocation(request.location());
        tournament.setStartDate(request.startDate());
        tournament.setEndDate(request.endDate());
        tournament.setTimezone(request.timezone());
        tournament.setStatus(request.status());
        tournament.setDefaultLanguage(request.defaultLanguage());
    }

    private void applyDivision(Division division, DivisionRequest request) {
        division.setName(request.name());
        division.setCode(request.code());
        division.setCategory(request.category());
        division.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        division.setActive(request.active() == null || request.active());
    }

    private void applyPlayer(Player player, TournamentPlayerRequest request) {
        player.setDisplayName(request.displayName());
        player.setFamilyName(request.familyName());
        player.setGivenName(request.givenName());
        player.setCountryCode(request.countryCode());
    }

    private void applyTournamentPlayer(TournamentPlayer entry, TournamentPlayerRequest request) {
        entry.setSeedNo(request.seedNo());
        entry.setEntryNo(request.entryNo());
        entry.setDisplayNameOverride(request.displayNameOverride());
        entry.setClubName(request.clubName());
        entry.setStatus(request.status() == null ? entry.getStatus() : request.status());
    }

    private void applyOfficial(Official official, OfficialRequest request) {
        official.setName(request.name());
        official.setShortCode(request.shortCode());
        official.setRoleName(request.roleName());
        official.setActive(request.active() == null || request.active());
    }

    private void applyStage(Stage stage, StageRequest request) {
        stage.setDivision(getDivision(request.divisionId()));
        stage.setName(request.name());
        stage.setStageType(request.stageType());
        stage.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private void applyGroup(TournamentGroup group, GroupRequest request) {
        group.setDivision(getDivision(request.divisionId()));
        group.setStage(stages.findById(request.stageId()).orElseThrow(() -> new EntityNotFoundException("단계를 찾을 수 없습니다.")));
        group.setCode(request.code());
        group.setName(request.name());
        group.setGroupType(request.groupType());
        group.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private void applyMatch(Match match, MatchRequest request) {
        match.setDivision(getDivision(request.divisionId()));
        match.setStage(stages.findById(request.stageId()).orElseThrow(() -> new EntityNotFoundException("단계를 찾을 수 없습니다.")));
        match.setGroup(request.groupId() == null ? null : getGroup(request.groupId()));
        match.setMatchNo(request.matchNo());
        match.setScheduledAt(request.scheduledAt());
        match.setCourtName(request.courtName());
        match.setDurationMinutes(request.durationMinutes() == null ? 30 : request.durationMinutes());
        match.setPlayer1(getTournamentPlayer(request.player1TournamentPlayerId()));
        match.setPlayer2(getTournamentPlayer(request.player2TournamentPlayerId()));
        if (request.player1TournamentPlayerId().equals(request.player2TournamentPlayerId())) {
            throw new IllegalArgumentException("같은 선수를 경기 양쪽에 배정할 수 없습니다.");
        }
        match.setStatus(request.status() == null ? MatchStatus.SCHEDULED : request.status());
        List<Official> assignedOfficials = resolveAssignedOfficials(match.getTournament(), request.refereeOfficialIds());
        validateScheduleConflicts(match, assignedOfficials);
        match.replaceOfficials(assignedOfficials);
    }

    private List<Official> resolveAssignedOfficials(Tournament tournament, List<UUID> officialIds) {
        if (officialIds == null || officialIds.size() != 2) {
            throw new IllegalArgumentException("경기마다 심판 2명을 배정해야 합니다.");
        }
        if (new HashSet<>(officialIds).size() != 2) {
            throw new IllegalArgumentException("동일한 심판을 한 경기에 중복 배정할 수 없습니다.");
        }
        List<Official> assignedOfficials = officialIds.stream().map(this::getOfficial).toList();
        for (Official official : assignedOfficials) {
            if (!official.getTournament().getId().equals(tournament.getId())) {
                throw new IllegalArgumentException("경기와 같은 대회의 심판만 배정할 수 있습니다.");
            }
            if (!Boolean.TRUE.equals(official.getActive())) {
                throw new IllegalArgumentException("비활성 심판은 배정할 수 없습니다: " + official.getName());
            }
        }
        return assignedOfficials;
    }

    private void validateScheduleConflicts(Match target, List<Official> assignedOfficials) {
        if (target.getScheduledAt() == null) {
            return;
        }
        OffsetDateTime targetStart = target.getScheduledAt();
        OffsetDateTime targetEnd = targetStart.plusMinutes(target.getDurationMinutes() == null ? 30 : target.getDurationMinutes());
        Set<UUID> officialIds = assignedOfficials.stream().map(Official::getId).collect(java.util.stream.Collectors.toSet());

        for (Match existing : matches.findByTournamentIdAndScheduledAtIsNotNullOrderByScheduledAtAscMatchNoAsc(target.getTournament().getId())) {
            if (target.getId() != null && target.getId().equals(existing.getId())) {
                continue;
            }
            OffsetDateTime existingStart = existing.getScheduledAt();
            OffsetDateTime existingEnd = existingStart.plusMinutes(existing.getDurationMinutes() == null ? 30 : existing.getDurationMinutes());
            if (!overlaps(targetStart, targetEnd, existingStart, existingEnd)) {
                continue;
            }
            if (target.getCourtName() != null
                    && existing.getCourtName() != null
                    && target.getCourtName().equalsIgnoreCase(existing.getCourtName())) {
                throw new IllegalArgumentException("같은 시간대에 동일 코트를 중복 배정할 수 없습니다: " + target.getCourtName());
            }
            boolean hasOfficialConflict = existing.getMatchOfficials().stream()
                    .map(matchOfficial -> matchOfficial.getOfficial().getId())
                    .anyMatch(officialIds::contains);
            if (hasOfficialConflict) {
                throw new IllegalArgumentException("같은 시간대에 동일 심판을 중복 배정할 수 없습니다.");
            }
        }
    }

    private boolean overlaps(OffsetDateTime leftStart, OffsetDateTime leftEnd, OffsetDateTime rightStart, OffsetDateTime rightEnd) {
        return leftStart.isBefore(rightEnd) && rightStart.isBefore(leftEnd);
    }

    private void validateMatchReferences(Tournament tournament, MatchRequest request) {
        Division division = getDivision(request.divisionId());
        Stage stage = stages.findById(request.stageId()).orElseThrow(() -> new EntityNotFoundException("단계를 찾을 수 없습니다."));
        TournamentPlayer player1 = getTournamentPlayer(request.player1TournamentPlayerId());
        TournamentPlayer player2 = getTournamentPlayer(request.player2TournamentPlayerId());
        requireSameTournament(tournament, division.getTournament(), "부문");
        requireSameTournament(tournament, stage.getTournament(), "단계");
        requireSameTournament(tournament, player1.getTournament(), "선수 1");
        requireSameTournament(tournament, player2.getTournament(), "선수 2");
        if (!stage.getDivision().getId().equals(division.getId())
                || !player1.getDivision().getId().equals(division.getId())
                || !player2.getDivision().getId().equals(division.getId())) {
            throw new IllegalArgumentException("경기의 단계와 선수는 같은 부문에 속해야 합니다.");
        }
        if (request.groupId() != null) {
            TournamentGroup group = getGroup(request.groupId());
            requireSameTournament(tournament, group.getTournament(), "조");
            if (!group.getStage().getId().equals(stage.getId()) || !group.getDivision().getId().equals(division.getId())) {
                throw new IllegalArgumentException("경기의 조, 단계와 부문이 일치하지 않습니다.");
            }
        }
    }

    private void requireSameTournament(Tournament expected, Tournament actual, String resourceName) {
        if (!expected.getId().equals(actual.getId())) {
            throw new IllegalArgumentException(resourceName + "이(가) 요청한 대회에 속하지 않습니다.");
        }
    }

    private void applyUser(AppUser user, UserRequest request) {
        user.setEmail(request.email());
        user.setDisplayName(request.displayName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        Role role = roles.findByCode(request.role().getRoleCode())
                .orElseThrow(() -> new EntityNotFoundException("역할을 찾을 수 없습니다: " + request.role().getRoleCode()));
        user.replaceGlobalRole(role);
        user.setActive(request.active() == null || request.active());
    }
}
