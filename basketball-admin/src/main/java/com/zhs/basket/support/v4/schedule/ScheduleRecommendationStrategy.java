package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.enums.v4.ScheduleRecommendationTypeEnum;
import com.zhs.basket.vo.v4.ScheduleUnitView;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRecommendationStrategy {

    ScheduleRecommendationTypeEnum type();

    List<ScheduleUnitView> recommend(List<ScheduleUnitView> units, LocalDate startDate, boolean overwriteScheduled);
}
