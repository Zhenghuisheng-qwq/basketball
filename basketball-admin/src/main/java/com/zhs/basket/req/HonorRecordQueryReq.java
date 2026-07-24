package com.zhs.basket.req;

import lombok.Data;

@Data
public class HonorRecordQueryReq {
    private Long tournamentId;
    private Long seasonId;
    private Long honorCategoryId;
    private String honorCode;
    private String honorScope;
    private String recipientType;
    private Long teamId;
    private Long playerId;
    private Integer displayStatus;
    private String keyword;
}
