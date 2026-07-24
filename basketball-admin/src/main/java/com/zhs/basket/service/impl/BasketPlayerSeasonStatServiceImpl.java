package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketPlayerSeasonStat;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.mapper.BasketConfirmedMatchAdminMapper;
import com.zhs.basket.mapper.BasketPlayerSeasonStatAdminMapper;
import com.zhs.basket.mapper.BasketStageStandingAdminMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.query.PlayerSeasonStatQuery;
import com.zhs.basket.service.IBasketPlayerSeasonStatService;
import com.zhs.basket.support.StatMathUtils;
import com.zhs.basket.vo.PlayerSeasonStatListVo;
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
public class BasketPlayerSeasonStatServiceImpl implements IBasketPlayerSeasonStatService {

    private static final long SEASON_STAGE_ID = 0L;

    @Autowired
    private BasketPlayerSeasonStatAdminMapper playerSeasonStatAdminMapper;
    @Autowired
    private BasketConfirmedMatchAdminMapper confirmedMatchAdminMapper;
    @Autowired
    private BasketStageStandingAdminMapper standingAdminMapper;
    @Autowired
    private BasketTournamentSeasonMapper seasonMapper;

    @Override
    public List<PlayerSeasonStatListVo> selectList(PlayerSeasonStatQuery query) {
        return playerSeasonStatAdminMapper.selectList(query);
    }

    @Override
    public int rebuildSeason(Long seasonId, Date calcTime) {
        requireSeason(seasonId);
        Date time = calcTime == null ? new Date() : calcTime;
        playerSeasonStatAdminMapper.softDeleteByScope(seasonId, null);

        List<BasketPlayerSeasonStat> rows = new ArrayList<>();
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
        playerSeasonStatAdminMapper.batchInsert(rows);
        return rows.size();
    }

    private List<BasketPlayerSeasonStat> buildRows(Long seasonId, Long stageId, Date calcTime) {
        Long stageFilter = SEASON_STAGE_ID == stageId ? null : stageId;
        List<Map<String, Object>> aggs = confirmedMatchAdminMapper.selectPlayerStatAgg(seasonId, stageFilter);
        List<BasketPlayerSeasonStat> rows = new ArrayList<>();
        if (CollectionUtils.isEmpty(aggs)) {
            return rows;
        }
        for (Map<String, Object> row : aggs) {
            Long playerId = toLong(mapGet(row, "playerId", "playerid"));
            Long teamId = toLong(mapGet(row, "teamId", "teamid"));
            if (playerId == null || teamId == null) {
                continue;
            }
            int games = toInt(mapGet(row, "games"));
            int starts = toInt(mapGet(row, "starts"));
            int minutesSeconds = toInt(mapGet(row, "minutesSeconds", "minutesseconds"));
            int points = toInt(mapGet(row, "points"));
            int rebounds = toInt(mapGet(row, "rebounds"));
            int assists = toInt(mapGet(row, "assists"));
            int steals = toInt(mapGet(row, "steals"));
            int blocks = toInt(mapGet(row, "blocks"));
            int turnovers = toInt(mapGet(row, "turnovers"));
            int fouls = toInt(mapGet(row, "fouls"));
            int fgm = toInt(mapGet(row, "fieldGoalsMade", "fieldgoalsmade"));
            int fga = toInt(mapGet(row, "fieldGoalsAttempted", "fieldgoalsattempted"));
            int tpm = toInt(mapGet(row, "threePointsMade", "threepointsmade"));
            int tpa = toInt(mapGet(row, "threePointsAttempted", "threepointsattempted"));
            int ftm = toInt(mapGet(row, "freeThrowsMade", "freethrowsmade"));
            int fta = toInt(mapGet(row, "freeThrowsAttempted", "freethrowsattempted"));
            BigDecimal efficiencySum = toBigDecimal(mapGet(row, "efficiencySum", "efficiencysum"));
            int doubleDoubleCount = toInt(mapGet(row, "doubleDoubleCount", "doubledoublecount"));
            int tripleDoubleCount = toInt(mapGet(row, "tripleDoubleCount", "tripledoublecount"));

            BasketPlayerSeasonStat stat = new BasketPlayerSeasonStat();
            stat.setSeasonId(seasonId);
            stat.setStageId(stageId);
            stat.setTeamId(teamId);
            stat.setPlayerId(playerId);
            stat.setGames(games);
            stat.setStarts(starts);
            stat.setMinutesSeconds(minutesSeconds);
            stat.setPoints(points);
            stat.setRebounds(rebounds);
            stat.setAssists(assists);
            stat.setSteals(steals);
            stat.setBlocks(blocks);
            stat.setTurnovers(turnovers);
            stat.setFouls(fouls);
            stat.setAvgMinutes(StatMathUtils.minutesAverage(minutesSeconds, games));
            stat.setAvgPoints(StatMathUtils.average(points, games));
            stat.setAvgRebounds(StatMathUtils.average(rebounds, games));
            stat.setAvgAssists(StatMathUtils.average(assists, games));
            stat.setAvgSteals(StatMathUtils.average(steals, games));
            stat.setAvgBlocks(StatMathUtils.average(blocks, games));
            stat.setFieldGoalPct(StatMathUtils.percentage(fgm, fga));
            stat.setThreePointPct(StatMathUtils.percentage(tpm, tpa));
            stat.setFreeThrowPct(StatMathUtils.percentage(ftm, fta));
            if (efficiencySum.compareTo(BigDecimal.ZERO) == 0 || games <= 0) {
                stat.setAvgEfficiency(BigDecimal.ZERO.setScale(StatMathUtils.AVG_SCALE, StatMathUtils.ROUNDING));
            } else {
                stat.setAvgEfficiency(StatMathUtils.average(efficiencySum, games));
            }
            stat.setDoubleDoubleCount(doubleDoubleCount);
            stat.setTripleDoubleCount(tripleDoubleCount);
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

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
