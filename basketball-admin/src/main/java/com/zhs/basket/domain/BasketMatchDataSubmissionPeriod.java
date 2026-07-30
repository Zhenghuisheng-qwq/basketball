package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchDataSubmissionPeriod {
    private Long id;
    private Long submissionId;
    private Long matchId;
    private Integer periodNo;
    private String periodType;
    private Integer homeScore;
    private Integer awayScore;
    private Integer sortNo;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
