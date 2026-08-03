package com.zhs.basket.vo.v4;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class BasketMatchSubmissionAuditDetailVo {
    private Long submissionId;
    private Long matchId;
    private String matchNo;
    private Integer roundNo;
    private Date matchTime;
    private String divisionName;
    private String platformCode;
    private Long homeEventTeamId;
    private String homeTeamName;
    private Long homeTeamLogoFileId;
    private Long awayEventTeamId;
    private String awayTeamName;
    private Long awayTeamLogoFileId;
    private Long submitEventTeamId;
    private String submitTeamName;
    private Long submitTeamLogoFileId;
    private Long submitEventPlayerId;
    private String submitterName;
    private String submitterRole;
    private Integer versionNo;
    private String submissionStatus;
    private Integer homeScore;
    private Integer awayScore;
    private String submitRemark;
    private Date submitTime;
    private Long auditUserId;
    private String auditUserName;
    private Date auditTime;
    private String auditRemark;
    private String rejectReason;
    private BasketMatchSubmissionTeamStatVo teamStat;
    private List<BasketMatchSubmissionPlayerStatVo> playerStats;
    private List<BasketMatchSubmissionMaterialVo> materials;
    private List<BasketMatchSubmissionVersionVo> historyVersions;
}
