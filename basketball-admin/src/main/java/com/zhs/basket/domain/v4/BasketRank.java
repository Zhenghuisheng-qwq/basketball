package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;
import java.math.BigDecimal;

/**
 * CNPA 赛事域 V4 实体：BasketRank
 */
@Data
public class BasketRank {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private String rankType;
    private Integer rankNo;
    private String targetType;
    private Long eventTeamId;
    private Long eventPlayerId;
    private BigDecimal rankValue;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
