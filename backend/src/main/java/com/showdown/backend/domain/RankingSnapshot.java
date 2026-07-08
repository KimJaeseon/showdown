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
import java.util.UUID;

@Entity
@Table(name = "rankings_snapshots")
public class RankingSnapshot {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "tournament_id") private Tournament tournament;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "division_id") private Division division;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "stage_id") private Stage stage;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "group_id") private TournamentGroup group;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "tournament_player_id") private TournamentPlayer tournamentPlayer;
    @Column(name = "rank_no", nullable = false) private Integer rankNo;
    @Column(name = "matches_played", nullable = false) private Integer matchesPlayed = 0;
    @Column(nullable = false) private Integer wins = 0;
    @Column(nullable = false) private Integer losses = 0;
    @Column(name = "match_points", nullable = false) private Integer matchPoints = 0;
    @Column(name = "sets_won", nullable = false) private Integer setsWon = 0;
    @Column(name = "sets_lost", nullable = false) private Integer setsLost = 0;
    @Column(name = "set_difference", nullable = false) private Integer setDifference = 0;
    @Column(name = "points_for", nullable = false) private Integer pointsFor = 0;
    @Column(name = "points_against", nullable = false) private Integer pointsAgainst = 0;
    @Column(name = "point_difference", nullable = false) private Integer pointDifference = 0;
    @Column(name = "tie_break_note") private String tieBreakNote;
    @Column(name = "calculated_at", nullable = false) private OffsetDateTime calculatedAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public Tournament getTournament() { return tournament; } public void setTournament(Tournament v) { tournament = v; }
    public Division getDivision() { return division; } public void setDivision(Division v) { division = v; }
    public Stage getStage() { return stage; } public void setStage(Stage v) { stage = v; }
    public TournamentGroup getGroup() { return group; } public void setGroup(TournamentGroup v) { group = v; }
    public TournamentPlayer getTournamentPlayer() { return tournamentPlayer; } public void setTournamentPlayer(TournamentPlayer v) { tournamentPlayer = v; }
    public Integer getRankNo() { return rankNo; } public void setRankNo(Integer v) { rankNo = v; }
    public Integer getMatchesPlayed() { return matchesPlayed; } public void setMatchesPlayed(Integer v) { matchesPlayed = v; }
    public Integer getWins() { return wins; } public void setWins(Integer v) { wins = v; }
    public Integer getLosses() { return losses; } public void setLosses(Integer v) { losses = v; }
    public Integer getMatchPoints() { return matchPoints; } public void setMatchPoints(Integer v) { matchPoints = v; }
    public Integer getSetsWon() { return setsWon; } public void setSetsWon(Integer v) { setsWon = v; }
    public Integer getSetsLost() { return setsLost; } public void setSetsLost(Integer v) { setsLost = v; }
    public Integer getSetDifference() { return setDifference; } public void setSetDifference(Integer v) { setDifference = v; }
    public Integer getPointsFor() { return pointsFor; } public void setPointsFor(Integer v) { pointsFor = v; }
    public Integer getPointsAgainst() { return pointsAgainst; } public void setPointsAgainst(Integer v) { pointsAgainst = v; }
    public Integer getPointDifference() { return pointDifference; } public void setPointDifference(Integer v) { pointDifference = v; }
    public String getTieBreakNote() { return tieBreakNote; } public void setTieBreakNote(String v) { tieBreakNote = v; }
    public OffsetDateTime getCalculatedAt() { return calculatedAt; } public void setCalculatedAt(OffsetDateTime v) { calculatedAt = v; }
}
