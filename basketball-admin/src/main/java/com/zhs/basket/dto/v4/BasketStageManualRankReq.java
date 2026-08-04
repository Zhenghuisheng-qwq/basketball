package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.List;

@Data
public class BasketStageManualRankReq {
    private Long stageId;
    private Long groupScopeKey;
    private String reason;
    private List<ManualRankItem> items;

    @Data
    public static class ManualRankItem {
        private Long eventTeamId;
        private Integer manualRankNo;
    }
}
