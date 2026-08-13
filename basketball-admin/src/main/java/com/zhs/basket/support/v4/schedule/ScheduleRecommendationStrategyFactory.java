package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;
import com.zhs.basket.enums.v4.ScheduleRecommendationTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScheduleRecommendationStrategyFactory {

    @Autowired
    private RoundRobinScheduleRecommendationStrategy roundRobinStrategy;

    public ScheduleRecommendationTypeEnum resolveType(BasketDivisionStage stage) {
        if (stage != null
                && CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat())
                && OrganizationModeEnum.UNGROUPED.getCode().equals(stage.getOrganizationMode())) {
            return ScheduleRecommendationTypeEnum.ROUND_ROBIN_STANDARD;
        }
        return ScheduleRecommendationTypeEnum.MANUAL_ONLY;
    }

    public ScheduleRecommendationStrategy require(BasketDivisionStage stage) {
        ScheduleRecommendationTypeEnum type = resolveType(stage);
        if (type == ScheduleRecommendationTypeEnum.ROUND_ROBIN_STANDARD) {
            return roundRobinStrategy;
        }
        throw new com.zhs.common.exception.ServiceException("当前赛段不支持系统推荐排期，请手工安排日期时间");
    }
}
