package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.List;

@Data
public class BasketDivisionTieBreakRuleSaveReq {
    private Long stageId;
    private List<Item> rules;

    @Data
    public static class Item {
        private String ruleCode;
        private Integer priorityNo;
        private Integer enabled;
    }
}
