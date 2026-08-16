package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.List;

@Data
public class BasketSeriesSubmissionGameAuditVo {
    private Long matchId;
    private Long submissionId;
    private Integer seriesGameNo;
    private String matchNo;
    private Integer homeScore;
    private Integer awayScore;
    private String submissionStatus;
    private Integer draftComplete;
    private String submitRemark;
    private BasketMatchSubmissionTeamStatVo homeTeamStat;
    private BasketMatchSubmissionTeamStatVo awayTeamStat;
    private List<BasketMatchSubmissionPlayerStatVo> homePlayerStats;
    private List<BasketMatchSubmissionPlayerStatVo> awayPlayerStats;
    private List<BasketMatchSubmissionPeriodScoreVo> periodScores;
    private List<BasketMatchSubmissionMaterialVo> materials;
    private List<String> validationErrors;
}
