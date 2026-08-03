package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketCaptainGrant
 */
@Data
public class BasketCaptainGrant {
    private Long id;
    private Long divisionId;
    private Long seasonId;
    private Long teamId;
    private Long playerId;
    private Long memberId;
    private String grantStatus;
    private Date validFrom;
    private Date validTo;
    private Long grantedBy;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
