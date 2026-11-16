package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.ScheduleRecommendationTypeEnum;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ScheduleRecommendationStrategyFactory {

    @Autowired
    private RoundRobinScheduleRecommendationStrategy roundRobinStrategy;

    public ScheduleRecommendationTypeEnum resolveType(BasketDivisionStage stage) {
        if (stage == null) {
            return ScheduleRecommendationTypeEnum.MANUAL_ONLY;
        }
        String format = stage.getCompetitionFormat();
        if (CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(format)
                || CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(format)) {
            return ScheduleRecommendationTypeEnum.ROUND_ROBIN_STANDARD;
        }
        return ScheduleRecommendationTypeEnum.MANUAL_ONLY;
    }

    public ScheduleRecommendationStrategy require(BasketDivisionStage stage) {
        ScheduleRecommendationTypeEnum type = resolveType(stage);
        if (type == ScheduleRecommendationTypeEnum.ROUND_ROBIN_STANDARD) {
            return roundRobinStrategy;
        }
        throw new ServiceException("当前赛段不支持系统推荐排期，请手工安排日期时间");
    }

    public List<ScheduleUnitView> recommend(BasketDivisionStage stage, List<ScheduleUnitView> units,
                                            LocalDate start, boolean overwriteScheduled) {
        require(stage);
        int roundsPerDay = CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(stage.getCompetitionFormat()) ? 1 : 2;
        return roundRobinStrategy.recommend(units, start, overwriteScheduled, roundsPerDay);
    }
}
