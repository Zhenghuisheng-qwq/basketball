package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class StageScheduleSummaryVo {
    private Long stageId;
    private Long structureBatchId;
    private Integer totalUnits;
    private Integer scheduledUnits;
    private Integer unscheduledUnits;
    private Integer resolvedUnits;
    private Integer unresolvedUnits;
    private String publishStatus;
    private String structureStatus;
    private Integer executionVersion;
    private Integer scheduleRevision;
    private Integer publishNo;
    private String ruleStatus;
    private String rosterStatus;
    private String pairingStatus;
    private boolean canRecommend;
    private String recommendationType;
}
