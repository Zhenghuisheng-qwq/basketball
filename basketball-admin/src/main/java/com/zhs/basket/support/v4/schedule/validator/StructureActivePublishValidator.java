package com.zhs.basket.support.v4.schedule.validator;

import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.enums.v4.StructureStatusEnum;
import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.StagePublishValidator;
import com.zhs.basket.support.v4.schedule.ValidationIssue;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class StructureActivePublishValidator implements StagePublishValidator {
    @Override
    public void validate(StagePublishContext context, ValidationResult result) {
        BasketScheduleBatch batch = context.getStructureBatch();
        if (batch == null) {
            result.add(ValidationIssue.error("STRUCTURE_MISSING", "当前赛段没有结构，不能发布"));
            return;
        }
        if (!StructureStatusEnum.ACTIVE.getCode().equals(batch.getStructureStatus())) {
            result.add(ValidationIssue.error("STRUCTURE_STALE", "当前结构已失效，不能发布"));
        }
        if (context.getStage() != null && context.getStage().getScheduleBatchId() != null
                && !context.getStage().getScheduleBatchId().equals(batch.getId())) {
            result.add(ValidationIssue.error("STRUCTURE_NOT_CURRENT", "只能发布当前赛段的当前结构"));
        }
    }
}
