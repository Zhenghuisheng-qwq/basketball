package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.List;

@Data
public class StageScheduleBoardVo {
    private StageScheduleSummaryVo summary;
    private List<ScheduleUnitView> units;
}
