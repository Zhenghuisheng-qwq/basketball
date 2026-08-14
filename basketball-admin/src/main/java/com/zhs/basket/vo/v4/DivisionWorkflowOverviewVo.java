package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DivisionWorkflowOverviewVo {
    private DivisionContextVo context;
    private RuleSummaryVo rule;
    private RosterSummaryVo roster;
    private List<StageWorkflowSummaryVo> stages = new ArrayList<>();
    private DivisionOperationCapabilities capabilities;
    private String workflowHint;
    /** LEGACY divisions are read-compatible; VERSIONED write guards stay in capabilities. */
    private boolean legacy;
    private boolean readOnly;
}
