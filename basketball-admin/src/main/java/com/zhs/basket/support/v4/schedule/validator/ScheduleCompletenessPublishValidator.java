package com.zhs.basket.support.v4.schedule.validator;

import com.zhs.basket.enums.v4.ScheduleStatusEnum;
import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.StagePublishValidator;
import com.zhs.basket.support.v4.schedule.ValidationIssue;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(60)
public class ScheduleCompletenessPublishValidator implements StagePublishValidator {
    @Override
    public void validate(StagePublishContext context, ValidationResult result) {
        if (context.getUnits() == null) {
            return;
        }
        for (ScheduleUnitView unit : context.getUnits()) {
            if (!unit.isRequired()) {
                continue;
            }
            boolean scheduled = ScheduleStatusEnum.SCHEDULED.getCode().equals(unit.getScheduleStatus())
                    && unit.getScheduledDate() != null && unit.getScheduledStartTime() != null;
            if (!scheduled) {
                ValidationIssue issue = ValidationIssue.error("SCHEDULE_MISSING",
                        "赛程尚未完整排期：Round " + unit.getRoundNo() + " 单位 #" + unit.getUnitId() + " 缺少日期时间");
                issue.setUnitType(unit.getUnitType());
                issue.setUnitId(unit.getUnitId());
                result.add(issue);
            }
        }
    }
}
