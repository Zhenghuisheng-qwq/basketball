package com.zhs.basket.vo;

import lombok.Data;

@Data
public class RosterOperationLogVo {
    private Long id; private Long seasonId; private Long eventTeamId; private String operationType; private Long targetRosterId; private Long targetPlayerId; private String beforeValue; private String afterValue; private String operateReason; private java.util.Date effectiveTime; private String operatorName; private java.util.Date createTime;
}
