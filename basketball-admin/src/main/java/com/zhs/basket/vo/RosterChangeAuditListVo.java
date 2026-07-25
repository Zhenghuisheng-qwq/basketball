package com.zhs.basket.vo;

import lombok.Data;

@Data
public class RosterChangeAuditListVo {
    private Long id; private String requestNo; private Long seasonId; private String seasonName; private String changeType; private String changeTypeName; private Integer requestStatus; private String requestStatusName; private Long playerId; private String playerNickname; private Long sourceEventTeamId; private Long targetEventTeamId; private java.util.Date submitTime; private java.util.Date reviewedTime;
}
