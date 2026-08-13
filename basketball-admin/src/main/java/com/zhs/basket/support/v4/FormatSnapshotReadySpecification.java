package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionTieBreakRule;
import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.support.v4.stage.LinearStageChainValidator;
import com.zhs.common.exception.ServiceException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 规则快照完整性校验（LOCK 前）。
 */
public class FormatSnapshotReadySpecification {

    private final LinearStageChainValidator linearStageChainValidator = new LinearStageChainValidator();

    public void validate(List<BasketDivisionStage> stages) {
        validate(stages, List.of());
    }

    public void validate(List<BasketDivisionStage> stages, List<BasketDivisionTieBreakRule> tieBreakRules) {
        if (stages == null || stages.isEmpty()) {
            throw new ServiceException("规则快照至少需要一个比赛阶段");
        }

        List<BasketDivisionStage> ordered = stages.stream()
                .sorted(Comparator.comparing(BasketDivisionStage::getStageOrder))
                .collect(Collectors.toList());

        linearStageChainValidator.validate(ordered);

        for (int i = 0; i < ordered.size(); i++) {
            BasketDivisionStage stage = ordered.get(i);
            boolean isLast = i == ordered.size() - 1;

            validateBestOf(stage.getBestOf());
            if (stage.getSemifinalBestOf() != null) {
                validateBestOf(stage.getSemifinalBestOf());
            }
            if (stage.getFinalBestOf() != null) {
                validateBestOf(stage.getFinalBestOf());
            }
            StageFormatSpecification.validateOrganizationAndGrouping(stage);
            StageFormatSpecification.validateCompetitionFormat(stage, false);
            StageFormatSpecification.validateAllowedCombination(stage);
            StageFormatSpecification.validateAdvancementCount(stage);
            StageFormatSpecification.validateChainAdvancement(stage, isLast);
            StageFormatSpecification.validateRoundRobinRules(stage, isLast);
            StageFormatSpecification.validateEliminationRules(stage, isLast);

            if (CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat())) {
                validateTieBreakRules(stage, tieBreakRules);
            }
        }
    }

    private void validateTieBreakRules(BasketDivisionStage stage, List<BasketDivisionTieBreakRule> tieBreakRules) {
        if (stage.getId() == null) {
            return;
        }
        boolean hasEnabled = tieBreakRules != null && tieBreakRules.stream()
                .anyMatch(rule -> stage.getId().equals(rule.getStageId())
                        && rule.getEnabled() != null && rule.getEnabled() == 1);
        if (!hasEnabled) {
            throw new ServiceException("阶段「" + stage.getStageName() + "」需配置至少一条启用的同分规则");
        }
    }

    public static void validateBestOf(Integer bestOf) {
        BestOfValues.requireLegal(bestOf);
    }

    public static void validateBestOfForSave(Integer bestOf) {
        validateBestOf(bestOf);
    }
}
