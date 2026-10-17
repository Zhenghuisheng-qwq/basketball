package com.zhs.basket.support.v4.schedule.validator;

import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.enums.v4.RosterWorkflowModeEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.StagePublishValidator;
import com.zhs.basket.support.v4.schedule.ValidationIssue;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class RuleVersionPublishValidator implements StagePublishValidator {
    @Override
    public void validate(StagePublishContext context, ValidationResult result) {
        BasketDivisionFormatSnapshot rule = context.getRuleVersion();
        BasketScheduleBatch batch = context.getStructureBatch();
        if (rule == null) {
            result.add(ValidationIssue.error("RULE_MISSING", "缺少规则版本"));
            return;
        }
        boolean versioned = RosterWorkflowModeEnum.VERSIONED.getCode().equals(rule.getRosterWorkflowMode());
        if (!versioned) {
            return;
        }
        if (!SnapshotStatusEnum.LOCKED.getCode().equals(rule.getSnapshotStatus())
                && !SnapshotStatusEnum.IN_USE.getCode().equals(rule.getSnapshotStatus())) {
            result.add(ValidationIssue.error("RULE_NOT_LOCKED", "VERSIONED 规则版本未锁定"));
        }
        if (batch == null) {
            return;
        }
        if (batch.getRuleVersionId() == null || !batch.getRuleVersionId().equals(rule.getId())) {
            result.add(ValidationIssue.error("RULE_BINDING_MISMATCH", "结构绑定的规则版本不是当前锁定规则"));
        }
    }
}
