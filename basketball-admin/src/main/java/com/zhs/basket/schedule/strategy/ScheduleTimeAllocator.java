package com.zhs.basket.schedule.strategy;

import com.zhs.basket.constant.ScheduleSlotConstants;
import com.zhs.basket.enums.ScheduleAllocationModeEnum;
import com.zhs.basket.schedule.model.MatchPlan;
import com.zhs.basket.schedule.model.ScheduleRuleContext;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 按比赛日 + 时间槽容量为每轮内的每场比赛分配 scheduledStartTime。
 * 一轮对应一个比赛日；轮内比赛按配置填入 21:00 / 22:00 等时间槽。
 */
@Component
public class ScheduleTimeAllocator {

    public void allocateMatchTimes(List<List<MatchPlan>> rounds, ScheduleRuleContext context) {
        if (rounds == null || rounds.isEmpty()) {
            return;
        }
        List<Integer> weekdays = context.getWeekdays();
        List<String> timeSlots = context.getTimeSlots();
        if (weekdays == null || weekdays.isEmpty()) {
            throw new ServiceException("规则未配置比赛日");
        }
        if (timeSlots == null || timeSlots.isEmpty()) {
            throw new ServiceException("规则未配置比赛时间段");
        }
        int slotCapacity = resolveSlotCapacity(context);
        ScheduleAllocationModeEnum mode = ScheduleAllocationModeEnum.fromCode(context.getAllocationMode());

        LocalDate cursor = toLocalDate(context.getBaseDate() != null ? context.getBaseDate() : new Date());
        LocalDate baseDate = cursor;
        int roundIndex = 0;
        while (roundIndex < rounds.size()) {
            if (!containsWeekday(cursor, weekdays)) {
                cursor = cursor.plusDays(1);
                if (cursor.isAfter(baseDate.plusYears(2))) {
                    throw new ServiceException("无法在合理时间范围内分配全部轮次比赛日，请检查规则配置");
                }
                continue;
            }
            LocalDate matchDay = cursor;
            List<MatchPlan> roundMatches = rounds.get(roundIndex);
            int matchCount = roundMatches.size();
            int[] perSlotCounts = distributeMatchCounts(matchCount, timeSlots.size(), slotCapacity, mode);
            int matchIdx = 0;
            for (int slotIdx = 0; slotIdx < timeSlots.size(); slotIdx++) {
                int countInSlot = perSlotCounts[slotIdx];
                if (countInSlot <= 0) {
                    continue;
                }
                LocalTime slotTime = LocalTime.parse(timeSlots.get(slotIdx).trim());
                LocalDateTime dateTime = LocalDateTime.of(matchDay, slotTime);
                Date scheduled = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
                for (int i = 0; i < countInSlot; i++) {
                    roundMatches.get(matchIdx++).setMatchTime(scheduled);
                }
            }
            if (matchIdx != matchCount) {
                throw new ServiceException(String.format(
                        "第 %d 轮共 %d 场比赛，超出当日时间槽总容量（每槽最多 %d 场，共 %d 个时间槽）",
                        roundIndex + 1, matchCount, slotCapacity, timeSlots.size()));
            }
            roundIndex++;
            cursor = cursor.plusDays(1);
        }
    }

    /**
     * @deprecated 保留兼容；新逻辑请使用 {@link #allocateMatchTimes(List, ScheduleRuleContext)}。
     */
    @Deprecated
    public List<Date> allocateTimes(int matchCount, ScheduleRuleContext context) {
        if (matchCount <= 0) {
            return List.of();
        }
        List<List<MatchPlan>> dummyRounds = new ArrayList<>(matchCount);
        for (int i = 0; i < matchCount; i++) {
            List<MatchPlan> single = new ArrayList<>(1);
            single.add(new MatchPlan());
            dummyRounds.add(single);
        }
        allocateMatchTimes(dummyRounds, context);
        List<Date> result = new ArrayList<>(matchCount);
        for (List<MatchPlan> round : dummyRounds) {
            result.add(round.get(0).getMatchTime());
        }
        return result;
    }

    static int[] distributeMatchCounts(int matchCount, int slotCount, int slotCapacity,
            ScheduleAllocationModeEnum mode) {
        if (matchCount <= 0) {
            return new int[slotCount];
        }
        if (slotCount <= 0) {
            throw new ServiceException("规则未配置比赛时间段");
        }
        int maxTotal = slotCount * slotCapacity;
        if (matchCount > maxTotal) {
            throw new ServiceException(String.format(
                    "单轮 %d 场比赛超出时间槽总容量 %d（%d 个时间槽 × 每槽最多 %d 场）",
                    matchCount, maxTotal, slotCount, slotCapacity));
        }
        int[] counts = new int[slotCount];
        if (mode == ScheduleAllocationModeEnum.EVEN_SPLIT
                || mode == ScheduleAllocationModeEnum.EVEN_SPLIT_WITH_LATE_EXTRA) {
            int base = matchCount / slotCount;
            int remainder = matchCount % slotCount;
            for (int i = 0; i < slotCount; i++) {
                counts[i] = base;
            }
            if (mode == ScheduleAllocationModeEnum.EVEN_SPLIT) {
                for (int i = 0; i < remainder; i++) {
                    counts[i]++;
                }
            } else {
                for (int i = slotCount - 1; i >= slotCount - remainder; i--) {
                    counts[i]++;
                }
            }
            for (int i = 0; i < slotCount; i++) {
                if (counts[i] > slotCapacity) {
                    throw new ServiceException(String.format(
                            "均分模式下第 %d 个时间槽需安排 %d 场，超过容量 %d",
                            i + 1, counts[i], slotCapacity));
                }
            }
            return counts;
        }
        int remaining = matchCount;
        for (int i = 0; i < slotCount && remaining > 0; i++) {
            int assign = Math.min(remaining, slotCapacity);
            counts[i] = assign;
            remaining -= assign;
        }
        if (remaining > 0) {
            throw new ServiceException(String.format(
                    "优先填充模式下仍有 %d 场比赛无法安排，请增加时间槽或提高单槽容量", remaining));
        }
        return counts;
    }

    private int resolveSlotCapacity(ScheduleRuleContext context) {
        if (context.getSlotCapacity() != null && context.getSlotCapacity() > 0) {
            return context.getSlotCapacity();
        }
        return ScheduleSlotConstants.DEFAULT_SLOT_CAPACITY;
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
