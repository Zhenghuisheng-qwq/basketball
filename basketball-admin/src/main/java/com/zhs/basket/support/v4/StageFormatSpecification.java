package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;

/**
 * Stage 三维赛制模型校验：organization_mode + competition_format + best_of。
 */
public final class StageFormatSpecification {

    private static final String GROUPED_ELIM_MSG =
            "当前版本暂不支持分组内淘汰赛，请选择分组循环积分赛或不分组单败淘汰赛。";

    private StageFormatSpecification() {
    }

    public static void requireExplicitOrganizationMode(String organizationMode) {
        if (StringUtils.isEmpty(organizationMode)) {
            throw new ServiceException("organizationMode 不能为空，请明确选择分组（GROUPED）或不分组（UNGROUPED）");
        }
        OrganizationModeEnum.fromCode(organizationMode);
    }

    public static void validateOrganizationAndGrouping(BasketDivisionStage stage) {
        String orgMode = OrganizationModeSupport.resolveOrganizationMode(stage);
        GroupingModeEnum grouping = GroupingModeEnum.fromCode(stage.getGroupingMode());
        if (grouping == null) {
            throw new ServiceException("阶段「" + stage.getStageName() + "」分组方式无效");
        }

        if (OrganizationModeEnum.UNGROUPED.getCode().equals(orgMode)) {
            if (grouping != GroupingModeEnum.NONE) {
                throw new ServiceException("阶段「" + stage.getStageName()
                        + "」不分组时 grouping_mode 必须为 NONE");
            }
            if (stage.getGroupCount() != null && stage.getGroupCount() > 0) {
                throw new ServiceException("阶段「" + stage.getStageName() + "」不分组时不应配置 group_count");
            }
        } else if (OrganizationModeEnum.GROUPED.getCode().equals(orgMode)) {
            if (grouping == GroupingModeEnum.NONE) {
                throw new ServiceException("阶段「" + stage.getStageName()
                        + "」分组时 grouping_mode 不能为 NONE");
            }
            if (stage.getGroupCount() == null || stage.getGroupCount() <= 0) {
                throw new ServiceException("阶段「" + stage.getStageName() + "」分组时需配置有效 group_count");
            }
        } else {
            throw new ServiceException("阶段「" + stage.getStageName() + "」organization_mode 无效");
        }
    }

    public static void validateCompetitionFormat(BasketDivisionStage stage, boolean rejectManualForNewWrite) {
        String format = stage.getCompetitionFormat();
        if (CompetitionFormatEnum.MANUAL.getCode().equals(format) && rejectManualForNewWrite) {
            throw new ServiceException("阶段「" + stage.getStageName() + "」暂不支持 MANUAL 赛制新写入");
        }
        DoubleEliminationCapabilityGate.assertNotReservedFormat(format);
        if (!CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(format)
                && !CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(format)
                && !CompetitionFormatEnum.MANUAL.getCode().equals(format)) {
            throw new ServiceException("阶段「" + stage.getStageName() + "」暂不支持该比赛方式");
        }

        String orgMode = OrganizationModeSupport.resolveOrganizationMode(stage);
        if (OrganizationModeEnum.GROUPED.getCode().equals(orgMode)
                && CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(format)) {
            throw new ServiceException(GROUPED_ELIM_MSG);
        }
    }

    public static void validateAllowedCombination(BasketDivisionStage stage) {
        String orgMode = OrganizationModeSupport.resolveOrganizationMode(stage);
        String format = stage.getCompetitionFormat();
        boolean allowed = (OrganizationModeEnum.UNGROUPED.getCode().equals(orgMode)
                        && CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(format))
                || (OrganizationModeEnum.GROUPED.getCode().equals(orgMode)
                        && CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(format))
                || (OrganizationModeEnum.UNGROUPED.getCode().equals(orgMode)
                        && CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(format))
                || CompetitionFormatEnum.MANUAL.getCode().equals(format);
        if (!allowed) {
            throw new ServiceException(GROUPED_ELIM_MSG);
        }
    }

    public static void validateRoundRobinRules(BasketDivisionStage stage, boolean isLastStage) {
        if (!CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat())) {
            return;
        }
        if (stage.getRoundRobinCycles() == null || stage.getRoundRobinCycles() <= 0) {
            throw new ServiceException("循环赛阶段必须配置 round_robin_cycles");
        }
        if (stage.getWinPoints() == null || stage.getLossPoints() == null) {
            throw new ServiceException("阶段「" + stage.getStageName() + "」需配置胜负积分");
        }
        AdvancementTypeEnum adv = AdvancementTypeEnum.fromCode(stage.getAdvancementType());
        if (isLastStage) {
            return;
        }
        String orgMode = OrganizationModeSupport.resolveOrganizationMode(stage);
        if (OrganizationModeEnum.GROUPED.getCode().equals(orgMode)) {
            if (adv != AdvancementTypeEnum.GROUP_TOP_N) {
                throw new ServiceException("阶段「" + stage.getStageName() + "」分组循环赛晋级方式必须为 GROUP_TOP_N");
            }
        } else {
            if (adv != AdvancementTypeEnum.OVERALL_TOP_N) {
                throw new ServiceException("阶段「" + stage.getStageName() + "」整体循环赛晋级方式必须为 OVERALL_TOP_N");
            }
        }
    }

    public static void validateEliminationRules(BasketDivisionStage stage, boolean isLastStage) {
        if (!CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(stage.getCompetitionFormat())) {
            return;
        }
        String orgMode = OrganizationModeSupport.resolveOrganizationMode(stage);
        if (!OrganizationModeEnum.UNGROUPED.getCode().equals(orgMode)) {
            throw new ServiceException(GROUPED_ELIM_MSG);
        }
        AdvancementTypeEnum adv = AdvancementTypeEnum.fromCode(stage.getAdvancementType());
        if (isLastStage) {
            if (adv != AdvancementTypeEnum.NONE) {
                throw new ServiceException("最后阶段「" + stage.getStageName() + "」不应再配置晋级规则");
            }
        } else {
            if (adv != AdvancementTypeEnum.WINNER_ADVANCE && adv != AdvancementTypeEnum.SERIES_WINNER) {
                throw new ServiceException("阶段「" + stage.getStageName()
                        + "」淘汰赛非最终阶段晋级方式必须为 WINNER_ADVANCE");
            }
        }
    }

    public static void validateAdvancementCount(BasketDivisionStage stage) {
        AdvancementTypeEnum adv = AdvancementTypeEnum.fromCode(stage.getAdvancementType());
        if (adv == null) {
            throw new ServiceException("阶段「" + stage.getStageName() + "」晋级方式无效");
        }
        if (adv == AdvancementTypeEnum.SERIES_WINNER) {
            return;
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

    public static void validateChainAdvancement(BasketDivisionStage stage, boolean isLastStage) {
        AdvancementTypeEnum adv = AdvancementTypeEnum.fromCode(stage.getAdvancementType());
        boolean hasAdvancement = adv != null && adv != AdvancementTypeEnum.NONE;

        if (isLastStage) {
            if (hasAdvancement) {
                throw new ServiceException("最后阶段「" + stage.getStageName() + "」不应再配置晋级规则");
            }
        } else {
            if (!hasAdvancement) {
                throw new ServiceException("阶段「" + stage.getStageName() + "」不是最后阶段，请配置晋级规则");
            }
            if (CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(stage.getCompetitionFormat())
                    && adv == AdvancementTypeEnum.NONE) {
                throw new ServiceException("阶段「" + stage.getStageName()
                        + "」淘汰赛非最终阶段不能配置 NONE 晋级");
            }
        }
    }

    public static boolean isGrouped(BasketDivisionStage stage) {
        return OrganizationModeEnum.GROUPED.getCode()
                .equals(OrganizationModeSupport.resolveOrganizationMode(stage));
    }
}
