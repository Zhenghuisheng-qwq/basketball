package com.zhs.basket.support.v4.version;

import lombok.Data;

@Data
public class RuleVersionView {
    private Long ruleVersionId;
    private Integer ruleVersionNo;
    private String ruleVersionStatus;
    private Long divisionId;
    private Integer isCurrent;
    private boolean locked;
    private String rosterWorkflowMode;
}
