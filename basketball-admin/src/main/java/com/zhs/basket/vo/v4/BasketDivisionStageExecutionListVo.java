package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionStageExecutionListVo {
    private Long stageId;
    private String stageCode;
    private String stageName;
    private Integer stageOrder;
    private String stageType;
    private String stageTypeName;
    private String competitionFormat;
    private String competitionFormatName;
    private String executionStatus;
    private String executionStatusName;
    private Long scheduleBatchId;
    private String publishStatus;
    private String structureStatus;
    private Integer executionVersion;
    private Integer scheduleRevision;
    private Integer participantCount;
    private Integer qualifiedCount;
    private Date startedTime;
    private Date completedTime;
    private Date confirmedTime;
    private String nextStageCode;
    private Integer roundRobinCycles;
    private Integer bestOf;
    private Integer semifinalBestOf;
    private Integer finalBestOf;
    private String pairingRule;
    private String seedRule;
    private String drawRule;
    private String byeRule;
    private Integer advancementCount;
    private String advancementType;
    private Boolean hasUnresolvedPlaceholders;
    private Boolean participantResolved;
}
