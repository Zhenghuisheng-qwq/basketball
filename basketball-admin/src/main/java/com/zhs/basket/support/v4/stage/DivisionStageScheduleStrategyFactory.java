package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 按阶段配置选择赛程生成策略。
 */
@Component
public class DivisionStageScheduleStrategyFactory {

    private final List<DivisionStageScheduleStrategy> strategies;

    public DivisionStageScheduleStrategyFactory(List<DivisionStageScheduleStrategy> strategies) {
        this.strategies = strategies;
    }

    public DivisionStageScheduleStrategy resolve(BasketDivisionStage stage) {
        if (stage == null) {
            throw new ServiceException("阶段不能为空");
        }
        return strategies.stream()
                .filter(strategy -> strategy.supports(stage))
                .findFirst()
                .orElseThrow(() -> new ServiceException(
                        "未找到阶段「" + stage.getStageName() + "」对应的赛程生成策略"));
    }
}
