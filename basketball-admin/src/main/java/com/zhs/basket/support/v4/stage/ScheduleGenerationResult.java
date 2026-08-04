package com.zhs.basket.support.v4.stage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阶段赛程生成结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleGenerationResult {

    private Long batchId;
    private Integer expectedMatchCount;
    private Integer actualMatchCount;
    private Integer roundCount;
}
