package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.enums.v4.StageTeamEntryTypeEnum;
import com.zhs.common.exception.ServiceException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 季后赛固定淘汰树：首轮按象限落位，后续轮按相邻 Series Winner 继承（禁止重排/重抽）。
 */
public final class PlayoffBracketSupport {

    private PlayoffBracketSupport() {
    }

    public static boolean isPlayoffBracket(String pairingRule) {
        return PairingRuleEnum.PLAYOFF_BRACKET.getCode().equals(pairingRule);
    }

    /**
     * 4/8/16 首轮 Series 落位顺序（连续两两配对）。
     * 16: 1/16, 4/13, 5/12, 8/9, 6/11, 7/10, 2/15, 3/14
     */
    public static int[] firstRoundSeedOrder(int teamCount) {
        if (teamCount == 4) {
            return new int[]{1, 4, 2, 3};
        }
        if (teamCount == 8) {
            return new int[]{1, 8, 4, 5, 2, 7, 3, 6};
        }
        if (teamCount == 16) {
            return new int[]{1, 16, 4, 13, 5, 12, 8, 9, 6, 11, 7, 10, 2, 15, 3, 14};
        }
        throw new ServiceException("季后赛固定淘汰树当前仅支持 4 / 8 / 16 支队伍，当前 " + teamCount + " 支");
    }

    public static boolean supportsTeamCount(int teamCount) {
        return teamCount == 4 || teamCount == 8 || teamCount == 16;
    }

    /**
     * 是否继承上一淘汰轮固定路径（相邻 Series Winner）。
     * 来自积分/初始名单的首轮必须走象限矩阵；仅 MATCH/SERIES/BYE 胜者晋级才相邻配对。
     */
    public static boolean isBracketPathInherit(List<BasketDivisionStageTeam> teams) {
        if (teams == null || teams.isEmpty()) {
            return false;
        }
        return teams.stream().allMatch(PlayoffBracketSupport::isWinnerAdvanceEntry);
    }

    private static boolean isWinnerAdvanceEntry(BasketDivisionStageTeam team) {
        if (team == null || team.getEntryType() == null) {
            return false;
        }
        String et = team.getEntryType();
        return StageTeamEntryTypeEnum.MATCH_WINNER.getCode().equals(et)
                || StageTeamEntryTypeEnum.SERIES_WINNER.getCode().equals(et)
                || StageTeamEntryTypeEnum.BYE_ADVANCE.getCode().equals(et);
    }

    /**
     * 进入配对前的队伍顺序。
     * 首轮（初始/积分种子）：按种子象限落位；
     * 后续轮（淘汰胜者晋级）：仅按 seedNo 排序，供相邻配对。
     */
    public static List<BasketDivisionStageTeam> orderTeams(List<BasketDivisionStageTeam> teams) {
        List<BasketDivisionStageTeam> sorted = EliminationByeSupport.sortTeams(teams);
        if (sorted.isEmpty()) {
            return sorted;
        }
        if (isBracketPathInherit(sorted)) {
            return sorted;
        }
        int n = sorted.size();
        if (!supportsTeamCount(n)) {
            throw new ServiceException("季后赛固定淘汰树当前仅支持 4 / 8 / 16 支队伍，当前 " + n + " 支");
        }
        Map<Integer, BasketDivisionStageTeam> bySeed = new HashMap<>();
        for (BasketDivisionStageTeam team : sorted) {
            if (team.getSeedNo() == null) {
                throw new ServiceException("季后赛队伍缺少 seedNo，无法生成固定淘汰树");
            }
            if (bySeed.put(team.getSeedNo(), team) != null) {
                throw new ServiceException("季后赛种子号重复: " + team.getSeedNo());
            }
        }
        int[] order = firstRoundSeedOrder(n);
        List<BasketDivisionStageTeam> arranged = new ArrayList<>(n);
        for (int seed : order) {
            BasketDivisionStageTeam team = bySeed.get(seed);
            if (team == null) {
                throw new ServiceException("季后赛缺少种子 #" + seed);
            }
            arranged.add(team);
        }
        return arranged;
    }

    /** 相邻配对：slot → (2*slot, 2*slot+1)。 */
    public static void resolveAdjacentPair(List<BasketDivisionStageTeam> ordered,
                                           int slot,
                                           BasketDivisionStageTeam[] outHomeAway) {
        int homeIdx = slot * 2;
        int awayIdx = homeIdx + 1;
        if (awayIdx >= ordered.size()) {
            throw new ServiceException("季后赛对阵槽位越界: slot=" + slot);
        }
        outHomeAway[0] = ordered.get(homeIdx);
        outHomeAway[1] = ordered.get(awayIdx);
    }

    public static boolean useAdjacentPairing(String pairingRule) {
        return isPlayoffBracket(pairingRule);
    }
}
