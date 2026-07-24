package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournamentRound {
    private Long id;
    private Long seasonId;
    private Long stageId;
    private String roundNo;
    private String roundName;
    private String roundType;
    private Integer sequenceNo;
    private Integer bestOf;
    private Date startTime;
    private Date endTime;
    private Integer roundStatus;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
