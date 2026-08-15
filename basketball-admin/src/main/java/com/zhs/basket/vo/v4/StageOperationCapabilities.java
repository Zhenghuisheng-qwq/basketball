package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class StageOperationCapabilities {
    private boolean canGenerateStructure;
    private boolean canEditSchedule;
    private boolean canAdjustSchedule;
    private boolean canPublish;
    private boolean canWithdraw;
    private boolean canRecommendSchedule;
    private String blockReason;
}
