package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournamentGroup {
    private Long id;
    private Long seasonId;
    private Long stageId;
    private String groupCode;
    private String groupName;
    private Integer qualifyCount;
    private Integer sequenceNo;
    private Integer groupStatus;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
