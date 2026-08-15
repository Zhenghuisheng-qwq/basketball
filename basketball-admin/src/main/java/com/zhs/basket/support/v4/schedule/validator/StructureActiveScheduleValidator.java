package com.zhs.basket.support.v4.schedule.validator;

import com.zhs.basket.support.v4.schedule.ScheduleAssignmentValidator;
import com.zhs.basket.support.v4.schedule.ScheduleMutableGuard;
import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.ValidationIssue;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import com.zhs.common.exception.ServiceException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@Order(10)
public class StructureActiveScheduleValidator implements ScheduleAssignmentValidator {
    @Override
    public void validate(StagePublishContext context, List<ScheduleUnitView> proposed, LocalDate date, LocalTime time, ValidationResult result) {
        try {
            ScheduleMutableGuard.assertStructureActive(context.getStructureBatch());
        } catch (ServiceException ex) {
            result.add(ValidationIssue.error("SCHEDULE_GUARD", ex.getMessage()));
        }
    }
}
