package com.showdown.backend.api;

import com.showdown.backend.api.dto.DivisionDtos.DivisionResponse;
import com.showdown.backend.api.dto.GroupDtos.GroupResponse;
import com.showdown.backend.api.dto.MatchDtos.MatchResponse;
import com.showdown.backend.api.dto.MatchDtos.MatchSetResponse;
import com.showdown.backend.api.dto.OfficialDtos.OfficialResponse;
import com.showdown.backend.api.dto.PlayerDtos.TournamentPlayerResponse;
import com.showdown.backend.api.dto.RankingDtos.RankingResponse;
import com.showdown.backend.api.dto.StageDtos.StageResponse;
import com.showdown.backend.api.dto.TournamentDtos.TournamentResponse;
import com.showdown.backend.api.dto.UserDtos.UserResponse;
import com.showdown.backend.domain.AppUser;
import com.showdown.backend.domain.Division;
import com.showdown.backend.domain.Match;
import com.showdown.backend.domain.MatchOfficial;
import com.showdown.backend.domain.MatchSet;
import com.showdown.backend.domain.Official;
import com.showdown.backend.domain.Player;
import com.showdown.backend.domain.RankingSnapshot;
import com.showdown.backend.domain.Stage;
import com.showdown.backend.domain.Tournament;
import com.showdown.backend.domain.TournamentGroup;
import com.showdown.backend.domain.TournamentPlayer;
import java.util.List;

public final class ApiMapper {
    private ApiMapper() {
    }

    public static TournamentResponse toTournamentResponse(Tournament tournament) {
        return new TournamentResponse(
                tournament.getId(),
                tournament.getCode(),
                tournament.getName(),
                tournament.getLocation(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getTimezone(),
                tournament.getStatus(),
                tournament.getDefaultLanguage()
        );
    }

    public static DivisionResponse toDivisionResponse(Division division) {
        return new DivisionResponse(
                division.getId(),
                division.getTournament().getId(),
                division.getName(),
                division.getCode(),
                division.getCategory(),
                division.getSortOrder(),
                division.getActive()
        );
    }

    public static TournamentPlayerResponse toTournamentPlayerResponse(TournamentPlayer entry) {
        Player player = entry.getPlayer();
        return new TournamentPlayerResponse(
                entry.getId(),
                entry.getTournament().getId(),
                entry.getDivision().getId(),
                player.getId(),
                player.getDisplayName(),
                player.getCountryCode(),
                entry.getSeedNo(),
                entry.getEntryNo(),
                entry.getDisplayNameOverride(),
                entry.getClubName(),
                entry.getStatus()
        );
    }

    public static OfficialResponse toOfficialResponse(Official official) {
        return new OfficialResponse(
                official.getId(),
                official.getTournament().getId(),
                official.getName(),
                official.getShortCode(),
                official.getRoleName(),
                official.getActive()
        );
    }

    public static StageResponse toStageResponse(Stage stage) {
        return new StageResponse(
                stage.getId(),
                stage.getTournament().getId(),
                stage.getDivision().getId(),
                stage.getName(),
                stage.getStageType(),
                stage.getSortOrder()
        );
    }

    public static GroupResponse toGroupResponse(TournamentGroup group) {
        return new GroupResponse(
                group.getId(),
                group.getTournament().getId(),
                group.getDivision().getId(),
                group.getStage().getId(),
                group.getCode(),
                group.getName(),
                group.getGroupType(),
                group.getSortOrder()
        );
    }

    public static MatchResponse toMatchResponse(Match match, List<MatchSet> sets) {
        TournamentPlayer player1 = match.getPlayer1();
        TournamentPlayer player2 = match.getPlayer2();
        return new MatchResponse(
                match.getId(),
                match.getTournament().getId(),
                match.getDivision().getId(),
                match.getStage().getId(),
                match.getGroup() == null ? null : match.getGroup().getId(),
                match.getMatchNo(),
                match.getScheduledAt(),
                match.getCourtName(),
                match.getDurationMinutes(),
                match.getMaxSets(),
                refereeNames(match).isEmpty() ? null : String.join(", ", refereeNames(match)),
                match.getMatchOfficials().stream()
                        .map(MatchOfficial::getOfficial)
                        .map(Official::getId)
                        .toList(),
                refereeNames(match),
                player1 == null ? null : player1.getId(),
                player1 == null ? null : displayName(player1),
                player2 == null ? null : player2.getId(),
                player2 == null ? null : displayName(player2),
                match.getWinner() == null ? null : match.getWinner().getId(),
                match.getStatus(),
                match.getEndReason(),
                match.getResultNote(),
                match.getPlayer1SetsWon(),
                match.getPlayer2SetsWon(),
                match.getPlayer1TotalPoints(),
                match.getPlayer2TotalPoints(),
                match.getVersion(),
                sets.stream().map(ApiMapper::toMatchSetResponse).toList()
        );
    }

    public static MatchSetResponse toMatchSetResponse(MatchSet set) {
        return new MatchSetResponse(
                set.getId(),
                set.getSetNo(),
                set.getPlayer1Score(),
                set.getPlayer2Score(),
                set.getWinnerSide()
        );
    }

    public static UserResponse toUserResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getTournamentPlayer() == null ? null : user.getTournamentPlayer().getId(),
                user.getRoleTournamentId(),
                user.getOfficial() == null ? null : user.getOfficial().getId(),
                user.getActive()
        );
    }

    public static RankingResponse toRankingResponse(RankingSnapshot ranking) {
        return new RankingResponse(ranking.getId(), ranking.getTournament().getId(), ranking.getDivision().getId(),
                ranking.getStage() == null ? null : ranking.getStage().getId(),
                ranking.getGroup() == null ? null : ranking.getGroup().getId(), ranking.getTournamentPlayer().getId(),
                ranking.getRankNo(), ranking.getMatchesPlayed(), ranking.getWins(), ranking.getLosses(),
                ranking.getMatchPoints(), ranking.getSetsWon(), ranking.getSetsLost(), ranking.getSetDifference(),
                ranking.getPointsFor(), ranking.getPointsAgainst(), ranking.getPointDifference(),
                ranking.getTieBreakNote(), ranking.getCalculatedAt());
    }

    private static String displayName(TournamentPlayer entry) {
        if (entry.getDisplayNameOverride() != null && !entry.getDisplayNameOverride().isBlank()) {
            return entry.getDisplayNameOverride();
        }
        return entry.getPlayer().getDisplayName();
    }

    private static List<String> refereeNames(Match match) {
        return match.getMatchOfficials().stream()
                .sorted((left, right) -> left.getPositionNo().compareTo(right.getPositionNo()))
                .map(MatchOfficial::getOfficial)
                .map(Official::getName)
                .toList();
    }
}
