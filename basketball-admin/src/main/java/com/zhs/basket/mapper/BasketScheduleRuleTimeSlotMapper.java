package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketScheduleRuleTimeSlot;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketScheduleRuleTimeSlotMapper {
    List<BasketScheduleRuleTimeSlot> selectByRuleId(@Param("ruleId") Long ruleId);
}
