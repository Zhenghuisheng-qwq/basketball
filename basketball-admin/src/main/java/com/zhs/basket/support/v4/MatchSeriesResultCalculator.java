package com.zhs.basket.support.v4;

import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.common.exception.ServiceException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BO3/BO5 系列赛结果纯计算器：无数据库副作用。
 */
public final class MatchSeriesResultCalculator {

    private MatchSeriesResultCalculator() {
    }

    public static int requiredWins(int bestOf) {
        return BestOfValues.requiredWins(bestOf);
    }

    public static MatchSeriesResultSnapshot calculate(int bestOf,
                                                      Long homeEventTeamId,
                                                      Long awayEventTeamId,
                                                      List<MatchSeriesGameInput> games) {
        validateSeriesTeams(bestOf, homeEventTeamId, awayEventTeamId);

        int required = requiredWins(bestOf);
        int homeWins = 0;
        int awayWins = 0;
        Long winner = null;
        boolean completed = false;
        Set<Integer> seenGameNos = new HashSet<>();

        if (games != null) {
            List<MatchSeriesGameInput> sorted = games.stream()
                    .sorted((a, b) -> Integer.compare(
                            a.getSeriesGameNo() != null ? a.getSeriesGameNo() : 0,
                            b.getSeriesGameNo() != null ? b.getSeriesGameNo() : 0))
                    .toList();

            for (MatchSeriesGameInput game : sorted) {
                if (completed) {
                    continue;
                }
                validateGame(game, homeEventTeamId, awayEventTeamId, seenGameNos);

                if (!isEffectiveResult(game.getMatchStatus())) {
                    continue;
                }

                Long gameWinner = game.getWinnerEventTeamId();
                if (gameWinner == null) {
                    throw new ServiceException("有效比赛必须存在 winnerEventTeamId");
                }
                if (gameWinner.equals(homeEventTeamId)) {
                    homeWins++;
                } else if (gameWinner.equals(awayEventTeamId)) {
                    awayWins++;
                } else {
                    throw new ServiceException("比赛胜者不属于系列赛双方");
                }

                if (homeWins >= required && awayWins >= required) {
                    throw new ServiceException("系列赛双方不能同时达到胜场阈值");
                }
                if (homeWins >= required) {
                    winner = homeEventTeamId;
                    completed = true;
                } else if (awayWins >= required) {
                    winner = awayEventTeamId;
                    completed = true;
                }
            }
        }

        if (homeWins + awayWins > bestOf) {
            throw new ServiceException("系列赛胜场总数不能超过 best_of");
        }

        MatchSeriesResultSnapshot snapshot = new MatchSeriesResultSnapshot();
        snapshot.setRequiredWins(required);
        snapshot.setHomeWinCount(homeWins);
        snapshot.setAwayWinCount(awayWins);
        snapshot.setWinnerEventTeamId(winner);
        snapshot.setCompleted(completed);
        return snapshot;
    }

    private static void validateSeriesTeams(int bestOf, Long homeEventTeamId, Long awayEventTeamId) {
        requiredWins(bestOf);
        if (homeEventTeamId == null || awayEventTeamId == null) {
            throw new ServiceException("系列赛双方球队不能为空");
        }
        if (homeEventTeamId.equals(awayEventTeamId)) {
            throw new ServiceException("系列赛主客队不能相同");
        }
    }

    private static void validateGame(MatchSeriesGameInput game,
                                     Long homeEventTeamId,
                                     Long awayEventTeamId,
                                     Set<Integer> seenGameNos) {
        if (game.getSeriesGameNo() == null || game.getSeriesGameNo() < 1) {
            throw new ServiceException("series_game_no 必须 >= 1");
        }
        if (!seenGameNos.add(game.getSeriesGameNo())) {
            throw new ServiceException("series_game_no 重复: " + game.getSeriesGameNo());
        }
        if (game.getHomeEventTeamId() == null || game.getAwayEventTeamId() == null) {
            throw new ServiceException("比赛双方不能为空");
        }
        if (!homeEventTeamId.equals(game.getHomeEventTeamId())
                || !awayEventTeamId.equals(game.getAwayEventTeamId())) {
            throw new ServiceException("子比赛双方必须与系列赛双方一致");
        }
    }

    private static boolean isEffectiveResult(String matchStatus) {
        return MatchStatusEnum.FINISHED.getCode().equals(matchStatus)
                || MatchStatusEnum.FORFEITED.getCode().equals(matchStatus);
    }
}
