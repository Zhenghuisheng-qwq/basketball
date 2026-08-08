package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.support.v4.MatchSeriesExecutionCapability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 单轮非最终淘汰阶段奇数队轮空（BYE）规则：最高 seed 轮空，确定性 HIGH_VS_LOW 配对。
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
