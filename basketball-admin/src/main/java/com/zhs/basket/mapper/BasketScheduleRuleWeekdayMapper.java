package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketScheduleRuleWeekday;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketScheduleRuleWeekdayMapper {
    List<BasketScheduleRuleWeekday> selectByRuleId(@Param("ruleId") Long ruleId);
}
