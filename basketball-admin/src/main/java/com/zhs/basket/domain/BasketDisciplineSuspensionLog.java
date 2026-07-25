package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDisciplineSuspensionLog {
    private Long id;
    private Long disciplineId;
    private Long matchId;
    private Long memberId;
    private Integer consumeCount;
    private String idempotencyKey;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
