package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketStagePairingExecution {
    private Long id;
    private Long divisionId;
    private Long stageId;
    private Long ruleVersionId;
    private Long rosterVersionId;
    private Integer executionNo;
    private String seedRule;
    private String drawRule;
    private String byeRule;
    private String placementRule;
    private Long sourceStageId;
    private String resolutionStatus;
    private Long qualificationStageId;
    private String status;
    private Long randomSeed;
    private Long operatorId;
    private Date generatedTime;
    private Date confirmedTime;
    private Long confirmedBy;
    private Integer isCurrent;
    private String currentUniqueKey;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
