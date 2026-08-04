package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionStageTeam {
    private Long id;
    private Long divisionId;
    private Long snapshotId;
    private Long stageId;
    private Long eventTeamId;
    private Long sourceStageId;
    private Long sourceGroupId;
    private Integer sourceRank;
    private Integer seedNo;
    private String entryType;
    private String qualificationStatus;
    /** 有效唯一键 stageId_eventTeamId */
    private String activeUniqueKey;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
