package com.zhs.basket.vo;

import lombok.Data;
import java.util.Date;

@Data
public class MatchDetailVo {
    private Long id;
    private String matchNo;
    private Long seasonId;
    private String seasonName;
    private Long stageId;
    private String stageName;
    private Long roundId;
    private String roundName;
    private Long groupId;
    private String groupName;
    private Long venueId;
    private String venueName;
    private Long homeTeamId;
    private String homeTeamName;
    private Long awayTeamId;
    private String awayTeamName;
    private Date scheduledStartTime;
    private Date scheduledEndTime;
    private Date actualStartTime;
    private Date actualEndTime;
    private Integer matchStatus;
    private String matchStatusName;
    private Integer publishStatus;
    private String publishStatusName;
    /** 运营展示状态：已取消 / 已下架 / 比赛状态 */
    private String operationalStatusName;
    private Integer homeScore;
    private Integer awayScore;
    private Long winnerTeamId;
    private String winnerTeamName;
    private Integer overtimeCount;
    private Long walkoverTeamId;
    private String cancelReason;
    private Integer dataStatus;
    private String dataStatusName;
    private Integer publicStatus;
    private Integer sortNo;
    private Boolean canEdit;
    private Boolean canSchedule;
    private Boolean canStart;
    private Boolean canPostpone;
    private Boolean canCancel;
    private Boolean canDelete;
    private Boolean canEnterLineup;
    private Boolean canEnterScore;
    private Boolean canEnterStat;
    private Boolean canConfirmResult;
    private Boolean canReopenResult;
}
