package com.zhs.basket.support.v4.schedule.validator;

import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.enums.v4.RosterWorkflowModeEnum;
import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.StagePublishValidator;
import com.zhs.basket.support.v4.schedule.ValidationIssue;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class RosterVersionPublishValidator implements StagePublishValidator {
    @Override
    public void validate(StagePublishContext context, ValidationResult result) {
        BasketDivisionFormatSnapshot rule = context.getRuleVersion();
        if (rule == null || !RosterWorkflowModeEnum.VERSIONED.getCode().equals(rule.getRosterWorkflowMode())) {
            return;
        }
        BasketDivisionRosterVersion roster = context.getRosterVersion();
        BasketScheduleBatch batch = context.getStructureBatch();
        if (roster == null || !"LOCKED".equals(roster.getStatus())) {
            result.add(ValidationIssue.error("ROSTER_NOT_LOCKED", "VERSIONED 名单版本未锁定"));
            return;
        }
        if (batch == null) {
            return;
        }
        if (batch.getRosterVersionId() == null || !batch.getRosterVersionId().equals(roster.getId())) {
            result.add(ValidationIssue.error("ROSTER_BINDING_MISMATCH", "结构绑定的名单版本不是当前锁定名单"));
        }
    }
}
