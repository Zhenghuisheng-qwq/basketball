package com.zhs.basket.support.v4.schedule.validator;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.support.v4.schedule.ScheduleAssignmentValidator;
import com.zhs.basket.support.v4.schedule.ScheduleTimes;
import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.ValidationIssue;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@Order(20)
public class ScheduleTimeValidator implements ScheduleAssignmentValidator {
    @Override
    public void validate(StagePublishContext context, List<ScheduleUnitView> proposed, LocalDate date, LocalTime time, ValidationResult result) {
        if (date != null || time != null) {
            if (date == null || time == null) {
                result.add(ValidationIssue.error("SCHEDULE_TIME_REQUIRED", "日期和时间必须同时填写"));
                return;
            }
            checkBounds(context.getDivision(), date, result);
            return;
        }
        if (proposed == null) {
            return;
        }
        for (ScheduleUnitView unit : proposed) {
            if (unit.getScheduledDate() == null && unit.getScheduledStartTime() == null) {
                continue;
            }
            if (unit.getScheduledDate() == null || unit.getScheduledStartTime() == null) {
                ValidationIssue issue = ValidationIssue.error("SCHEDULE_TIME_REQUIRED",
                        "单位 #" + unit.getUnitId() + " 日期和时间必须成对填写");
                issue.setUnitType(unit.getUnitType());
                issue.setUnitId(unit.getUnitId());
                result.add(issue);
                continue;
            }
            LocalDate unitDate = ScheduleTimes.parseDate(unit.getScheduledDate());
            checkBounds(context.getDivision(), unitDate, result);
        }
    }

    private void checkBounds(BasketDivision division, LocalDate date, ValidationResult result) {
        if (division != null && division.getMatchStartTime() != null) {
            LocalDate start = ScheduleTimes.toLocalDate(division.getMatchStartTime());
            if (start != null && date.isBefore(start)) {
                result.add(ValidationIssue.error("SCHEDULE_BEFORE_START", "比赛日期不能早于赛事开始日期"));
            }
        }
        if (division != null && division.getMatchEndTime() != null) {
            LocalDate end = ScheduleTimes.toLocalDate(division.getMatchEndTime());
            if (end != null && date.isAfter(end)) {
                result.add(ValidationIssue.error("SCHEDULE_AFTER_END", "比赛日期不能晚于赛事结束日期"));
            }
        }
    }
}
