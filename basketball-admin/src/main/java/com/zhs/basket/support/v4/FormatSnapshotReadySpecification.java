package com.zhs.basket.support.v4;



import com.zhs.basket.domain.v4.BasketDivisionStage;

import com.zhs.basket.domain.v4.BasketDivisionTieBreakRule;

import com.zhs.basket.enums.v4.AdvancementTypeEnum;

import com.zhs.basket.enums.v4.CompetitionFormatEnum;

import com.zhs.basket.enums.v4.GroupingModeEnum;

import com.zhs.common.exception.ServiceException;



import com.zhs.basket.support.v4.stage.StageCombinationValidator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;



/**

 * 规则快照完整性校验。

 */

public class FormatSnapshotReadySpecification {



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

        validateStageOrderContinuous(ordered);
        new StageCombinationValidator().validate(ordered);

        Set<Integer> orders = new HashSet<>();
        Set<String> codes = new HashSet<>();
        Map<String, BasketDivisionStage> codeToStage = new HashMap<>();

        for (BasketDivisionStage stage : ordered) {

            if (stage.getStageOrder() == null || stage.getStageOrder() <= 0) {

                throw new ServiceException("阶段顺序必须大于 0");

            }

            if (!orders.add(stage.getStageOrder())) {

                throw new ServiceException("阶段顺序不能重复：" + stage.getStageOrder());

            }

            if (!codes.add(stage.getStageCode())) {

                throw new ServiceException("阶段编码不能重复：" + stage.getStageCode());

            }

            codeToStage.put(stage.getStageCode(), stage);

            validateBestOf(stage.getBestOf());

            validateSupportedCompetitionFormat(stage);

            validateGroupingMode(stage);

            validateAdvancement(stage);

            if (CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat())) {

                if (stage.getRoundRobinCycles() == null || stage.getRoundRobinCycles() <= 0) {

                    throw new ServiceException("循环赛阶段必须配置 round_robin_cycles");

                }

                validateRankingPoints(stage);

                validateTieBreakRules(stage, tieBreakRules);

            }

        }

        for (int i = 0; i < ordered.size(); i++) {
            BasketDivisionStage stage = ordered.get(i);
            boolean isLast = i == ordered.size() - 1;
            AdvancementTypeEnum adv = AdvancementTypeEnum.fromCode(stage.getAdvancementType());
            boolean hasAdvancement = adv != null && adv != AdvancementTypeEnum.NONE;

            if (isLast) {
                if (hasAdvancement) {
                    throw new ServiceException("最后阶段「" + stage.getStageName() + "」不应再配置晋级规则");
                }
                if (stage.getNextStageCode() != null && !stage.getNextStageCode().isEmpty()) {
                    throw new ServiceException("最后阶段「" + stage.getStageName() + "」不应再指向下一阶段");
                }
            } else {
                if (!hasAdvancement) {
                    throw new ServiceException("阶段「" + stage.getStageName() + "」不是最后阶段，请配置晋级规则");
                }
                if (stage.getNextStageCode() == null || stage.getNextStageCode().isEmpty()) {
                    throw new ServiceException("阶段「" + stage.getStageName() + "」需要指定下一比赛阶段");
                }
                if (!codes.contains(stage.getNextStageCode())) {
                    throw new ServiceException("阶段「" + stage.getStageName() + "」的下一阶段配置无效，请重新选择");
                }
                BasketDivisionStage next = codeToStage.get(stage.getNextStageCode());
                if (next != null && next.getStageOrder() != null && stage.getStageOrder() != null
                        && next.getStageOrder() <= stage.getStageOrder()) {
                    throw new ServiceException("阶段顺序与晋级关系不一致，请调整阶段顺序");
                }
            }
        }

        detectNextStageCycle(ordered);
        validateEliminationAdvancementCount(ordered);
    }

    private void validateStageOrderContinuous(List<BasketDivisionStage> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            if (!Integer.valueOf(i + 1).equals(ordered.get(i).getStageOrder())) {
                throw new ServiceException("阶段顺序必须为连续正整数");
            }
        }
    }

    private void validateEliminationAdvancementCount(List<BasketDivisionStage> ordered) {
        if (ordered.size() < 2) {
            return;
        }
        BasketDivisionStage first = ordered.get(0);
        BasketDivisionStage second = ordered.get(1);
        if (!CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(second.getCompetitionFormat())) {
            return;
        }
        int qualifierCount = 0;
        AdvancementTypeEnum adv = AdvancementTypeEnum.fromCode(first.getAdvancementType());
        if (adv == AdvancementTypeEnum.GROUP_TOP_N) {
            int groupCount = first.getGroupCount() == null ? 0 : first.getGroupCount();
            int perGroup = first.getAdvancementCount() == null ? 0 : first.getAdvancementCount();
            qualifierCount = groupCount * perGroup;
        } else if (adv == AdvancementTypeEnum.OVERALL_TOP_N) {
            qualifierCount = first.getAdvancementCount() == null ? 0 : first.getAdvancementCount();
        }
        if (qualifierCount > 0 && !DivisionFormatPlanValidator.isPowerOfTwo(qualifierCount)) {
            throw new ServiceException(String.format(
                    "当前将有 %d 支球队晋级，但系统暂不支持 %d 支球队自动生成单败淘汰赛。请调整为 4 支或 8 支晋级。",
                    qualifierCount, qualifierCount));
        }
    }



    private void validateGroupingMode(BasketDivisionStage stage) {

        GroupingModeEnum mode = GroupingModeEnum.fromCode(stage.getGroupingMode());

        if (mode == null) {

            throw new ServiceException("阶段「" + stage.getStageName() + "」分组方式无效");

        }

        if (mode == GroupingModeEnum.NONE) {

            if (stage.getGroupCount() != null && stage.getGroupCount() > 0) {

                throw new ServiceException("阶段「" + stage.getStageName() + "」无分组时不应配置 group_count");

            }

        } else if (stage.getGroupCount() == null || stage.getGroupCount() <= 0) {

            throw new ServiceException("阶段「" + stage.getStageName() + "」需要配置有效 group_count");

        }

    }



    private void validateAdvancement(BasketDivisionStage stage) {

        AdvancementTypeEnum adv = AdvancementTypeEnum.fromCode(stage.getAdvancementType());

        if (adv == null) {

            throw new ServiceException("阶段「" + stage.getStageName() + "」晋级方式无效");

        }

        if (adv == AdvancementTypeEnum.SERIES_WINNER) {

            throw new ServiceException("阶段「" + stage.getStageName() + "」暂不支持系列赛胜者（SERIES_WINNER）晋级方式");

        }

        if (adv == AdvancementTypeEnum.NONE) {

            if (stage.getAdvancementCount() != null && stage.getAdvancementCount() > 0) {

                throw new ServiceException("阶段「" + stage.getStageName() + "」无晋级时不应配置晋级数量");

            }

        } else if (adv.requiresAdvancementCount()

                && (stage.getAdvancementCount() == null || stage.getAdvancementCount() <= 0)) {

            throw new ServiceException("阶段「" + stage.getStageName() + "」需要配置晋级数量");

        }

    }



    private void validateRankingPoints(BasketDivisionStage stage) {

        if (stage.getWinPoints() == null || stage.getLossPoints() == null) {

            throw new ServiceException("阶段「" + stage.getStageName() + "」需配置胜负积分");

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



    private void detectNextStageCycle(List<BasketDivisionStage> stages) {

        Map<String, String> nextMap = new HashMap<>();

        for (BasketDivisionStage stage : stages) {

            if (stage.getNextStageCode() != null && !stage.getNextStageCode().isEmpty()) {

                nextMap.put(stage.getStageCode(), stage.getNextStageCode());

            }

        }

        for (BasketDivisionStage stage : stages) {

            Set<String> visited = new HashSet<>();

            String current = stage.getStageCode();

            while (nextMap.containsKey(current)) {

                if (!visited.add(current)) {

                    throw new ServiceException("阶段链路存在循环：" + current);

                }

                current = nextMap.get(current);

            }

        }

    }



    private void validateSupportedCompetitionFormat(BasketDivisionStage stage) {

        String format = stage.getCompetitionFormat();

        if (CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(format)

                || CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(format)) {

            return;

        }

        throw new ServiceException("阶段「" + stage.getStageName() + "」暂不支持该比赛方式，"

                + "当前仅允许单循环（ROUND_ROBIN）、小组赛（分组循环）和 BO1 单败淘汰（SINGLE_ELIMINATION）");

    }



    public static void validateBestOf(Integer bestOf) {

        if (bestOf == null || bestOf != 1) {

            throw new ServiceException("当前版本仅支持 BO1，BO3/BO5 尚未开放。");

        }

    }

}

