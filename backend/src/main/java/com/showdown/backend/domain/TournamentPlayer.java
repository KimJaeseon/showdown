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
@Table(name = "tournament_players")
public class TournamentPlayer extends BaseEntity {
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
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(name = "seed_no")
    private Integer seedNo;

    @Column(name = "entry_no")
    private Integer entryNo;

    @Column(name = "display_name_override", length = 200)
    private String displayNameOverride;

    @Column(name = "club_name", length = 200)
    private String clubName;

    @Column(nullable = false, length = 30)
    @ColumnTransformer(write = "?::participant_status")
    private ParticipantStatus status = ParticipantStatus.ACTIVE;

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

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Integer getSeedNo() {
        return seedNo;
    }

    public void setSeedNo(Integer seedNo) {
        this.seedNo = seedNo;
    }

    public Integer getEntryNo() {
        return entryNo;
    }

    public void setEntryNo(Integer entryNo) {
        this.entryNo = entryNo;
    }

    public String getDisplayNameOverride() {
        return displayNameOverride;
    }

    public void setDisplayNameOverride(String displayNameOverride) {
        this.displayNameOverride = displayNameOverride;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public ParticipantStatus getStatus() {
        return status;
    }

    public void setStatus(ParticipantStatus status) {
        this.status = status;
    }
}
