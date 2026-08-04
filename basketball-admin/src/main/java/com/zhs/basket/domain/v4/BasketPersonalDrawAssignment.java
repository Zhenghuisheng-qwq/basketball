package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPersonalDrawAssignment {
    private Long id;
    private Long drawBatchId;
    private Long drawTeamId;
    private Long divisionId;
    private Long personalRegistrationId;
    private Long playerId;
    private String assignmentRole;
    private String joinStatus;
    private Integer sortOrder;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
