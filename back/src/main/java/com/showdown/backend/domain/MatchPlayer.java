package com.showdown.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "match_players")
public class MatchPlayer extends BaseEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tournament_player_id")
    private TournamentPlayer tournamentPlayer;

    @Column(nullable = false, length = 30)
    @ColumnTransformer(write = "?::match_side")
    private MatchSide side;

    @Column(name = "source_slot", length = 200)
    private String sourceSlot;

    public UUID getId() {
        return id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public TournamentPlayer getTournamentPlayer() {
        return tournamentPlayer;
    }

    public void setTournamentPlayer(TournamentPlayer tournamentPlayer) {
        this.tournamentPlayer = tournamentPlayer;
    }

    public MatchSide getSide() {
        return side;
    }

    public void setSide(MatchSide side) {
        this.side = side;
    }

    public String getSourceSlot() {
        return sourceSlot;
    }

    public void setSourceSlot(String sourceSlot) {
        this.sourceSlot = sourceSlot;
    }
}
