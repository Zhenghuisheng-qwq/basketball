package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketScheduleRuleTimeSlot;
import com.zhs.basket.domain.BasketScheduleRuleWeekday;
import com.zhs.basket.mapper.BasketScheduleRuleMapper;
import com.zhs.basket.mapper.BasketScheduleRuleTimeSlotMapper;
import com.zhs.basket.mapper.BasketScheduleRuleWeekdayMapper;
import com.zhs.basket.service.IBasketScheduleRuleService;
import com.zhs.basket.vo.ScheduleRuleDetailVo;
import com.zhs.basket.vo.ScheduleRuleListVo;
import com.zhs.basket.vo.ScheduleRuleTimeSlotVo;
import com.zhs.basket.vo.ScheduleRuleWeekdayVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BasketScheduleRuleServiceImpl implements IBasketScheduleRuleService {

    private static final String[] WEEKDAY_NAMES = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    @Autowired private BasketScheduleRuleMapper ruleMapper;
    @Autowired private BasketScheduleRuleWeekdayMapper weekdayMapper;
    @Autowired private BasketScheduleRuleTimeSlotMapper timeSlotMapper;

    @Override
    public List<ScheduleRuleListVo> listEnabled() {
        return ruleMapper.selectEnabledList();
    }

    @Override
    public ScheduleRuleDetailVo getDetail(Long ruleId) {
        ScheduleRuleDetailVo detail = ruleMapper.selectDetailById(ruleId);
        if (detail == null) {
            throw new ServiceException("赛程规则不存在");
        }
        List<BasketScheduleRuleWeekday> weekdays = weekdayMapper.selectByRuleId(ruleId);
        List<ScheduleRuleWeekdayVo> weekdayVos = new ArrayList<>();
        for (BasketScheduleRuleWeekday weekday : weekdays) {
            ScheduleRuleWeekdayVo vo = new ScheduleRuleWeekdayVo();
            vo.setWeekday(weekday.getWeekday());
            vo.setSort(weekday.getSort());
            if (weekday.getWeekday() != null && weekday.getWeekday() >= 1 && weekday.getWeekday() <= 7) {
                vo.setWeekdayName(WEEKDAY_NAMES[weekday.getWeekday()]);
            }
            weekdayVos.add(vo);
        }
        detail.setWeekdays(weekdayVos);

        List<BasketScheduleRuleTimeSlot> slots = timeSlotMapper.selectByRuleId(ruleId);
        List<ScheduleRuleTimeSlotVo> slotVos = new ArrayList<>();
        for (BasketScheduleRuleTimeSlot slot : slots) {
            ScheduleRuleTimeSlotVo vo = new ScheduleRuleTimeSlotVo();
            vo.setStartTime(slot.getStartTime());
            vo.setSort(slot.getSort());
            slotVos.add(vo);
        }
        detail.setTimeSlots(slotVos);
        return detail;
    }
}
