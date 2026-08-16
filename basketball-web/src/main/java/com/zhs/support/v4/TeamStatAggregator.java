package com.zhs.support.v4;

import com.zhs.model.v4.BasketMatchSubmissionTeamStat;
import com.zhs.request.v4.V4MatchSubmissionPlayerStatReq;
import com.zhs.response.v4.V4MatchSubmissionTeamStatResp;

import java.util.List;

/**
 * 球队统计以后端 SUM 球员数据为准，不信任前端传入的 Team Stat。
 */
public final class TeamStatAggregator {

    private TeamStatAggregator() {
    }

    public static BasketMatchSubmissionTeamStat aggregate(List<V4MatchSubmissionPlayerStatReq> rows) {
        BasketMatchSubmissionTeamStat stat = new BasketMatchSubmissionTeamStat();
        apply(stat, rows);
        return stat;
    }

    public static void apply(BasketMatchSubmissionTeamStat stat, List<V4MatchSubmissionPlayerStatReq> rows) {
        int score = 0, reb = 0, ast = 0, stl = 0, blk = 0, tov = 0, pf = 0;
        int fgm = 0, fga = 0, tpm = 0, tpa = 0, ftm = 0, fta = 0;
        for (V4MatchSubmissionPlayerStatReq row : FullMatchSubmissionRules.usedRows(rows)) {
            score += FullMatchSubmissionRules.nvl(row.getPoints());
            reb += FullMatchSubmissionRules.nvl(row.getRebound());
            ast += FullMatchSubmissionRules.nvl(row.getAssist());
            stl += FullMatchSubmissionRules.nvl(row.getSteal());
            blk += FullMatchSubmissionRules.nvl(row.getBlock());
            tov += FullMatchSubmissionRules.nvl(row.getTurnover());
            pf += FullMatchSubmissionRules.nvl(row.getFoul());
            fgm += FullMatchSubmissionRules.nvl(row.getFieldGoalMade());
            fga += FullMatchSubmissionRules.nvl(row.getFieldGoalAttempt());
            tpm += FullMatchSubmissionRules.nvl(row.getThreePointMade());
            tpa += FullMatchSubmissionRules.nvl(row.getThreePointAttempt());
            ftm += FullMatchSubmissionRules.nvl(row.getFreeThrowMade());
            fta += FullMatchSubmissionRules.nvl(row.getFreeThrowAttempt());
        }
        stat.setScore(score);
        stat.setRebound(reb);
        stat.setAssist(ast);
        stat.setSteal(stl);
        stat.setBlock(blk);
        stat.setTurnover(tov);
        stat.setFoul(pf);
        stat.setFieldGoalMade(fgm);
        stat.setFieldGoalAttempt(fga);
        stat.setThreePointMade(tpm);
        stat.setThreePointAttempt(tpa);
        stat.setFreeThrowMade(ftm);
        stat.setFreeThrowAttempt(fta);
    }

    public static V4MatchSubmissionTeamStatResp toResp(BasketMatchSubmissionTeamStat stat) {
        V4MatchSubmissionTeamStatResp resp = new V4MatchSubmissionTeamStatResp();
        if (stat == null) {
            return resp;
        }
        resp.setScore(stat.getScore());
        resp.setRebound(stat.getRebound());
        resp.setAssist(stat.getAssist());
        resp.setSteal(stat.getSteal());
        resp.setBlock(stat.getBlock());
        resp.setTurnover(stat.getTurnover());
        resp.setFoul(stat.getFoul());
        resp.setFieldGoalMade(stat.getFieldGoalMade());
        resp.setFieldGoalAttempt(stat.getFieldGoalAttempt());
        resp.setThreePointMade(stat.getThreePointMade());
        resp.setThreePointAttempt(stat.getThreePointAttempt());
        resp.setFreeThrowMade(stat.getFreeThrowMade());
        resp.setFreeThrowAttempt(stat.getFreeThrowAttempt());
        return resp;
    }
}
