package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class TournamentListVo {
    private Long id;
    private String tournamentNo;
    private Long competitionSeasonId;
    private String competitionSeasonName;
    private Integer competitionSeasonYear;
    private String tournamentName;
    private Integer tournamentStatus;
    private String tournamentStatusName;
    private String gameTerminal;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date competitionStartTime;
    private Date competitionEndTime;
    /** 报名窗口状态码：UNCONFIGURED/NOT_STARTED/OPEN/CLOSED */
    private String registrationWindowStatus;
    private String registrationWindowStatusName;
    private Long operationalSeasonId;
    private Integer displayOrder;
    private Date createTime;
    private Date updateTime;
}
