package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchOfficialResult {
    private Long id;
    private Long matchId;
    private Integer dataVersion;
    private Integer homeScore;
    private Integer awayScore;
    private Long winnerTeamId;
    private Long loserTeamId;
    private Integer isDraw;
    private String resultType;
    private Date effectiveTime;
    private Long confirmedBy;
    private Date confirmedTime;
    private Integer isCurrent;
    private Integer isPublic;
    private Integer revisionNo;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
