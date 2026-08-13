package com.zhs.basket.support.v4.schedule.validator;

import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.StagePublishValidator;
import com.zhs.basket.support.v4.schedule.ValidationIssue;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
public class StructureBindingPublishValidator implements StagePublishValidator {
    @Override
    public void validate(StagePublishContext context, ValidationResult result) {
        BasketScheduleBatch batch = context.getStructureBatch();
        if (batch == null || context.getStage() == null) {
            return;
        }
        if (batch.getStageId() == null || !batch.getStageId().equals(context.getStage().getId())) {
            result.add(ValidationIssue.error("STRUCTURE_STAGE_MISMATCH", "结构不属于当前赛段"));
        }
        if (context.getStage().getSnapshotId() != null && batch.getRuleVersionId() != null
                && !batch.getRuleVersionId().equals(context.getStage().getSnapshotId())) {
            result.add(ValidationIssue.error("STRUCTURE_RULE_STAGE_MISMATCH", "结构规则绑定与赛段规则不一致"));
        }
    }
}
