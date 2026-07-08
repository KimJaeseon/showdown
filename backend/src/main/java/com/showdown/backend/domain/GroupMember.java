package com.showdown.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "group_members", uniqueConstraints = {
        @UniqueConstraint(name = "uq_group_members_player", columnNames = {"group_id", "tournament_player_id"}),
        @UniqueConstraint(name = "uq_group_members_slot", columnNames = {"group_id", "slot_no"})
})
public class GroupMember {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "group_id")
    private TournamentGroup group;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tournament_player_id")
    private TournamentPlayer tournamentPlayer;

    @Column(name = "slot_no", nullable = false)
    private Integer slotNo;

    @Column(name = "source_rule")
    private String sourceRule;

    public UUID getId() { return id; }
    public TournamentGroup getGroup() { return group; }
    public void setGroup(TournamentGroup group) { this.group = group; }
    public TournamentPlayer getTournamentPlayer() { return tournamentPlayer; }
    public void setTournamentPlayer(TournamentPlayer tournamentPlayer) { this.tournamentPlayer = tournamentPlayer; }
    public Integer getSlotNo() { return slotNo; }
    public void setSlotNo(Integer slotNo) { this.slotNo = slotNo; }
    public String getSourceRule() { return sourceRule; }
    public void setSourceRule(String sourceRule) { this.sourceRule = sourceRule; }
}
