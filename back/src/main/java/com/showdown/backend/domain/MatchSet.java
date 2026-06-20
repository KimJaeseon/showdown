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
@Table(name = "match_sets")
public class MatchSet extends BaseEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private Match match;

    @Column(name = "set_no", nullable = false)
    private Integer setNo;

    @Column(name = "player1_score", nullable = false)
    private Integer player1Score;

    @Column(name = "player2_score", nullable = false)
    private Integer player2Score;

    @Column(name = "winner_side", nullable = false, length = 30)
    @ColumnTransformer(write = "?::match_side")
    private MatchSide winnerSide;

    public UUID getId() {
        return id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public Integer getSetNo() {
        return setNo;
    }

    public void setSetNo(Integer setNo) {
        this.setNo = setNo;
    }

    public Integer getPlayer1Score() {
        return player1Score;
    }

    public void setPlayer1Score(Integer player1Score) {
        this.player1Score = player1Score;
    }

    public Integer getPlayer2Score() {
        return player2Score;
    }

    public void setPlayer2Score(Integer player2Score) {
        this.player2Score = player2Score;
    }

    public MatchSide getWinnerSide() {
        return winnerSide;
    }

    public void setWinnerSide(MatchSide winnerSide) {
        this.winnerSide = winnerSide;
    }
}
