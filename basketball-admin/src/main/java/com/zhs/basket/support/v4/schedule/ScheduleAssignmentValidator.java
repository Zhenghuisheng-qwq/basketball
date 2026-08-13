package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.vo.v4.ScheduleUnitView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ScheduleAssignmentValidator {
    void validate(StagePublishContext context, List<ScheduleUnitView> proposed, LocalDate date, LocalTime time, ValidationResult result);
}
