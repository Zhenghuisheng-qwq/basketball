package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketStagePairingEntry {
    private Long id;
    private Long executionId;
    private Integer slotNo;
    private Long eventTeamId;
    private Integer seedNo;
    private Integer rankingNo;
    private Integer isBye;
    private String sourceType;
    private Long sourceStageId;
    private Long sourceGroupId;
    private Integer sourceRank;
    private Long resolvedEventTeamId;
    private String resolutionStatus;
    private Long qualificationId;
    private String groupRankKey;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
