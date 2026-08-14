package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class DivisionContextVo {
    private Long divisionId;
    private String divisionName;
    private Long seasonId;
    private String seasonTitle;
    private Long seriesId;
    private String seriesCode;
    private String seriesName;
    private String platformCode;
    private String platformName;
    private String gameMode;
    private String gameModeName;
    private String registrationMode;
    private String registrationModeName;
    private String templateType;
    private String templateCode;
    private String templateName;
    private String rosterWorkflowMode;
    private String rosterWorkflowModeName;
    private String divisionStatus;
    private String divisionStatusName;
}
