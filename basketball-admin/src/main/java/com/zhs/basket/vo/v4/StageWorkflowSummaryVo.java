package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class StageWorkflowSummaryVo {
    private Long stageId;
    private String stageCode;
    private String stageName;
    private Integer stageOrder;
    private String stageType;
    private String stageTypeName;
    private String competitionFormat;
    private String competitionFormatName;
    private String participantSource;
    private Integer participantCount;
    private String structureStatus;
    private String structureStatusName;
    private Integer executionVersion;
    private String pairingStatus;
    private String pairingStatusName;
    private String qualificationStatus;
    private String qualificationStatusName;
    private Integer scheduledUnits;
    private Integer totalUnits;
    private Integer unresolvedPlaceholders;
    private Integer resolvedPlaceholders;
    private String publishStatus;
    private String publishStatusName;
    private Integer publishNo;
    private Integer scheduleRevision;
    private Integer finishedCount;
    private Integer resultTotal;
    private String workflowHint;
    private StageOperationCapabilities capabilities;
}
