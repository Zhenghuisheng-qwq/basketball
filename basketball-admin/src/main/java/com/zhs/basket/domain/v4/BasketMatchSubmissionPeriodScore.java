package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchSubmissionPeriodScore {
    private Long id;
    private Long submissionId;
    private Long matchId;
    private Long divisionId;
    private Integer periodNo;
    private Integer homeScore;
    private Integer awayScore;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
