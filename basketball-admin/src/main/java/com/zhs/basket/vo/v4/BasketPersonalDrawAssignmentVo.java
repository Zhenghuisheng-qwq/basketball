package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketPersonalDrawAssignmentVo {
    private Long assignmentId;
    private Long personalRegistrationId;
    private Long playerId;
    private String playerName;
    private String assignmentRole;
    private String joinStatus;
    private Integer sortOrder;
}
