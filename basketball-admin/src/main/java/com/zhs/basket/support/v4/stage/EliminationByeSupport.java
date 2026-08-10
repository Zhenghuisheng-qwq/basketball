package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.support.v4.MatchSeriesExecutionCapability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 单轮非最终淘汰阶段奇数队轮空（BYE）规则：排序后首位轮空，再按首尾配对。
 * HIGH_VS_LOW 按 seed 排序；RANDOM_DRAW 先随机打乱顺序再复用同一配对/BYE 逻辑。
 */
public final class EliminationByeSupport {

    private EliminationByeSupport() {
    }

    public static boolean allowsBye(BasketDivisionStage stage, int teamCount) {
        if (teamCount < 3 || teamCount % 2 == 0) {
            return false;
        }
        if (EliminationStageSupport.isFinalEliminationStage(stage)) {
            return false;
        }
        return EliminationStageSupport.isSingleRoundNonFinalStage(stage)
                || MatchSeriesExecutionCapability.supportsScheduleGeneration(stage);
    }

    /**
     * 按对阵规则确定进入 bracket/pairing 的队伍顺序。
     * RANDOM_DRAW：复制后 shuffle（不修改入参）；其余（含空/历史缺省）按 seed 排序。
     */
    public static List<BasketDivisionStageTeam> orderTeamsForPairing(List<BasketDivisionStageTeam> teams,
                                                                     String pairingRule) {
        return orderTeamsForPairing(teams, pairingRule, null);
    }

    public static List<BasketDivisionStageTeam> orderTeamsForPairing(List<BasketDivisionStageTeam> teams,
                                                                     String pairingRule,
                                                                     Random random) {
        if (PairingRuleEnum.RANDOM_DRAW.getCode().equals(pairingRule)) {
            List<BasketDivisionStageTeam> shuffled = new ArrayList<>(teams);
            if (random != null) {
                Collections.shuffle(shuffled, random);
            } else {
                Collections.shuffle(shuffled);
            }
            return shuffled;
        }
        return sortTeams(teams);
    }

    public static List<BasketDivisionStageTeam> sortTeams(List<BasketDivisionStageTeam> teams) {
        return teams.stream()
                .sorted(Comparator
                        .comparing(BasketDivisionStageTeam::getSeedNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(BasketDivisionStageTeam::getEventTeamId))
                .toList();
    }

    /** 轮空队：排序后第一支（最高 seed）。 */
    public static Long resolveByeEventTeamId(List<BasketDivisionStageTeam> sortedTeams) {
        return sortedTeams.get(0).getEventTeamId();
    }

    /** 参与本轮配对的球队（排除 BYE 队）。 */
    public static List<BasketDivisionStageTeam> pairingTeams(List<BasketDivisionStageTeam> sortedTeams, Long byeEventTeamId) {
        List<BasketDivisionStageTeam> result = new ArrayList<>();
        for (BasketDivisionStageTeam team : sortedTeams) {
            if (!team.getEventTeamId().equals(byeEventTeamId)) {
                result.add(team);
            }
        }
        return result;
    }

    public static int pairingSlotCount(int teamCount) {
        return teamCount / 2;
    }
}
