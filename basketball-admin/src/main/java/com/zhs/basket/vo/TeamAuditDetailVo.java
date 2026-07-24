package com.zhs.basket.vo;

import lombok.Data;

@Data
public class TeamAuditDetailVo {
    private Long id;
    private String teamNo;
    private String teamName;
    private String shortName;
    private String regionName;
    private String gameTerminal;
    private String description;
    private Long captainMemberId;
    private Long captainPlayerId;
    private Integer auditStatus;
    private String auditStatusName;
    private Integer currentMemberCount;
}
