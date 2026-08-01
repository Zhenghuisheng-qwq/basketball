package com.zhs.basket.support;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketScheduleRule;
import com.zhs.basket.domain.BasketScheduleRuleTimeSlot;
import com.zhs.basket.domain.BasketScheduleRuleWeekday;
import com.zhs.basket.enums.MatchPublishStatusEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.mapper.BasketScheduleRuleMapper;
import com.zhs.basket.mapper.BasketScheduleRuleTimeSlotMapper;
import com.zhs.basket.mapper.BasketScheduleRuleWeekdayMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ScheduleMatchValidationSupport {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired private BasketScheduleRuleMapper ruleMapper;
    @Autowired private BasketScheduleRuleWeekdayMapper weekdayMapper;
    @Autowired private BasketScheduleRuleTimeSlotMapper timeSlotMapper;

    public void assertDraftEditable(BasketMatch match) {
        if (match.getPublishStatus() != null
                && match.getPublishStatus() != MatchPublishStatusEnum.DRAFT.getCode()) {
            throw new ServiceException("已发布赛程不允许直接编辑，请先下架或联系运维调整");
        }
        MatchStatusEnum status = MatchStatusEnum.fromCode(match.getMatchStatus());
        if (status == MatchStatusEnum.FINISHED) {
            throw new ServiceException("已结束比赛不能调整");
        }
        if (status == MatchStatusEnum.CANCELLED) {
            throw new ServiceException("已取消比赛不能作为正常对阵");
        }
    }

    public void assertMatchTimeByRule(BasketMatch match, Date matchTime) {
        if (matchTime == null) {
            throw new ServiceException("请填写比赛时间");
        }
        BasketScheduleRule rule = resolveRule(match);
        List<Integer> weekdays = weekdayMapper.selectByRuleId(rule.getId()).stream()
                .map(BasketScheduleRuleWeekday::getWeekday).collect(Collectors.toList());
        List<String> timeSlots = timeSlotMapper.selectByRuleId(rule.getId()).stream()
                .map(BasketScheduleRuleTimeSlot::getStartTime).collect(Collectors.toList());
        LocalDateTime dateTime = toLocalDateTime(matchTime);
        int weekday = dateTime.getDayOfWeek().getValue();
        if (!weekdays.contains(weekday)) {
            throw new ServiceException("比赛日期必须是规则配置的比赛日（周二/周四/周六）");
        }
        String slot = dateTime.toLocalTime().format(TIME_FORMAT);
        boolean matched = timeSlots.stream().anyMatch(item -> Objects.equals(normalizeTime(item), slot));
        if (!matched) {
            throw new ServiceException("比赛时间必须是规则配置的时间段（21:00 或 22:00）");
        }
    }

    private BasketScheduleRule resolveRule(BasketMatch match) {
        if (match.getRuleId() != null) {
            BasketScheduleRule rule = ruleMapper.selectById(match.getRuleId());
            if (rule != null) {
                return rule;
            }
        }
        if (match.getRuleCode() != null && match.getRuleVersion() != null) {
            BasketScheduleRule rule = ruleMapper.selectByCodeAndVersion(match.getRuleCode(), match.getRuleVersion());
            if (rule != null) {
                return rule;
            }
        }
        BasketScheduleRule defaultRule = ruleMapper.selectByCodeAndVersion(
                com.zhs.basket.enums.ScheduleRuleCodeEnum.REGULAR_ROUND_ROBIN.getCode(),
                com.zhs.basket.enums.ScheduleRuleVersionEnum.V2.getCode());
        if (defaultRule != null) {
            return defaultRule;
        }
        throw new ServiceException("未找到赛程规则，无法校验比赛时间");
    }

    private String normalizeTime(String value) {
        if (value == null) {
            return null;
        }
        return LocalTime.parse(value.trim()).format(TIME_FORMAT);
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public LocalDate toLocalDate(Date date) {
        return toLocalDateTime(date).toLocalDate();
    }
}
