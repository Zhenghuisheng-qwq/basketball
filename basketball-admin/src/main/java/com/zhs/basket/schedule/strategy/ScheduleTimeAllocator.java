package com.zhs.basket.schedule.strategy;

import com.zhs.basket.schedule.model.MatchPlan;
import com.zhs.basket.schedule.model.ScheduleRuleContext;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class ScheduleTimeAllocator {

    public List<Date> allocateTimes(int matchCount, ScheduleRuleContext context) {
        if (matchCount <= 0) {
            return Collections.emptyList();
        }
        List<Integer> weekdays = context.getWeekdays();
        List<String> timeSlots = context.getTimeSlots();
        if (weekdays == null || weekdays.isEmpty()) {
            throw new ServiceException("规则未配置比赛日");
        }
        if (timeSlots == null || timeSlots.isEmpty()) {
            throw new ServiceException("规则未配置比赛时间段");
        }
        LocalDate cursor = toLocalDate(context.getBaseDate() != null ? context.getBaseDate() : new Date());
        List<Date> result = new ArrayList<>(matchCount);
        int slotIndex = 0;
        while (result.size() < matchCount) {
            if (containsWeekday(cursor, weekdays)) {
                for (String slot : timeSlots) {
                    if (result.size() >= matchCount) {
                        break;
                    }
                    LocalTime time = LocalTime.parse(slot.trim());
                    LocalDateTime dateTime = LocalDateTime.of(cursor, time);
                    result.add(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));
                    slotIndex++;
                }
            }
            cursor = cursor.plusDays(1);
            if (cursor.isAfter(toLocalDate(context.getBaseDate() != null ? context.getBaseDate() : new Date()).plusYears(2))) {
                throw new ServiceException("无法在合理时间范围内分配全部比赛时间，请检查规则配置");
            }
        }
        return result;
    }

    public int slotsPerWeek(List<Integer> weekdays, List<String> timeSlots) {
        return Math.max(1, weekdays.size() * timeSlots.size());
    }

    private boolean containsWeekday(LocalDate date, List<Integer> weekdays) {
        int value = date.getDayOfWeek().getValue();
        for (Integer weekday : weekdays) {
            if (weekday != null && weekday == value) {
                return true;
            }
        }
        return false;
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
