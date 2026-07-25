package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerTeamSummaryVo {
    private Long teamId;
    private String teamNo;
    private String teamName;
    private String roleType;
    private Integer membershipStatus;
    private Integer isCurrent;
    private Date joinTime;
    private Integer auditStatus;
    private String auditStatusName;
}
