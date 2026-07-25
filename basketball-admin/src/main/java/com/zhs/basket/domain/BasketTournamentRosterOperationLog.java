package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournamentRosterOperationLog {
    private Long id;
    private Long seasonId;
    private Long eventTeamId;
    private String operationType;
    private Long targetRosterId;
    private Long targetPlayerId;
    private String beforeValue;
    private String afterValue;
    private String operateReason;
    private Date effectiveTime;
    private Long operatorId;
    private String operatorName;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
