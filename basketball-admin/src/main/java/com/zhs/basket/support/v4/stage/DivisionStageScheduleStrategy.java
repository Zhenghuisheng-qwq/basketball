package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;

/**
 * 阶段赛程生成策略。
 */
public interface DivisionStageScheduleStrategy {

    boolean supports(BasketDivisionStage stage);

    void validate(StageExecutionContext context);

    StageSchedulePreviewVo preview(StageExecutionContext context);

    ScheduleGenerationResult generate(StageExecutionContext context);
}
