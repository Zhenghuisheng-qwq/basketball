package com.zhs.basket.support.standing;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.support.v4.StandingPointsRule;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StandingStageResolver {

    public static final String MSG_AMBIGUOUS_STAGE = "当前赛区存在多个积分阶段，无法自动确定扣分作用范围";

    @Autowired
    private BasketDivisionStageMapper stageMapper;

    public BasketDivisionStage resolveStandingStage(Long divisionId) {
        if (divisionId == null) {
            return null;
        }
        List<BasketDivisionStage> stages = stageMapper.selectByDivisionId(divisionId);
        if (stages == null || stages.isEmpty()) {
            return null;
        }
        List<BasketDivisionStage> pointStages = new ArrayList<>();
        for (BasketDivisionStage stage : stages) {
            if (stage != null && StandingPointsRule.supportsCompetitionFormat(stage.getCompetitionFormat())) {
                pointStages.add(stage);
            }
        }
        if (pointStages.isEmpty()) {
            return null;
        }
        if (pointStages.size() > 1) {
            throw new ServiceException(MSG_AMBIGUOUS_STAGE);
        }
        return pointStages.get(0);
    }
}
