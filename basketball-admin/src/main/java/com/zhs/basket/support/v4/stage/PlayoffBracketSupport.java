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

    /** CNPA E2E 锁定的 16 队首轮矩阵（不可改）。 */
    private static final int[] LOCKED_SEED_ORDER_16 = {
            1, 16, 4, 13, 5, 12, 8, 9, 6, 11, 7, 10, 2, 15, 3, 14
    };

    /**
     * 2 的次幂首轮 Series 落位顺序（连续两两配对）。
     * 4/8 为标准 mirror 递归；16 使用 E2E 锁定矩阵（历史兼容，不作为 32+ 的展开源）。
     * 32+ 使用标准 recursive mirror，保证 Seed1/Seed2 分半区、Seed1/Seed4 与 Seed2/Seed3 不早于 SF 相遇。
     */
    public static int[] firstRoundSeedOrder(int teamCount) {
        if (!supportsTeamCount(teamCount)) {
            throw new ServiceException("季后赛固定淘汰树仅支持 2 的次幂队伍数（>=2），当前 " + teamCount + " 支");
        }
        if (teamCount == 16) {
            return LOCKED_SEED_ORDER_16.clone();
        }
        return buildRecursiveSeedOrder(teamCount);
    }

    /** 标准 mirror：每一档 [s] → [s, n+1-s]。 */
    static int[] expandSeedOrder(int[] prev) {
        int n = prev.length * 2;
        int sum = n + 1;
        int[] out = new int[n];
        int i = 0;
        for (int seed : prev) {
            out[i++] = seed;
            out[i++] = sum - seed;
        }
        return out;
    }

    static int[] buildRecursiveSeedOrder(int teamCount) {
        int[] order = new int[]{1};
        while (order.length < teamCount) {
            order = expandSeedOrder(order);
        }
        return order;
    }

    public static boolean supportsTeamCount(int teamCount) {
        return teamCount >= 2 && (teamCount & (teamCount - 1)) == 0;
    }

    /**
     * 是否按球队创建时间赋种子。
     * 首阶段默认创建时间；后续胜者路径不重排；仅当 stageCode 含 _TCT 时，对积分晋级名单按创建时间重新赋种。
     */
    public static boolean useTeamCreateTimeSeed(String stageCode, boolean firstStage, boolean winnerPathInherit) {
        if (firstStage) {
            return true;
        }
        if (winnerPathInherit) {
            return false;
        }
        return stageCode != null && stageCode.toUpperCase().contains("_TCT");
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
            throw new ServiceException("季后赛固定淘汰树仅支持 2 的次幂队伍数（>=2），当前 " + n + " 支");
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
