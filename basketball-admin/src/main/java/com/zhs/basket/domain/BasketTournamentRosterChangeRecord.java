package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournamentRosterChangeRecord {
    private Long id;
    private Long seasonId;
    private Long changeRequestId;
    private String changeType;
    private Long playerId;
    private Long sourceEventTeamId;
    private Long targetEventTeamId;
    private Long sourceRosterId;
    private Long targetRosterId;
    private String beforeSnapshot;
    private String afterSnapshot;
    private Long operatorId;
    private String operatorName;
    private String operateReason;
    private Date effectiveTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
