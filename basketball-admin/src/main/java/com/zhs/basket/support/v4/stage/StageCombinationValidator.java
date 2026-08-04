package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.StageTypeEnum;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 阶段链路组合校验：R3 仅允许有限的阶段组合。
 */
@Component
public class StageCombinationValidator {

    public void validate(List<BasketDivisionStage> stages) {
        if (stages == null || stages.isEmpty()) {
            throw new ServiceException("规则快照至少需要一个阶段");
        }
        if (stages.size() > 2) {
            throw new ServiceException("当前版本最多支持 2 个阶段的赛制链路");
        }

        List<BasketDivisionStage> ordered = stages.stream()
                .sorted(Comparator.comparing(BasketDivisionStage::getStageOrder))
                .toList();

        if (ordered.size() == 1) {
            validateSingleStage(ordered.get(0));
            return;
        }

        BasketDivisionStage first = ordered.get(0);
        BasketDivisionStage second = ordered.get(1);
        validateTwoStageChain(first, second);
    }

    private void validateSingleStage(BasketDivisionStage stage) {
        if (isGroupStage(stage)) {
            return;
        }
        if (CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat())) {
            return;
        }
        if (CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(stage.getCompetitionFormat())) {
            return;
        }
        throw unsupportedCombination("单阶段仅支持循环赛、小组赛或单败淘汰");
    }

    private void validateTwoStageChain(BasketDivisionStage first, BasketDivisionStage second) {
        if (first.getNextStageCode() == null || !first.getNextStageCode().equals(second.getStageCode())) {
            throw new ServiceException("两阶段链路未正确衔接，请检查 next_stage_code");
        }
        if (second.getNextStageCode() != null && !second.getNextStageCode().isEmpty()) {
            throw new ServiceException("最后阶段不应配置下一阶段");
        }
        if (!CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(second.getCompetitionFormat())) {
            throw unsupportedCombination("第二阶段必须为单败淘汰");
        }

        if (isGroupStage(first)) {
            return;
        }
        if (CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(first.getCompetitionFormat()) && !isGroupStage(first)) {
            return;
        }

        throw unsupportedCombination("当前仅支持「循环赛→单败淘汰」或「小组赛→单败淘汰」");
    }

    private boolean isGroupStage(BasketDivisionStage stage) {
        if (StageTypeEnum.GROUP.getCode().equals(stage.getStageType())) {
            return true;
        }
        String groupingMode = stage.getGroupingMode();
        return groupingMode != null && !GroupingModeEnum.NONE.getCode().equals(groupingMode);
    }

    private ServiceException unsupportedCombination(String detail) {
        return new ServiceException("不支持的赛制组合：" + detail
                + "。当前版本允许：单循环、小组赛、单败淘汰，或 循环赛→单败淘汰、小组赛→单败淘汰");
    }
}
