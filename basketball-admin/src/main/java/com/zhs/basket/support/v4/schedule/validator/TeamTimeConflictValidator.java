package com.zhs.basket.support.v4.schedule.validator;

import com.zhs.basket.enums.v4.ScheduleStatusEnum;
import com.zhs.basket.support.v4.schedule.ScheduleAssignmentValidator;
import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.ValidationIssue;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Order(30)
public class TeamTimeConflictValidator implements ScheduleAssignmentValidator {
    @Override
    public void validate(StagePublishContext context, List<ScheduleUnitView> proposed, LocalDate date, LocalTime time, ValidationResult result) {
        Map<String, ScheduleUnitView> occupied = new HashMap<>();
        for (ScheduleUnitView unit : proposed) {
            if (!ScheduleStatusEnum.SCHEDULED.getCode().equals(unit.getScheduleStatus())) {
                continue;
            }
            if (unit.getScheduledDate() == null || unit.getScheduledStartTime() == null) {
                continue;
            }
            checkTeam(occupied, unit, unit.getHomeParticipantId(), result);
            checkTeam(occupied, unit, unit.getAwayParticipantId(), result);
        }
    }

    private void checkTeam(Map<String, ScheduleUnitView> occupied, ScheduleUnitView unit, Long teamId, ValidationResult result) {
        if (teamId == null || teamId <= 0) {
            return;
        }
        String key = teamId + "|" + unit.getScheduledDate() + "|" + unit.getScheduledStartTime();
        ScheduleUnitView existing = occupied.putIfAbsent(key, unit);
        if (existing != null && !Objects.equals(existing.getUnitId(), unit.getUnitId())) {
            ValidationIssue issue = ValidationIssue.error("TEAM_TIME_CONFLICT",
                    "同一支球队在 " + unit.getScheduledDate() + " " + unit.getScheduledStartTime() + " 被安排了两场比赛");
            issue.setUnitType(unit.getUnitType());
            issue.setUnitId(unit.getUnitId());
            result.add(issue);
        }
    }
}
