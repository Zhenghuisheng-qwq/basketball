package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class DivisionSelectorItemVo {
    private Long divisionId;
    private String divisionName;
    private Long seasonId;
    private String seasonTitle;
    private Long seriesId;
    private String seriesCode;
    private String seriesName;
    private String platformCode;
    private String gameMode;
    private String registrationMode;
    private String templateType;
    private String templateCode;
    private String ruleStatus;
    private String ruleStatusName;
    private String rosterStatus;
    private String rosterStatusName;
    private String structureSummary;
    private String scheduleSummary;
    private String publishStatus;
    private String publishStatusName;
    private String rosterWorkflowMode;
    private boolean selectable;
    private String blockReason;
}
