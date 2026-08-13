package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.List;

@Data
public class ScheduleRecommendationPreviewVo {
    private String recommendationType;
    private boolean overwriteScheduled;
    private int proposedCount;
    private int skippedManualCount;
    private List<ScheduleUnitView> units;
}
