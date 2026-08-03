package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketRegistrationPlayer
 */
@Data
public class BasketRegistrationPlayer {
    private Long id;
    private Long registrationId;
    private Long divisionId;
    private Long seasonId;
    private Long teamId;
    private Long playerId;
    private Long memberId;
    private Long platformIdentityId;
    private String platformCode;
    private String gameIdSnapshot;
    private String playerNameSnapshot;
    private String position;
    private String jerseyNo;
    private Integer isCaptain;
    private Integer isStarter;
    private Integer sortOrder;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
