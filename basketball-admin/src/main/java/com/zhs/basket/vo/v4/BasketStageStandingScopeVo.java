package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BasketStageStandingScopeVo {
    private Long groupScopeKey;
    private Long groupId;
    private String groupCode;
    private String groupName;
    private Boolean manualRankPending;
    private List<BasketStageStandingRowVo> rows = new ArrayList<>();
}
