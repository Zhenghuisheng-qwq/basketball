package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketMatch
 */
@Data
public class BasketMatch {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long scheduleBatchId;
    private Integer roundNo;
    private String matchNo;
    private String stageCode;
    private Date matchTime;
    private Long homeEventTeamId;
    private Long awayEventTeamId;
    private Integer homeScore;
    private Integer awayScore;
    private Long winnerEventTeamId;
    private String matchStatus;
    private String publishStatus;
    private String platformCode;
    private String venueText;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
