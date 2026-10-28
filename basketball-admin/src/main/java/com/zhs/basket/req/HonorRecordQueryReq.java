package com.zhs.basket.req;

import lombok.Data;

@Data
public class HonorRecordQueryReq {
    private Long tournamentId;
    private Long seasonId;
    private Long honorCategoryId;
    private String honorCode;
    /** 逗号分隔的荣誉编码，用于名人堂分栏查询。 */
    private String honorCodes;
    private String honorScope;
    private String recipientType;
    private Long teamId;
    private Long playerId;
    private Integer displayStatus;
    private String keyword;
}
