package com.zhs.response.matchdata;

import com.zhs.request.matchdata.PlayerMatchDataSubmitReq;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("比赛数据提交详情")
public class PlayerMatchDataSubmissionDetailResp implements Serializable {

    private String matchId;
    private String matchNo;
    private String seasonId;
    private String stageId;
    private String homeTeamId;
    private String homeTeamName;
    private String awayTeamId;
    private String awayTeamName;
    private Integer matchStatus;
    private Integer dataStatus;
    private Date scheduledStartTime;
    private List<String> captainTeamIds;
    private List<RosterItem> rosters;
    private Integer homeScore;
    private Integer awayScore;
    private List<PlayerMatchDataSubmitReq.PeriodScoreItem> periodScores;
    private List<PlayerMatchDataSubmitReq.TeamStatItem> teamStats;
    private List<PlayerMatchDataSubmitReq.PlayerStatItem> playerStats;
    private List<String> materialFileIds;
    private String remark;
    private String submissionId;
    private String submissionNo;
    private Integer versionNo;
    private Integer submissionStatus;
    private String rejectReason;
    private Boolean canSubmit;
    private String cannotSubmitReason;

    @Data
    public static class RosterItem implements Serializable {
        private String rosterId;
        private String teamId;
        private String playerId;
        private String playerNickname;
        private String jerseyNo;
        private Integer isStarting;
        private String rosterRole;
    }
}
