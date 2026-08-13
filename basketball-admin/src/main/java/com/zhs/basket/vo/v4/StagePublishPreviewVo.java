package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.List;

@Data
public class StagePublishPreviewVo {
    private boolean canPublish;
    private StageScheduleSummaryVo summary;
    private List<ScheduleUnitView> units;
    private List<ValidationIssueVo> errors;
    private List<ValidationIssueVo> warnings;
}
