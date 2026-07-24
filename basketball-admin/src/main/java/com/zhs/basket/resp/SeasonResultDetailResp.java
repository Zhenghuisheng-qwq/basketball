package com.zhs.basket.resp;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class SeasonResultDetailResp {
    private Long seasonId;
    private String seasonNo;
    private String seasonName;
    private Integer seasonYear;
    private Long tournamentId;
    private String tournamentName;
    private Integer seasonStatus;
    private String seasonStatusName;
    private Long championTeamId;
    private String championTeamName;
    private Long runnerUpTeamId;
    private String runnerUpTeamName;
    private Long thirdPlaceTeamId;
    private String thirdPlaceTeamName;
    private String resultStatus;
    private String resultStatusName;
    private Integer confirmedMatchCount;
    private Integer unfinishedMatchCount;
    private Integer reopenedMatchCount;
    private Integer finishedUnconfirmedCount;
    private Integer teamSeasonStatCount;
    private List<HonorRecordListResp> honors;
    private List<HonorLineupResp> championLineup;
    private Date updateTime;
}
