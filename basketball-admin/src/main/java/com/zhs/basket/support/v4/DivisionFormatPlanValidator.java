package com.zhs.basket.support.v4;

import com.zhs.basket.dto.v4.BasketDivisionFormatPlanApplyReq;
import com.zhs.basket.enums.v4.FormatPlanCodeEnum;
import com.zhs.basket.vo.v4.FormatPlanValidationResultVo;
import org.springframework.stereotype.Component;

/**
 * 赛制组合方案统一校验器：供 applyFormatPlan、lockSnapshot 预览等共用。
 */
@Component
public class DivisionFormatPlanValidator {

    public FormatPlanValidationResultVo validate(BasketDivisionFormatPlanApplyReq req) {
        FormatPlanValidationResultVo result = new FormatPlanValidationResultVo();
        if (req == null || req.getPlanCode() == null || req.getPlanCode().isEmpty()) {
            result.getErrors().add("请选择赛制方案");
            result.setValid(false);
            return result;
        }
        FormatPlanCodeEnum planCode;
        try {
            planCode = FormatPlanCodeEnum.fromCode(req.getPlanCode());
        } catch (Exception ex) {
            result.getErrors().add(ex.getMessage());
            result.setValid(false);
            return result;
        }

        int expectedTeamCount = req.getExpectedTeamCount() == null ? 0 : req.getExpectedTeamCount();
        if (expectedTeamCount < 2) {
            result.getErrors().add("预计参赛队伍至少需要 2 支");
        }

        int winPoints = req.getWinPoints() == null ? 2 : req.getWinPoints();
        int lossPoints = req.getLossPoints() == null ? 1 : req.getLossPoints();
        if (winPoints < 0 || lossPoints < 0) {
            result.getErrors().add("胜负积分不能为负数");
        }

        int roundRobinLegs = req.getRoundRobinLegs() == null ? 1 : req.getRoundRobinLegs();
        if (roundRobinLegs <= 0) {
            result.getErrors().add("循环次数必须大于 0");
        }

        int qualifierCount = 0;
        int groupStageMatches = 0;
        int elimMatches = 0;
        Integer expectedGroupSize = null;

        if (planCode == FormatPlanCodeEnum.GROUP_TO_ELIM) {
            int groupCount = req.getGroupCount() == null ? 0 : req.getGroupCount();
            int qualifiersPerGroup = req.getQualifiersPerGroup() == null ? 0 : req.getQualifiersPerGroup();
            expectedGroupSize = calcTeamsPerGroup(expectedTeamCount, groupCount);
            qualifierCount = groupCount * qualifiersPerGroup;

            if (groupCount <= 1) {
                result.getErrors().add("分组数量必须大于 1");
            }
            if (groupCount > expectedTeamCount) {
                result.getErrors().add("分组数量不能大于预计参赛队伍数");
            }
            if (qualifiersPerGroup <= 0) {
                result.getErrors().add("每组晋级数量必须大于 0");
            }
            if (qualifiersPerGroup > expectedGroupSize) {
                result.getErrors().add(String.format("每组晋级数（%d）不能大于预计每组队伍数（约 %d 支）",
                        qualifiersPerGroup, expectedGroupSize));
            }
            groupStageMatches = calcGroupRoundRobinMatches(expectedTeamCount, groupCount, roundRobinLegs);
        } else if (planCode == FormatPlanCodeEnum.ROUND_ROBIN_TO_ELIM) {
            qualifierCount = req.getOverallAdvanceCount() == null ? 0 : req.getOverallAdvanceCount();
            if (qualifierCount <= 0) {
                result.getErrors().add("晋级名额必须大于 0");
            }
            if (qualifierCount > expectedTeamCount) {
                result.getErrors().add("晋级名额不能大于预计参赛队伍数");
            }
            groupStageMatches = calcFullRoundRobinMatches(expectedTeamCount, roundRobinLegs);
        }

        if (qualifierCount > 0 && !isPowerOfTwo(qualifierCount)) {
            result.getErrors().add(String.format(
                    "当前将有 %d 支球队晋级，但系统暂不支持 %d 支球队自动生成单败淘汰赛。请调整为 4 支或 8 支晋级。",
                    qualifierCount, qualifierCount));
        }

        if (qualifierCount >= 2) {
            elimMatches = qualifierCount - 1;
        }
        result.setQualifierCount(qualifierCount > 0 ? qualifierCount : null);
        result.setExpectedGroupSize(expectedGroupSize);
        result.setExpectedGroupMatchCount(groupStageMatches > 0 ? groupStageMatches : null);
        result.setExpectedEliminationMatchCount(elimMatches > 0 ? elimMatches : null);
        result.setExpectedTotalMatchCount(groupStageMatches + elimMatches > 0 ? groupStageMatches + elimMatches : null);
        result.setEliminationStartRound(qualifierCount >= 2 ? eliminationRoundLabel(qualifierCount) : null);
        result.setValid(result.getErrors().isEmpty());
        return result;
    }

    public FormatPlanValidationResultVo validateStages(int maxTeamCount,
                                                       String firstAdvancementType,
                                                       Integer firstAdvancementCount,
                                                       Integer firstGroupCount,
                                                       boolean hasEliminationSecondStage) {
        FormatPlanValidationResultVo result = new FormatPlanValidationResultVo();
        if (!hasEliminationSecondStage) {
            result.setValid(true);
            return result;
        }
        int qualifierCount = 0;
        if ("GROUP_TOP_N".equalsIgnoreCase(firstAdvancementType)) {
            int groupCount = firstGroupCount == null ? 0 : firstGroupCount;
            int perGroup = firstAdvancementCount == null ? 0 : firstAdvancementCount;
            qualifierCount = groupCount * perGroup;
        } else if ("OVERALL_TOP_N".equalsIgnoreCase(firstAdvancementType)) {
            qualifierCount = firstAdvancementCount == null ? 0 : firstAdvancementCount;
        }
        if (qualifierCount > 0 && !isPowerOfTwo(qualifierCount)) {
            result.getErrors().add(String.format(
                    "当前将有 %d 支球队晋级，但系统暂不支持 %d 支球队自动生成单败淘汰赛。请调整为 4 支或 8 支晋级。",
                    qualifierCount, qualifierCount));
        }
        result.setQualifierCount(qualifierCount > 0 ? qualifierCount : null);
        result.setValid(result.getErrors().isEmpty());
        return result;
    }

    static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    static int calcTeamsPerGroup(int teamCount, int groupCount) {
        if (groupCount <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) teamCount / groupCount);
    }

    static int calcGroupRoundRobinMatches(int teamCount, int groupCount, int cycles) {
        if (groupCount <= 0 || teamCount <= 0) {
            return 0;
        }
        int base = teamCount / groupCount;
        int remainder = teamCount % groupCount;
        int matches = 0;
        for (int i = 0; i < groupCount; i++) {
            int size = base + (i < remainder ? 1 : 0);
            if (size >= 2) {
                matches += size * (size - 1) / 2;
            }
        }
        return matches * Math.max(cycles, 1);
    }

    static int calcFullRoundRobinMatches(int teamCount, int cycles) {
        if (teamCount < 2) {
            return 0;
        }
        return teamCount * (teamCount - 1) / 2 * Math.max(cycles, 1);
    }

    static String eliminationRoundLabel(int teamCount) {
        if (teamCount <= 2) {
            return "决赛";
        }
        if (teamCount == 4) {
            return "4 强";
        }
        if (teamCount == 8) {
            return "8 强";
        }
        if (teamCount == 16) {
            return "16 强";
        }
        return teamCount + " 支队伍";
    }
}
