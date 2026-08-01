package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketTeamProfileChange {
    private Long id;
    private String changeNo;
    private Long teamId;
    private Long submitUserId;
    private Long submitPlayerId;
    private String oldTeamName;
    private String newTeamName;
    private String oldTeamShortName;
    private String newTeamShortName;
    private Long oldLogoFileId;
    private Long newLogoFileId;
    private String oldLogoUrl;
    private String newLogoUrl;
    private String oldDescription;
    private String newDescription;
    private Integer oldPublicStatus;
    private Integer newPublicStatus;
    private String changeReason;
    private Integer auditStatus;
    private Long auditUserId;
    private Date auditTime;
    private String auditRemark;
    private Date cancelTime;
    private String cancelReason;
    private Integer activeFlag;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
