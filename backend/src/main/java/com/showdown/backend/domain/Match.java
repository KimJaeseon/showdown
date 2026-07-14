package com.showdown.backend.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "matches")
public class Match extends BaseEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "division_id")
    private Division division;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "stage_id")
    private Stage stage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id")
    private TournamentGroup group;

    @Column(name = "match_no", nullable = false)
    private Integer matchNo;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "court_name", length = 80)
    private String courtName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id")
    private Court court;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "max_sets", nullable = false)
    private Integer maxSets = 3;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "winner_tournament_player_id")
    private TournamentPlayer winner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_source_match_id")
    private Match player1SourceMatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_source_match_id")
    private Match player2SourceMatch;

    @Column(nullable = false, length = 30)
    @ColumnTransformer(write = "?::match_status")
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Column(name = "end_reason", nullable = false, length = 30)
    @ColumnTransformer(write = "?::match_end_reason")
    private MatchEndReason endReason = MatchEndReason.NORMAL;

    @Column(name = "result_note")
    private String resultNote;

    @Column(name = "player1_sets_won", nullable = false)
    private Integer player1SetsWon = 0;

    @Column(name = "player2_sets_won", nullable = false)
    private Integer player2SetsWon = 0;

    @Column(name = "player1_total_points", nullable = false)
    private Integer player1TotalPoints = 0;

    @Column(name = "player2_total_points", nullable = false)
    private Integer player2TotalPoints = 0;

    @Version
    private Integer version = 1;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MatchPlayer> matchPlayers = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MatchOfficial> matchOfficials = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Division getDivision() {
        return division;
    }

    public void setDivision(Division division) {
        this.division = division;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public TournamentGroup getGroup() {
        return group;
    }

    public void setGroup(TournamentGroup group) {
        this.group = group;
    }

    public Integer getMatchNo() {
        return matchNo;
    }

    public void setMatchNo(Integer matchNo) {
        this.matchNo = matchNo;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(OffsetDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getCourtName() {
        return courtName;
    }

    public void setCourtName(String courtName) {
        this.courtName = courtName;
    }

    public Court getCourt() {
        return court;
    }

    public void setCourt(Court court) {
        this.court = court;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getMaxSets() { return maxSets; }
    public void setMaxSets(Integer maxSets) { this.maxSets = maxSets; }

    public TournamentPlayer getPlayer1() {
        return getMatchPlayer(MatchSide.PLAYER1)
                .map(MatchPlayer::getTournamentPlayer)
                .orElse(null);
    }

    public void setPlayer1(TournamentPlayer player1) {
        putMatchPlayer(MatchSide.PLAYER1, player1);
    }

    public TournamentPlayer getPlayer2() {
        return getMatchPlayer(MatchSide.PLAYER2)
                .map(MatchPlayer::getTournamentPlayer)
                .orElse(null);
    }

    public void setPlayer2(TournamentPlayer player2) {
        putMatchPlayer(MatchSide.PLAYER2, player2);
    }

    public TournamentPlayer getWinner() {
        return winner;
    }

    public void setWinner(TournamentPlayer winner) {
        this.winner = winner;
    }

    public Match getPlayer1SourceMatch() {
        return player1SourceMatch;
    }

    public void setPlayer1SourceMatch(Match player1SourceMatch) {
        this.player1SourceMatch = player1SourceMatch;
    }

    public Match getPlayer2SourceMatch() {
        return player2SourceMatch;
    }

    public void setPlayer2SourceMatch(Match player2SourceMatch) {
        this.player2SourceMatch = player2SourceMatch;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public MatchEndReason getEndReason() {
        return endReason;
    }

    public void setEndReason(MatchEndReason endReason) {
        this.endReason = endReason;
    }

    public String getResultNote() {
        return resultNote;
    }

    public void setResultNote(String resultNote) {
        this.resultNote = resultNote;
    }

    public Integer getPlayer1SetsWon() {
        return player1SetsWon;
    }

    public void setPlayer1SetsWon(Integer player1SetsWon) {
        this.player1SetsWon = player1SetsWon;
    }

    public Integer getPlayer2SetsWon() {
        return player2SetsWon;
    }

    public void setPlayer2SetsWon(Integer player2SetsWon) {
        this.player2SetsWon = player2SetsWon;
    }

    public Integer getPlayer1TotalPoints() {
        return player1TotalPoints;
    }

    public void setPlayer1TotalPoints(Integer player1TotalPoints) {
        this.player1TotalPoints = player1TotalPoints;
    }

    public Integer getPlayer2TotalPoints() {
        return player2TotalPoints;
    }

    public void setPlayer2TotalPoints(Integer player2TotalPoints) {
        this.player2TotalPoints = player2TotalPoints;
    }

    public Integer getVersion() {
        return version;
    }

    public List<MatchPlayer> getMatchPlayers() {
        return matchPlayers;
    }

    public List<MatchOfficial> getMatchOfficials() {
        return matchOfficials;
    }

    public void replaceOfficials(List<Official> officials) {
        matchOfficials.clear();
        for (int i = 0; i < officials.size(); i++) {
            MatchOfficial matchOfficial = new MatchOfficial();
            matchOfficial.setMatch(this);
            matchOfficial.setOfficial(officials.get(i));
            matchOfficial.setPositionNo(i + 1);
            matchOfficial.setRoleName("referee");
            matchOfficials.add(matchOfficial);
        }
    }

    private Optional<MatchPlayer> getMatchPlayer(MatchSide side) {
        return matchPlayers.stream()
                .filter(matchPlayer -> matchPlayer.getSide() == side)
                .findFirst();
    }

    private void putMatchPlayer(MatchSide side, TournamentPlayer tournamentPlayer) {
        Optional<MatchPlayer> existing = getMatchPlayer(side);
        if (tournamentPlayer == null) {
            existing.ifPresent(matchPlayers::remove);
            return;
        }
        MatchPlayer matchPlayer = existing.orElseGet(() -> {
            MatchPlayer created = new MatchPlayer();
            created.setMatch(this);
            created.setSide(side);
            matchPlayers.add(created);
            return created;
        });
        matchPlayer.setTournamentPlayer(tournamentPlayer);
    }
}
