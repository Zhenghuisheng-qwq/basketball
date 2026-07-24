package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class StageDetailVo {
    private Long id;
    private Long seasonId;
    private String seasonName;
    private Long tournamentId;
    private String tournamentName;
    private String stageNo;
    private String stageName;
    private String stageType;
    private String formatType;
    private Date startTime;
    private Date endTime;
    private Integer sequenceNo;
    private Integer stageStatus;
    private String stageStatusName;
    private String qualificationRuleText;
    private Integer publicStatus;
    private Date createTime;
    private Date updateTime;
    private Boolean canEdit;
    private Boolean canActivate;
    private Boolean canFinish;
    private Boolean canCancel;
    private Boolean canDelete;
}
