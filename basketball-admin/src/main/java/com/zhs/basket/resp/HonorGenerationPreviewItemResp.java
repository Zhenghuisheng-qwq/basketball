package com.zhs.basket.resp;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HonorGenerationPreviewItemResp {
    private String honorCode;
    private String honorName;
    private Long honorCategoryId;
    private Long playerId;
    private String playerName;
    private Long teamId;
    private String teamName;
    private BigDecimal statValue;
    private Integer rankNo;
    private String skipReason;
    private boolean selected;
}
