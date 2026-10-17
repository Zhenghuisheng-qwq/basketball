package com.zhs.basket.vo;

import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class TeamAuditDetailVo {
    private Long id;
    private String teamNo;
    private String teamName;
    private String shortName;
    private Long logoFileId;
    private String logoUrl;
    private String regionName;
    private String gameTerminal;
    private String description;
    private Integer publicStatus;
    private String publicStatusName;
    private Integer teamStatus;
    private String teamStatusName;
    private Long divisionId;
    private String divisionName;
    private String platformCode;
    private String gameMode;
    private Long seasonId;
    private String seasonName;
    private String seriesName;
    private Integer auditStatus;
    private String auditStatusName;
    private Integer currentMemberCount;
    private Integer maxMemberCount;
    private Date createTime;
    private Date updateTime;

    private Long captainMemberId;
    private Long captainPlayerId;
    private String captainNickname;
    private String captainEmail;
    private String captainPlayerNo;
    private Integer captainCertStatus;
    private String captainCertStatusName;
    private Boolean captainQualificationValid;

    private Date submitTime;
    private Date auditTime;
    private String auditUserName;
    private String rejectReason;
    private String auditRemark;

    private Boolean captainAssigned;
    private Boolean captainCertified;
    private Boolean teamActive;

    private List<TeamAuditMemberVo> members;
    private Integer eventRosterCount;
    private Integer pendingAddCount;
    private Integer pendingRemoveCount;
    private String rosterGapSummary;
}
