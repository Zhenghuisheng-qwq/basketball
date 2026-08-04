package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

/**
 * CNPA 赛事域 V4：积分榜排名解释明细（结构化，替代 rank_explain_json）。
 */
@Data
public class BasketStandingRankReason {
    private Long id;
    private Long divisionId;
    private Long stageId;
    private Long groupId;
    private Long standingId;
    private Long eventTeamId;
    private String ruleCode;
    private Integer priorityNo;
    private String compareValue;
    private String reasonText;
    private Integer isDecisive;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
