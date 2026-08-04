package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketStanding
 */
@Data
public class BasketStanding {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long stageId;
    private Long groupId;
    /** 分组范围键：0=无小组 */
    private Long groupScopeKey;
    private Long eventTeamId;
    private Integer rankNo;
    private Integer winCount;
    private Integer loseCount;
    private Integer drawCount;
    private Integer pointFor;
    private Integer pointAgainst;
    private Integer pointDiff;
    private Integer standingPoints;
    private String rankExplainJson;
    private Integer manualRankPending;
    private String activeUniqueKey;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
