package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class TeamProfileChangeDetailVo {
    private Long id;
    private String changeNo;
    private Long teamId;
    private String teamName;
    private String oldTeamName;
    private String newTeamName;
    private String oldTeamShortName;
    private String newTeamShortName;
    private String oldLogoUrl;
    private String newLogoUrl;
    private String oldDescription;
    private String newDescription;
    private Integer oldPublicStatus;
    private Integer newPublicStatus;
    private String changeReason;
    private Integer auditStatus;
    private String auditStatusName;
    private Long submitUserId;
    private String submitUserName;
    private String submitUserEmail;
    private String submitPlayerNo;
    private Date submitTime;
    private Date auditTime;
    private Long auditUserId;
    private String auditRemark;
    private String changeSummary;
}
