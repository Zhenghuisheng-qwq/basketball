package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class DivisionOperationCapabilities {
    private boolean canEditRule;
    private boolean canLockRule;
    private boolean canManageRoster;
    private boolean canLockRoster;
    private boolean canGenerateStructure;
    private boolean canEditSchedule;
    private boolean canPublishStage;
    private boolean canWithdrawStage;
    private boolean requireRulePublished;
    private boolean legacyCompatible;
    private String blockReason;
}
