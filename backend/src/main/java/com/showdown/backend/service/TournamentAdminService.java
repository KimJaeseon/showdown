package com.showdown.backend.service;

import com.showdown.backend.api.dto.CourtDtos.CourtRequest;
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
import com.showdown.backend.domain.AppRole;
import com.showdown.backend.domain.AppUser;
import com.showdown.backend.domain.Court;
import com.showdown.backend.domain.Division;
import com.showdown.backend.domain.Match;
import com.showdown.backend.domain.MatchEndReason;
import com.showdown.backend.domain.MatchSet;
import com.showdown.backend.domain.MatchSide;
import com.showdown.backend.domain.MatchStatus;
import com.showdown.backend.domain.Official;
import com.showdown.backend.domain.Player;
import com.showdown.backend.domain.Role;
import com.showdown.backend.domain.Stage;
import com.showdown.backend.domain.StageStatus;
import com.showdown.backend.domain.Tournament;
import com.showdown.backend.domain.TournamentGroup;
import com.showdown.backend.domain.TournamentPlayer;
import com.showdown.backend.repository.AppUserRepository;
import com.showdown.backend.repository.CourtRepository;
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
import com.showdown.backend.security.TournamentAccessGuard;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Map;
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
    private final CourtRepository courts;
    private final StageRepository stages;
    private final GroupRepository groups;
    private final MatchRepository matches;
    private final MatchSetRepository matchSets;
    private final MatchOfficialRepository matchOfficials;
    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final RankingService rankingService;
    private final AuditLogService auditLogService;
    private final TournamentAccessGuard accessGuard;

    public TournamentAdminService(
            TournamentRepository tournaments,
            DivisionRepository divisions,
            PlayerRepository players,
            TournamentPlayerRepository tournamentPlayers,
            OfficialRepository officials,
            CourtRepository courts,
            StageRepository stages,
            GroupRepository groups,
            MatchRepository matches,
            MatchSetRepository matchSets,
            MatchOfficialRepository matchOfficials,
            AppUserRepository users,
            RoleRepository roles,
            PasswordEncoder passwordEncoder,
            RankingService rankingService,
            AuditLogService auditLogService,
            TournamentAccessGuard accessGuard
    ) {
        this.tournaments = tournaments;
        this.divisions = divisions;
        this.players = players;
        this.tournamentPlayers = tournamentPlayers;
        this.officials = officials;
        this.courts = courts;
        this.stages = stages;
        this.groups = groups;
        this.matches = matches;
        this.matchSets = matchSets;
        this.matchOfficials = matchOfficials;
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.rankingService = rankingService;
        this.auditLogService = auditLogService;
        this.accessGuard = accessGuard;
    }

    public Tournament createTournament(TournamentRequest request) {
        accessGuard.requireSystemAdmin();
        Tournament tournament = new Tournament();
        applyTournament(tournament, request);
        return tournaments.save(tournament);
    }

    public Tournament updateTournament(UUID id, TournamentRequest request) {
        Tournament tournament = getTournament(id);
        accessGuard.requireTournamentAccess(tournament);
        if (request.status().ordinal() < tournament.getStatus().ordinal()) {
            throw new IllegalArgumentException("대회 상태를 역방향으로 변경하려면 별도의 재개방 절차가 필요합니다.");
        }
        if (request.status().ordinal() > tournament.getStatus().ordinal() + 1) {
            throw new IllegalArgumentException("대회 상태는 한 단계씩 변경해야 합니다.");
        }
        applyTournament(tournament, request);
        return tournament;
    }

    public void deleteTournament(UUID id) {
        Tournament tournament = getTournament(id);
        accessGuard.requireTournamentAccess(tournament);
        tournaments.delete(tournament);
    }

    public Division createDivision(UUID tournamentId, DivisionRequest request) {
        Tournament tournament = getTournament(tournamentId);
        accessGuard.requireTournamentAccess(tournament);
        Division division = new Division();
        division.setTournament(tournament);
        applyDivision(division, request);
        return divisions.save(division);
    }

    public Division updateDivision(UUID id, DivisionRequest request) {
        Division division = getDivision(id);
        accessGuard.requireTournamentAccess(division.getTournament());
        applyDivision(division, request);
        return division;
    }

    public void deleteDivision(UUID id) {
        Division division = getDivision(id);
        accessGuard.requireTournamentAccess(division.getTournament());
        divisions.delete(division);
    }

    public TournamentPlayer createTournamentPlayer(UUID tournamentId, TournamentPlayerRequest request) {
        Tournament tournament = getTournament(tournamentId);
        accessGuard.requireTournamentAccess(tournament);
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
        accessGuard.requireTournamentAccess(entry.getTournament());
        Division nextDivision = getDivision(request.divisionId());
        requireSameTournament(entry.getTournament(), nextDivision.getTournament(), "부문");
        applyPlayer(entry.getPlayer(), request);
        entry.setDivision(nextDivision);
        applyTournamentPlayer(entry, request);
        return entry;
    }

    public void deleteTournamentPlayer(UUID id) {
        TournamentPlayer entry = getTournamentPlayer(id);
        accessGuard.requireTournamentAccess(entry.getTournament());
        tournamentPlayers.delete(entry);
    }

    public Official createOfficial(UUID tournamentId, OfficialRequest request) {
        Tournament tournament = getTournament(tournamentId);
        accessGuard.requireTournamentAccess(tournament);
        Official official = new Official();
        official.setTournament(tournament);
        applyOfficial(official, request);
        return officials.save(official);
    }

    public Official updateOfficial(UUID id, OfficialRequest request) {
        Official official = getOfficial(id);
        accessGuard.requireTournamentAccess(official.getTournament());
        applyOfficial(official, request);
        return official;
    }

    public void deleteOfficial(UUID id) {
        Official official = getOfficial(id);
        accessGuard.requireTournamentAccess(official.getTournament());
        officials.delete(official);
    }

    public Court createCourt(UUID tournamentId, CourtRequest request) {
        Tournament tournament = getTournament(tournamentId);
        accessGuard.requireTournamentAccess(tournament);
        Court court = new Court();
        court.setTournament(tournament);
        applyCourt(court, request);
        return courts.save(court);
    }

    public Court updateCourt(UUID id, CourtRequest request) {
        Court court = getCourt(id);
        accessGuard.requireTournamentAccess(court.getTournament());
        applyCourt(court, request);
        return court;
    }

    public void deleteCourt(UUID id) {
        Court court = getCourt(id);
        accessGuard.requireTournamentAccess(court.getTournament());
        courts.delete(court);
    }

    public Stage createStage(UUID tournamentId, StageRequest request) {
        Tournament tournament = getTournament(tournamentId);
        accessGuard.requireTournamentAccess(tournament);
        Division division = getDivision(request.divisionId());
        requireSameTournament(tournament, division.getTournament(), "부문");
        Stage stage = new Stage();
        stage.setTournament(tournament);
        applyStage(stage, request);
        return stages.save(stage);
    }

    public Stage updateStage(UUID id, StageRequest request) {
        Stage stage = stages.findById(id).orElseThrow(() -> new EntityNotFoundException("단계를 찾을 수 없습니다."));
        accessGuard.requireTournamentAccess(stage.getTournament());
        requireSameTournament(stage.getTournament(), getDivision(request.divisionId()).getTournament(), "부문");
        StageStatus previousStatus = stage.getStatus();
        if (request.status() != null) {
            validateStageStatusTransition(previousStatus, request.status());
        }
        applyStage(stage, request);
        if (request.status() == StageStatus.FINISHED && previousStatus != StageStatus.FINISHED) {
            rankingService.recalculateStageFinal(stage);
        }
        return stage;
    }

    private void validateStageStatusTransition(StageStatus current, StageStatus next) {
        if (next.ordinal() < current.ordinal()) {
            throw new IllegalArgumentException("단계 상태를 역방향으로 변경하려면 별도의 재개방 절차가 필요합니다.");
        }
        if (next.ordinal() > current.ordinal() + 1) {
            throw new IllegalArgumentException("단계 상태는 한 단계씩 변경해야 합니다.");
        }
    }

    public void deleteStage(UUID id) {
        Stage stage = stages.findById(id).orElseThrow(() -> new EntityNotFoundException("단계를 찾을 수 없습니다."));
        accessGuard.requireTournamentAccess(stage.getTournament());
        stages.delete(stage);
    }

    public TournamentGroup createGroup(UUID tournamentId, GroupRequest request) {
        Tournament tournament = getTournament(tournamentId);
        accessGuard.requireTournamentAccess(tournament);
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
        accessGuard.requireTournamentAccess(group.getTournament());
        Division division = getDivision(request.divisionId());
        Stage stage = stages.findById(request.stageId()).orElseThrow(() -> new EntityNotFoundException("단계를 찾을 수 없습니다."));
        requireSameTournament(group.getTournament(), division.getTournament(), "부문");
        requireSameTournament(group.getTournament(), stage.getTournament(), "단계");
        if (!stage.getDivision().getId().equals(division.getId())) throw new IllegalArgumentException("단계와 조의 부문이 일치하지 않습니다.");
        applyGroup(group, request);
        return group;
    }

    public void deleteGroup(UUID id) {
        TournamentGroup group = getGroup(id);
        accessGuard.requireTournamentAccess(group.getTournament());
        groups.delete(group);
    }

    public Match createMatch(UUID tournamentId, MatchRequest request) {
        Tournament tournament = getTournament(tournamentId);
        accessGuard.requireTournamentAccess(tournament);
        validateMatchReferences(tournament, request);
        Match match = new Match();
        match.setTournament(tournament);
        applyMatch(match, request);
        return matches.save(match);
    }

    public Match updateMatch(UUID id, MatchRequest request) {
        Match match = getMatch(id);
        accessGuard.requireTournamentAccess(match.getTournament());
        validateMatchReferences(match.getTournament(), request);
        applyMatch(match, request);
        return match;
    }

    public void deleteMatch(UUID id) {
        Match match = getMatch(id);
        accessGuard.requireTournamentAccess(match.getTournament());
        matches.delete(match);
    }

    public Match updateMatchSets(UUID matchId, MatchSetsUpdateRequest request) {
        Match match = saveMatchSetDraft(matchId, request);
        return completeMatch(match, request.changeReason());
    }

    public Match saveMatchSetDraft(UUID matchId, MatchSetsUpdateRequest request) {
        Match match = getMatch(matchId);
        accessGuard.requireMatchScoringAccess(match);
        Map<String, Object> before = scoreState(match);
        if (!match.getVersion().equals(request.version())) {
            throw new ApiConflictException("경기 버전이 다릅니다. 최신 데이터를 다시 조회하세요.");
        }
        requireChangeReasonForResultEdit(match, request.changeReason());
        if (request.sets() == null) {
            throw new IllegalArgumentException("세트 점수 목록이 필요합니다.");
        }
        validateSetScores(request.sets(), match.getMaxSets());
        matchSets.deleteByMatchId(matchId);
        int player1Sets = 0;
        int player2Sets = 0;
        int player1Points = 0;
        int player2Points = 0;

        for (MatchSetRequest setRequest : request.sets()) {
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

        match.setPlayer1SetsWon(player1Sets);
        match.setPlayer2SetsWon(player2Sets);
        match.setPlayer1TotalPoints(player1Points);
        match.setPlayer2TotalPoints(player2Points);
        match.setWinner(null);
        match.setStatus(MatchStatus.RUNNING);
        match.setEndReason(MatchEndReason.NORMAL);
        match.setResultNote(cleanNote(request.changeReason()));
        matches.flush();
        auditLogService.record(match.getTournament(), "SCORE_DRAFT_SAVED", "match", match.getId(), before, scoreState(match));
        return match;
    }

    public Match confirmMatchResult(UUID matchId, Integer version, String changeReason) {
        Match match = getMatch(matchId);
        accessGuard.requireMatchScoringAccess(match);
        if (!match.getVersion().equals(version)) {
            throw new ApiConflictException("경기 버전이 다릅니다. 최신 데이터를 다시 조회하세요.");
        }
        requireChangeReasonForResultEdit(match, changeReason);
        return completeMatch(match, changeReason);
    }

    public Match finishMatchSpecial(UUID matchId, Integer version, MatchEndReason reason, MatchSide winnerSide, String note) {
        if (reason == null || reason == MatchEndReason.NORMAL) {
            throw new IllegalArgumentException("특수 종료 사유는 기권, 몰수패 또는 BYE여야 합니다.");
        }
        if (winnerSide == null) {
            throw new IllegalArgumentException("특수 종료 승자를 선택해야 합니다.");
        }
        Match match = getMatch(matchId);
        accessGuard.requireMatchScoringAccess(match);
        if (!match.getVersion().equals(version)) {
            throw new ApiConflictException("경기 버전이 다릅니다. 최신 데이터를 다시 조회하세요.");
        }
        requireChangeReasonForResultEdit(match, note);
        Map<String, Object> before = scoreState(match);

        // DEFAULT_LOSS와 BYE는 기존 세트/득점을 무효화한다. GIVING_UP은 이미 확정된 세트와 기득점을 유지한다.
        boolean voidsPriorSets = reason == MatchEndReason.DEFAULT_LOSS || reason == MatchEndReason.BYE;
        int winnerSetsAlready = winnerSide == MatchSide.PLAYER1 ? match.getPlayer1SetsWon() : match.getPlayer2SetsWon();
        int nextSetNo = getMatchSets(matchId).size() + 1;
        if (voidsPriorSets) {
            matchSets.deleteByMatchId(matchId);
            resetScore(match);
            winnerSetsAlready = 0;
            nextSetNo = 1;
        }

        match.setWinner(winnerSide == MatchSide.PLAYER1 ? match.getPlayer1() : match.getPlayer2());
        match.setStatus(MatchStatus.WALKOVER);
        match.setEndReason(reason);
        match.setResultNote(cleanNote(note));

        if (reason != MatchEndReason.BYE) {
            int neededWins = match.getMaxSets() / 2 + 1;
            for (int setNo = nextSetNo; winnerSetsAlready < neededWins; setNo++, winnerSetsAlready++) {
                createSyntheticSet(match, setNo, winnerSide);
            }
            applyScoreFromSets(match);
        }

        matches.flush();
        if (match.getGroup() != null) {
            rankingService.recalculate(match.getGroup());
        }
        auditLogService.record(match.getTournament(), "SPECIAL_RESULT_CONFIRMED", "match", match.getId(), before, scoreState(match));
        return match;
    }

    private Match completeMatch(Match match, String changeReason) {
        List<MatchSet> sets = getMatchSets(match.getId());
        if (sets.isEmpty()) {
            throw new IllegalArgumentException("확정할 세트 점수가 없습니다.");
        }
        int neededWins = match.getMaxSets() / 2 + 1;
        if (match.getPlayer1SetsWon() != neededWins && match.getPlayer2SetsWon() != neededWins) {
            throw new IllegalArgumentException(match.getMaxSets() + "세트제 경기의 승리 조건을 충족하지 못했습니다.");
        }
        match.setWinner(match.getPlayer1SetsWon() > match.getPlayer2SetsWon() ? match.getPlayer1() : match.getPlayer2());
        match.setStatus(MatchStatus.COMPLETED);
        match.setEndReason(MatchEndReason.NORMAL);
        match.setResultNote(cleanNote(changeReason));
        if (match.getGroup() != null) {
            rankingService.recalculate(match.getGroup());
        }
        auditLogService.record(match.getTournament(), "RESULT_CONFIRMED", "match", match.getId(),
                Map.of("status", MatchStatus.RUNNING.name()), scoreState(match));
        return match;
    }

    private Map<String, Object> scoreState(Match match) {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put("status", match.getStatus().name());
        state.put("player1SetsWon", match.getPlayer1SetsWon()); state.put("player2SetsWon", match.getPlayer2SetsWon());
        state.put("player1TotalPoints", match.getPlayer1TotalPoints()); state.put("player2TotalPoints", match.getPlayer2TotalPoints());
        state.put("winnerTournamentPlayerId", match.getWinner() == null ? null : match.getWinner().getId().toString());
        state.put("endReason", match.getEndReason().name());
        state.put("resultNote", match.getResultNote());
        return state;
    }

    private void createSyntheticSet(Match match, int setNo, MatchSide winnerSide) {
        MatchSet set = new MatchSet();
        set.setMatch(match);
        set.setSetNo(setNo);
        set.setPlayer1Score(winnerSide == MatchSide.PLAYER1 ? 11 : 0);
        set.setPlayer2Score(winnerSide == MatchSide.PLAYER2 ? 11 : 0);
        set.setWinnerSide(winnerSide);
        matchSets.save(set);
    }

    private void applyScoreFromSets(Match match) {
        int player1Sets = 0;
        int player2Sets = 0;
        int player1Points = 0;
        int player2Points = 0;
        for (MatchSet set : getMatchSets(match.getId())) {
            player1Points += set.getPlayer1Score();
            player2Points += set.getPlayer2Score();
            if (set.getWinnerSide() == MatchSide.PLAYER1) player1Sets++;
            if (set.getWinnerSide() == MatchSide.PLAYER2) player2Sets++;
        }
        match.setPlayer1SetsWon(player1Sets);
        match.setPlayer2SetsWon(player2Sets);
        match.setPlayer1TotalPoints(player1Points);
        match.setPlayer2TotalPoints(player2Points);
    }

    private void resetScore(Match match) {
        match.setPlayer1SetsWon(0);
        match.setPlayer2SetsWon(0);
        match.setPlayer1TotalPoints(0);
        match.setPlayer2TotalPoints(0);
    }

    private void requireChangeReasonForResultEdit(Match match, String changeReason) {
        boolean alreadyFinal = match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.WALKOVER;
        if (alreadyFinal && (changeReason == null || changeReason.isBlank())) {
            throw new IllegalArgumentException("확정된 경기 결과를 수정하려면 변경 사유가 필요합니다.");
        }
    }

    private String cleanNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private void validateSetScores(List<MatchSetRequest> sets, int maxSets) {
        if (sets.size() > maxSets) {
            throw new IllegalArgumentException(maxSets + "세트제 경기의 최대 세트 수를 초과했습니다.");
        }
        int neededWins = maxSets / 2 + 1;
        Set<Integer> numbers = new HashSet<>();
        int player1Wins = 0;
        int player2Wins = 0;
        for (int i = 0; i < sets.size(); i++) {
            MatchSetRequest set = sets.get(i);
            if (set.setNo() == null || set.setNo() != i + 1 || !numbers.add(set.setNo())) {
                throw new IllegalArgumentException("세트 번호는 1부터 중복 없이 연속되어야 합니다.");
            }
            if (set.player1Score() < 0 || set.player2Score() < 0 || set.player1Score().equals(set.player2Score())) {
                throw new IllegalArgumentException(set.setNo() + "세트 점수는 음수 또는 동점일 수 없습니다.");
            }
            int winner = Math.max(set.player1Score(), set.player2Score());
            int loser = Math.min(set.player1Score(), set.player2Score());
            if (winner < 11 || winner - loser < 2) {
                throw new IllegalArgumentException(set.setNo() + "세트는 최소 11점과 2점 차가 필요합니다.");
            }
            if (set.player1Score() > set.player2Score()) player1Wins++; else player2Wins++;
            if ((player1Wins == neededWins || player2Wins == neededWins) && i < sets.size() - 1) {
                throw new IllegalArgumentException("경기 승리 조건을 충족한 뒤 추가 세트를 입력할 수 없습니다.");
            }
        }
    }

    public AppUser createUser(UserRequest request) {
        accessGuard.requireSystemAdmin();
        AppUser user = new AppUser();
        applyUser(user, request);
        return users.save(user);
    }

    public AppUser updateUser(UUID id, UserRequest request) {
        accessGuard.requireSystemAdmin();
        AppUser user = users.findById(id).orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        applyUser(user, request);
        return user;
    }

    public void deleteUser(UUID id) {
        accessGuard.requireSystemAdmin();
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

    public Court getCourt(UUID id) {
        return courts.findById(id).orElseThrow(() -> new EntityNotFoundException("코트를 찾을 수 없습니다."));
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

    private void applyCourt(Court court, CourtRequest request) {
        court.setName(request.name());
        court.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        court.setActive(request.active() == null || request.active());
    }

    private void applyStage(Stage stage, StageRequest request) {
        stage.setDivision(getDivision(request.divisionId()));
        stage.setName(request.name());
        stage.setStageType(request.stageType());
        stage.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        stage.setStatus(request.status() == null ? stage.getStatus() : request.status());
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
        Court court = resolveCourt(match.getTournament(), request.courtId());
        match.setCourt(court);
        match.setCourtName(request.courtName() != null ? request.courtName() : (court == null ? null : court.getName()));
        match.setDurationMinutes(request.durationMinutes() == null ? 30 : request.durationMinutes());
        int maxSets = request.maxSets() == null ? 3 : request.maxSets();
        if (maxSets != 1 && maxSets != 3 && maxSets != 5) throw new IllegalArgumentException("경기 형식은 1·3·5세트제만 지원합니다.");
        match.setMaxSets(maxSets);
        match.setPlayer1(getTournamentPlayer(request.player1TournamentPlayerId()));
        match.setPlayer2(getTournamentPlayer(request.player2TournamentPlayerId()));
        if (request.player1TournamentPlayerId().equals(request.player2TournamentPlayerId())) {
            throw new IllegalArgumentException("같은 선수를 경기 양쪽에 배정할 수 없습니다.");
        }
        match.setStatus(request.status() == null ? MatchStatus.SCHEDULED : request.status());
        if (match.getStatus() != MatchStatus.WALKOVER) {
            match.setEndReason(MatchEndReason.NORMAL);
            match.setResultNote(null);
        }
        List<Official> assignedOfficials = resolveAssignedOfficials(match.getTournament(), request.refereeOfficialIds());
        validateScheduleConflicts(match, assignedOfficials);
        match.replaceOfficials(assignedOfficials);
    }

    private Court resolveCourt(Tournament tournament, UUID courtId) {
        if (courtId == null) {
            return null;
        }
        Court court = getCourt(courtId);
        if (!court.getTournament().getId().equals(tournament.getId())) {
            throw new IllegalArgumentException("경기와 같은 대회의 코트만 배정할 수 있습니다.");
        }
        if (!Boolean.TRUE.equals(court.getActive())) {
            throw new IllegalArgumentException("비활성 코트는 배정할 수 없습니다: " + court.getName());
        }
        return court;
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
        var localDate = targetStart.atZoneSameInstant(ZoneId.of(target.getTournament().getTimezone())).toLocalDate();
        if (localDate.isBefore(target.getTournament().getStartDate()) || localDate.isAfter(target.getTournament().getEndDate())) {
            throw new IllegalArgumentException("경기 시각은 대회 기간 안이어야 합니다.");
        }
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
            if (hasSameCourt(target, existing)) {
                throw new IllegalArgumentException("같은 시간대에 동일 코트를 중복 배정할 수 없습니다: " + target.getCourtName());
            }
            boolean hasOfficialConflict = existing.getMatchOfficials().stream()
                    .map(matchOfficial -> matchOfficial.getOfficial().getId())
                    .anyMatch(officialIds::contains);
            if (hasOfficialConflict) {
                throw new IllegalArgumentException("같은 시간대에 동일 심판을 중복 배정할 수 없습니다.");
            }
            Set<UUID> targetPlayers = Set.of(target.getPlayer1().getId(), target.getPlayer2().getId());
            if (targetPlayers.contains(existing.getPlayer1().getId()) || targetPlayers.contains(existing.getPlayer2().getId())) {
                throw new IllegalArgumentException("같은 시간대에 동일 선수를 중복 배정할 수 없습니다.");
            }
        }
    }

    /** TDD-24: 코트 엔티티가 배정된 경기는 court_id로, 그렇지 않은 경기는 기존처럼 courtName 문자열로 충돌을 비교한다. */
    private boolean hasSameCourt(Match target, Match existing) {
        if (target.getCourt() != null && existing.getCourt() != null) {
            return target.getCourt().getId().equals(existing.getCourt().getId());
        }
        return target.getCourtName() != null
                && existing.getCourtName() != null
                && target.getCourtName().equalsIgnoreCase(existing.getCourtName());
    }

    private boolean overlaps(OffsetDateTime leftStart, OffsetDateTime leftEnd, OffsetDateTime rightStart, OffsetDateTime rightEnd) {
        return leftStart.isBefore(rightEnd) && rightStart.isBefore(leftEnd);
    }

    private void validateMatchReferences(Tournament tournament, MatchRequest request) {
        Division division = getDivision(request.divisionId());
        Stage stage = stages.findById(request.stageId()).orElseThrow(() -> new EntityNotFoundException("단계를 찾을 수 없습니다."));
        TournamentPlayer player1 = getTournamentPlayer(request.player1TournamentPlayerId());
        TournamentPlayer player2 = getTournamentPlayer(request.player2TournamentPlayerId());
        if (player1.getStatus() != com.showdown.backend.domain.ParticipantStatus.ACTIVE
                || player2.getStatus() != com.showdown.backend.domain.ParticipantStatus.ACTIVE) {
            throw new IllegalArgumentException("기권 또는 실격 선수는 경기에 배정할 수 없습니다.");
        }
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
        user.setTournamentPlayer(request.tournamentPlayerId() == null ? null : getTournamentPlayer(request.tournamentPlayerId()));
        user.setOfficial(request.officialId() == null ? null : getOfficial(request.officialId()));
        Role role = roles.findByCode(request.role().getRoleCode())
                .orElseThrow(() -> new EntityNotFoundException("역할을 찾을 수 없습니다: " + request.role().getRoleCode()));
        if (request.role() == AppRole.TOURNAMENT_ADMIN) {
            if (request.tournamentId() == null) {
                throw new IllegalArgumentException("대회 관리자 역할은 대회를 지정해야 합니다.");
            }
            user.replaceRole(role, getTournament(request.tournamentId()));
        } else {
            if (request.tournamentId() != null) {
                throw new IllegalArgumentException("전역 역할에는 대회를 지정할 수 없습니다.");
            }
            user.replaceGlobalRole(role);
        }
        user.setActive(request.active() == null || request.active());
    }
}
