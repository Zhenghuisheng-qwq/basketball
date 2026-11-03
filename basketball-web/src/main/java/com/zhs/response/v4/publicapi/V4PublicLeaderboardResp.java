package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class V4PublicLeaderboardResp {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;
    private String scope;
    private String tab;
    private String mode;
    private String sortBy;
    private String status;
    private Integer policyVersion;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long snapshotId;
    private Date lastCalculatedAt;
    private boolean stale;
    private int page;
    private int pageSize;
    private int totalEligible;
    private String emptyReason;
    private List<Row> rows = new ArrayList<>();

    @Data
    public static class Row {
        private Integer rank;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long eventTeamId;
        private String teamName;
        private String teamLogoUrl;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long eventPlayerId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long sourcePlayerId;
        private String playerName;
        private String avatarUrl;
        private String position;
        private Integer games;
        private Integer wins;
        private Integer losses;
        private Integer points;
        private Integer rebounds;
        private Integer assists;
        private Integer steals;
        private Integer blocks;
        private Integer turnovers;
        private BigDecimal averagePoints;
        private BigDecimal averageRebounds;
        private BigDecimal averageAssists;
        private BigDecimal averageSteals;
        private BigDecimal averageBlocks;
        private BigDecimal averageTurnovers;
        private Integer fieldGoalMade;
        private Integer fieldGoalAttempt;
        private Integer threePointMade;
        private Integer threePointAttempt;
        private Integer freeThrowMade;
        private Integer freeThrowAttempt;
    }
}
