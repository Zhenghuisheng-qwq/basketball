package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketDisciplineRecord {
    private Long id;
    private String recordNo;
    private Long seasonId;
    private Long matchId;
    private Long teamId;
    private Long playerId;
    private Long memberId;
    private String disciplineType;
    private String title;
    private Integer disciplineStatus;
    private Date effectiveStartTime;
    private Date effectiveEndTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
