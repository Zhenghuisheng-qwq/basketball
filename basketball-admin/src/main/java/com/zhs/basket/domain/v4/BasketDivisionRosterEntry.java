package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionRosterEntry {
    private Long id;
    private Long rosterVersionId;
    private Long divisionId;
    private Long registrationId;
    private Long teamId;
    private String participationRole;
    private Integer candidateOrder;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
