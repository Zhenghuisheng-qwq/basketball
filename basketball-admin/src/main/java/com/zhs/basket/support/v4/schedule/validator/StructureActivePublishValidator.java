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
    public static final String MSG_STRUCTURE_NOT_GENERATED =
            "当前赛段尚未生成赛程结构，请先完成所需前置步骤并生成结构。";
    public static final String MSG_STALE_REQUIRES_REGENERATE =
            "当前赛段只有已失效的历史结构，请按当前规则重新生成赛程结构。";

    @Override
    public void validate(StagePublishContext context, ValidationResult result) {
        BasketScheduleBatch batch = context.getStructureBatch();
        if (batch == null) {
            result.add(ValidationIssue.error("STRUCTURE_MISSING", context.isHistoricalStaleStructure()
                    ? MSG_STALE_REQUIRES_REGENERATE
                    : MSG_STRUCTURE_NOT_GENERATED));
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
