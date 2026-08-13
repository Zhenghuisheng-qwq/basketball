package com.zhs.basket.support.v4.schedule.validator;

import com.zhs.basket.enums.v4.ParticipantSourceTypeEnum;
import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.StagePublishValidator;
import com.zhs.basket.support.v4.schedule.ValidationIssue;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(70)
public class ParticipantSourcePublishValidator implements StagePublishValidator {
    @Override
    public void validate(StagePublishContext context, ValidationResult result) {
        if (context.getUnits() == null) {
            return;
        }
        for (ScheduleUnitView unit : context.getUnits()) {
            if (!unit.isRequired()) {
                continue;
            }
            boolean homeOk = isLegalSide(unit.getHomeParticipantId(), unit.getHomeSourceType(), unit.getHomeSourceDisplay());
            boolean awayOk = isLegalSide(unit.getAwayParticipantId(), unit.getAwaySourceType(), unit.getAwaySourceDisplay());
            if (!homeOk || !awayOk) {
                ValidationIssue issue = ValidationIssue.error("PARTICIPANT_SOURCE_INVALID",
                        "单位 #" + unit.getUnitId() + " 参赛来源不完整，不能发布");
                issue.setUnitType(unit.getUnitType());
                issue.setUnitId(unit.getUnitId());
                result.add(issue);
                continue;
            }
            if ("TBD".equals(unit.getParticipantResolutionStatus())) {
                ValidationIssue issue = ValidationIssue.warning("PLACEHOLDER_TBD",
                        (unit.getHomeParticipantName() == null ? "待定" : unit.getHomeParticipantName())
                                + " vs "
                                + (unit.getAwayParticipantName() == null ? "待定" : unit.getAwayParticipantName())
                                + " 尚未解析，将以占位名称发布");
                issue.setUnitType(unit.getUnitType());
                issue.setUnitId(unit.getUnitId());
                result.add(issue);
            }
        }
    }

    private boolean isLegalSide(Long teamId, String sourceType, String sourceDisplay) {
        if (teamId != null && teamId > 0) {
            return true;
        }
        return ParticipantSourceTypeEnum.GROUP_RANK.getCode().equals(sourceType)
                || ParticipantSourceTypeEnum.WINNER_OF_SERIES.getCode().equals(sourceType);
    }
}
