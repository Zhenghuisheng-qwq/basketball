package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketPersonalDrawAssignmentCreateReq {
    private Long personalRegistrationId;
    private String assignmentRole;
    private Integer sortOrder;
}
