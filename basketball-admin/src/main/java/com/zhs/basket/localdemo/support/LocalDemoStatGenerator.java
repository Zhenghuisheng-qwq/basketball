package com.zhs.basket.localdemo.support;

import com.zhs.basket.localdemo.dto.DemoPlayerDto;
import com.zhs.basket.req.MatchPlayerStatSaveReq;
import com.zhs.basket.req.MatchTeamStatSaveReq;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 根据最终比分与位置，稳定生成可校验的技术统计数据。
 */
public final class LocalDemoStatGenerator {

    private LocalDemoStatGenerator() {
    }

    public static MatchPlayerStatSaveReq buildPlayerStats(Long matchId, String matchNo, Long teamId,
                                                          List<DemoPlayerDto> roster, int teamScore, boolean home) {
        MatchPlayerStatSaveReq req = new MatchPlayerStatSaveReq();
        List<MatchPlayerStatSaveReq.PlayerStatItem> stats = new ArrayList<>();
        int[] points = LocalDemoSeedDefinitions.allocatePlayerPoints(teamScore, LocalDemoSeedDefinitions.SCORE_WEIGHTS);
        for (int i = 0; i < roster.size(); i++) {
            DemoPlayerDto player = roster.get(i);
            int minutes = i < 5 ? LocalDemoSeedDefinitions.STARTER_MINUTES[i] : LocalDemoSeedDefinitions.BENCH_MINUTES[i - 5];
            long seed = stableSeed(matchNo, player.getGlobalIndex());
            MatchPlayerStatSaveReq.PlayerStatItem item = buildPlayerItem(teamId, player, points[i], minutes, seed);
            stats.add(item);
        }
        req.setStats(stats);
        return req;
    }

    public static MatchTeamStatSaveReq aggregateTeamStats(Long teamId,
                                                          List<MatchPlayerStatSaveReq.PlayerStatItem> playerStats) {
        MatchTeamStatSaveReq req = new MatchTeamStatSaveReq();
        MatchTeamStatSaveReq.TeamStatItem item = new MatchTeamStatSaveReq.TeamStatItem();
        item.setTeamId(teamId);
        int points = 0, reb = 0, oreb = 0, dreb = 0, ast = 0, stl = 0, blk = 0, to = 0, pf = 0;
        int fgm = 0, fga = 0, tpm = 0, tpa = 0, ftm = 0, fta = 0;
        for (MatchPlayerStatSaveReq.PlayerStatItem ps : playerStats) {
            points += val(ps.getPoints());
            reb += val(ps.getRebounds());
            oreb += val(ps.getOffensiveRebounds());
            dreb += val(ps.getDefensiveRebounds());
            ast += val(ps.getAssists());
            stl += val(ps.getSteals());
            blk += val(ps.getBlocks());
            to += val(ps.getTurnovers());
            pf += val(ps.getFouls());
            fgm += val(ps.getFieldGoalsMade());
            fga += val(ps.getFieldGoalsAttempted());
            tpm += val(ps.getThreePointsMade());
            tpa += val(ps.getThreePointsAttempted());
            ftm += val(ps.getFreeThrowsMade());
            fta += val(ps.getFreeThrowsAttempted());
        }
        item.setPoints(points);
        item.setRebounds(reb);
        item.setOffensiveRebounds(oreb);
        item.setDefensiveRebounds(dreb);
        item.setAssists(ast);
        item.setSteals(stl);
        item.setBlocks(blk);
        item.setTurnovers(to);
        item.setFouls(pf);
        item.setFieldGoalsMade(fgm);
        item.setFieldGoalsAttempted(fga);
        item.setThreePointsMade(tpm);
        item.setThreePointsAttempted(tpa);
        item.setFreeThrowsMade(ftm);
        item.setFreeThrowsAttempted(fta);
        req.setStats(List.of(item));
        return req;
    }

    private static MatchPlayerStatSaveReq.PlayerStatItem buildPlayerItem(Long teamId, DemoPlayerDto player,
                                                                         int points, int minutes, long seed) {
        Random random = new Random(seed);
        MatchPlayerStatSaveReq.PlayerStatItem item = new MatchPlayerStatSaveReq.PlayerStatItem();
        item.setTeamId(teamId);
        item.setPlayerId(null);
        item.setStartedFlag(minutes >= 26 ? 1 : 0);
        item.setMinutesSeconds(minutes * 60);

        Shooting shooting = deriveShooting(points, player.getPositionCode(), random);
        item.setPoints(points);
        item.setThreePointsMade(shooting.tpm);
        item.setThreePointsAttempted(shooting.tpa);
        item.setFreeThrowsMade(shooting.ftm);
        item.setFreeThrowsAttempted(shooting.fta);
        item.setFieldGoalsMade(shooting.tpm + shooting.twoPm);
        item.setFieldGoalsAttempted(shooting.tpa + shooting.twoPa);

        item.setAssists(baseByPosition(player.getPositionCode(), 2, 8, random));
        item.setRebounds(baseByPosition(player.getPositionCode(), 1, 12, random));
        item.setOffensiveRebounds(Math.max(0, item.getRebounds() / 3));
        item.setDefensiveRebounds(item.getRebounds() - item.getOffensiveRebounds());
        item.setSteals(baseByPosition(player.getPositionCode(), 0, 3, random));
        item.setBlocks(baseByPosition("C".equals(player.getPositionCode()) ? "C" : "X", 0, 3, random));
        item.setTurnovers(baseByPosition(player.getPositionCode(), 0, 4, random));
        item.setFouls(baseByPosition(player.getPositionCode(), 1, 5, random));
        item.setPlusMinus(0);
        return item;
    }

    public static void bindPlayerIds(MatchPlayerStatSaveReq req, List<Long> playerIds) {
        for (int i = 0; i < req.getStats().size(); i++) {
            req.getStats().get(i).setPlayerId(playerIds.get(i));
        }
    }

    private static int baseByPosition(String position, int min, int max, Random random) {
        int base = min + random.nextInt(Math.max(1, max - min + 1));
        if ("PG".equals(position)) {
            base += 1;
        }
        if ("C".equals(position) || "PF".equals(position)) {
            base += 1;
        }
        return Math.max(0, base);
    }

    private static Shooting deriveShooting(int points, String position, Random random) {
        Shooting s = new Shooting();
        int remaining = points;
        int threeShare = "SG".equals(position) || "PG".equals(position) ? 2 : 1;
        s.tpm = Math.min(remaining / 3, threeShare + random.nextInt(3));
        remaining -= s.tpm * 3;
        s.twoPm = remaining / 2;
        remaining -= s.twoPm * 2;
        s.ftm = remaining;
        s.tpa = s.tpm + random.nextInt(4) + 2;
        s.twoPa = s.twoPm + random.nextInt(5) + 3;
        s.fta = s.ftm + random.nextInt(3);
        return s;
    }

    private static long stableSeed(String matchNo, int playerIndex) {
        return 20260724L + matchNo.hashCode() * 31L + playerIndex;
    }

    private static int val(Integer v) {
        return v == null ? 0 : v;
    }

    private static final class Shooting {
        int tpm;
        int tpa;
        int twoPm;
        int twoPa;
        int ftm;
        int fta;
    }
}
