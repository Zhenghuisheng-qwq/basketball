package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketMatchPeriodScore {
    private Long id;
    private Long matchId;
    private Integer periodNo;
    private String periodType;
    private Integer homeScore;
    private Integer awayScore;
    private Integer periodStatus;
    private Date startTime;
    private Date endTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
