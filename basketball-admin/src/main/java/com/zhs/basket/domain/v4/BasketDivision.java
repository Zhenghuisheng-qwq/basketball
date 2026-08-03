package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketDivision
 */
@Data
public class BasketDivision {
    private Long id;
    private Long seasonId;
    private String platformCode;
    private String divisionName;
    private String registrationMode;
    private String formatType;
    private Integer maxTeamCount;
    private Integer maxPersonCount;
    private Integer lineupMinCount;
    private Integer lineupMaxCount;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
    private String divisionStatus;
    private Integer sortOrder;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
