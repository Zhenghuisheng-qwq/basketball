package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTeamSeasonStat;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.mapper.BasketConfirmedMatchAdminMapper;
import com.zhs.basket.mapper.BasketStageStandingAdminMapper;
import com.zhs.basket.mapper.BasketTeamSeasonStatAdminMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.query.TeamSeasonStatQuery;
import com.zhs.basket.service.IBasketTeamSeasonStatService;
import com.zhs.basket.support.StatMathUtils;
import com.zhs.basket.vo.TeamSeasonStatListVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class BasketTeamSeasonStatServiceImpl implements IBasketTeamSeasonStatService {

    private static final long SEASON_STAGE_ID = 0L;

    @Autowired
    private BasketTeamSeasonStatAdminMapper teamSeasonStatAdminMapper;
    @Autowired
    private BasketConfirmedMatchAdminMapper confirmedMatchAdminMapper;
    @Autowired
    private BasketStageStandingAdminMapper standingAdminMapper;
    @Autowired
    private BasketTournamentSeasonMapper seasonMapper;

    @Override
    public List<TeamSeasonStatListVo> selectList(TeamSeasonStatQuery query) {
        return teamSeasonStatAdminMapper.selectList(query);
    }

    @Override
    public int rebuildSeason(Long seasonId, Date calcTime) {
        requireSeason(seasonId);
        Date time = calcTime == null ? new Date() : calcTime;
        teamSeasonStatAdminMapper.softDeleteByScope(seasonId, null);

        List<BasketTeamSeasonStat> rows = new ArrayList<>();
        rows.addAll(buildRows(seasonId, SEASON_STAGE_ID, time));
        List<Long> stageIds = standingAdminMapper.selectStageIdsBySeason(seasonId);
        if (!CollectionUtils.isEmpty(stageIds)) {
            for (Long stageId : stageIds) {
                rows.addAll(buildRows(seasonId, stageId, time));
            }
        }
        if (rows.isEmpty()) {
            return 0;
        }
        teamSeasonStatAdminMapper.batchInsert(rows);
        return rows.size();
    }

    private List<BasketTeamSeasonStat> buildRows(Long seasonId, Long stageId, Date calcTime) {
        Long stageFilter = SEASON_STAGE_ID == stageId ? null : stageId;
        List<Map<String, Object>> aggs = confirmedMatchAdminMapper.selectTeamStatAgg(seasonId, stageFilter);
        List<BasketTeamSeasonStat> rows = new ArrayList<>();
        if (CollectionUtils.isEmpty(aggs)) {
            return rows;
        }
        for (Map<String, Object> row : aggs) {
            Long teamId = toLong(mapGet(row, "teamId", "teamid"));
            if (teamId == null) {
                continue;
            }
            int games = toInt(mapGet(row, "games"));
            int wins = toInt(mapGet(row, "wins"));
            int losses = toInt(mapGet(row, "losses"));
            int pointsFor = toInt(mapGet(row, "pointsFor", "pointsfor"));
            int pointsAgainst = toInt(mapGet(row, "pointsAgainst", "pointsagainst"));
            long rebounds = toLongValue(mapGet(row, "rebounds"));
            long assists = toLongValue(mapGet(row, "assists"));
            long steals = toLongValue(mapGet(row, "steals"));
            long blocks = toLongValue(mapGet(row, "blocks"));
            long turnovers = toLongValue(mapGet(row, "turnovers"));
            int fgm = toInt(mapGet(row, "fieldGoalsMade", "fieldgoalsmade"));
            int fga = toInt(mapGet(row, "fieldGoalsAttempted", "fieldgoalsattempted"));
            int tpm = toInt(mapGet(row, "threePointsMade", "threepointsmade"));
            int tpa = toInt(mapGet(row, "threePointsAttempted", "threepointsattempted"));
            int ftm = toInt(mapGet(row, "freeThrowsMade", "freethrowsmade"));
            int fta = toInt(mapGet(row, "freeThrowsAttempted", "freethrowsattempted"));

            BasketTeamSeasonStat stat = new BasketTeamSeasonStat();
            stat.setSeasonId(seasonId);
            stat.setStageId(stageId);
            stat.setTeamId(teamId);
            stat.setGames(games);
            stat.setWins(wins);
            stat.setLosses(losses);
            stat.setPointsFor(pointsFor);
            stat.setPointsAgainst(pointsAgainst);
            stat.setAvgPoints(StatMathUtils.average(pointsFor, games));
            stat.setAvgPointsAgainst(StatMathUtils.average(pointsAgainst, games));
            stat.setAvgRebounds(StatMathUtils.average(rebounds, games));
            stat.setAvgAssists(StatMathUtils.average(assists, games));
            stat.setAvgSteals(StatMathUtils.average(steals, games));
            stat.setAvgBlocks(StatMathUtils.average(blocks, games));
            stat.setAvgTurnovers(StatMathUtils.average(turnovers, games));
            stat.setFieldGoalPct(StatMathUtils.percentage(fgm, fga));
            stat.setThreePointPct(StatMathUtils.percentage(tpm, tpa));
            stat.setFreeThrowPct(StatMathUtils.percentage(ftm, fta));
            stat.setWinRate(StatMathUtils.winRate(wins, games));
            stat.setLastCalculatedTime(calcTime);
            stat.setIsDelete(0);
            rows.add(stat);
        }
        return rows;
    }

    private BasketTournamentSeason requireSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("赛季ID不能为空");
        }
        BasketTournamentSeason season = seasonMapper.selectById(seasonId);
        if (season == null || (season.getIsDelete() != null && season.getIsDelete() != 0)) {
            throw new ServiceException("赛季不存在");
        }
        return season;
    }

    private Object mapGet(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        return new BigDecimal(value.toString()).intValue();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return new BigDecimal(value.toString()).longValue();
    }

    private long toLongValue(Object value) {
        Long v = toLong(value);
        return v == null ? 0L : v;
    }
}
