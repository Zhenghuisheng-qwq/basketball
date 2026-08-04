package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketDivisionGroupTeamVo {
    private Long assignmentId;
    private Long eventTeamId;
    private String teamNameSnapshot;
    private String eventTeamNo;
    private Integer seedNo;
    private Integer drawOrder;
    private String assignmentSource;
}
