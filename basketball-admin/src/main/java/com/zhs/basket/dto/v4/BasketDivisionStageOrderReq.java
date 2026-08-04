package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.List;

@Data
public class BasketDivisionStageOrderReq {
    /** 按期望顺序排列的 stageId 列表 */
    private List<Long> stageIds;
}
