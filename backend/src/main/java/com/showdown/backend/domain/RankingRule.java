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

@Entity
@Table(name = "ranking_rules")
public class RankingRule {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @Column(name = "win_points", nullable = false)
    private Integer winPoints = 1;

    @Column(name = "loss_points", nullable = false)
    private Integer lossPoints = 0;

    @Column(name = "walkover_win_points", nullable = false)
    private Integer walkoverWinPoints = 1;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    public static RankingRule fallbackDefault() {
        RankingRule rule = new RankingRule();
        rule.winPoints = 1;
        rule.lossPoints = 0;
        rule.walkoverWinPoints = 1;
        rule.isDefault = true;
        return rule;
    }

    public UUID getId() {
        return id;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public Integer getWinPoints() {
        return winPoints;
    }

    public Integer getLossPoints() {
        return lossPoints;
    }

    public Integer getWalkoverWinPoints() {
        return walkoverWinPoints;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }
}
