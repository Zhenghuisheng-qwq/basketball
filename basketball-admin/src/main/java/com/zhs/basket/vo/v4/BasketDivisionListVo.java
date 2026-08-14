package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionListVo {
    private Long id;
    private Long seasonId;
    private String seasonTitle;
    private String seriesCode;
    private String seriesName;
    private String platformCode;
    private String gameMode;
    private String gameModeName;
    private String platformName;
    private String divisionName;
    private String registrationMode;
    private String registrationModeName;
    private String formatType;
    private Long formatTemplateId;
    private String formatTemplateName;
    private String formatTemplateCode;
    private String formatTemplateType;
    private Long formatSnapshotId;
    private String formatSnapshotStatus;
    private String rosterWorkflowMode;
    private String formatSummary;
    private Integer maxTeamCount;
    private Integer maxPersonCount;
    private Integer lineupMinCount;
    private Integer lineupMaxCount;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
    private String divisionStatus;
    private Integer sortOrder;
    private Date createTime;
    private Date updateTime;
}
