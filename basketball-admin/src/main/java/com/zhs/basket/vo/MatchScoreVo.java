package com.zhs.basket.vo;

import lombok.Data;
import java.util.List;

@Data
public class MatchScoreVo {
    private Long matchId;
    private Integer homeScore;
    private Integer awayScore;
    private Long winnerTeamId;
    private Integer overtimeCount;
    private Integer matchStatus;
    private String matchStatusName;
    private Integer dataStatus;
    private String dataStatusName;
    private List<MatchPeriodScoreVo> periods;
}
