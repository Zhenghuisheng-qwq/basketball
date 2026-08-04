package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketDivisionGroupTeamAssignReq {
    private Long eventTeamId;
    private Integer seedNo;
    private Integer drawOrder;
    private String assignmentSource;
}
