package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketV4Match
 */
@Data
public class BasketV4Match {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long scheduleBatchId;
    private Integer roundNo;
    private Integer bracketSlot;
    private String matchNo;
    private String stageCode;
    private Long stageId;
    private Long groupId;
    private Date matchTime;
    private Long homeEventTeamId;
    private Long awayEventTeamId;
    private Integer homeScore;
    private Integer awayScore;
    private Long winnerEventTeamId;
    private Long sourceMatchIdHome;
    private Long sourceMatchIdAway;
    private Long loserEventTeamId;
    private String matchStatus;
    private String publishStatus;
    private String platformCode;
    private String venueText;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
