package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BasketStageQualificationPreviewVo {
    private Long stageId;
    private String stageCode;
    private String stageName;
    private Long targetStageId;
    private String targetStageCode;
    private String targetStageName;
    private Boolean manualRankPending;
    private List<BasketStageQualificationItemVo> items = new ArrayList<>();
}
