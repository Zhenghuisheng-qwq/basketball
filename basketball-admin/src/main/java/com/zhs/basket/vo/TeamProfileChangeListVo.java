package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class TeamProfileChangeListVo {
    private Long id;
    private String changeNo;
    private Long teamId;
    private String teamName;
    private Long submitUserId;
    private String submitUserName;
    private String submitUserEmail;
    private String changeSummary;
    private Integer auditStatus;
    private String auditStatusName;
    private Date submitTime;
    private Date auditTime;
    private String oldTeamName;
    private String newTeamName;
    private String oldTeamShortName;
    private String newTeamShortName;
    private Long oldLogoFileId;
    private Long newLogoFileId;
    private String oldDescription;
    private String newDescription;
}
