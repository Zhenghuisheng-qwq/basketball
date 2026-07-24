package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketPlayerRank;
import com.zhs.basket.domain.BasketTeamRank;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.enums.PlayerRankTypeEnum;
import com.zhs.basket.enums.TeamRankTypeEnum;
import com.zhs.basket.mapper.BasketPlayerRankAdminMapper;
import com.zhs.basket.mapper.BasketPlayerSeasonStatAdminMapper;
import com.zhs.basket.mapper.BasketTeamRankAdminMapper;
import com.zhs.basket.mapper.BasketTeamSeasonStatAdminMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.query.RankingQuery;
import com.zhs.basket.service.IBasketRankingService;
import com.zhs.basket.vo.PlayerRankListVo;
import com.zhs.basket.vo.PlayerSeasonStatListVo;
import com.zhs.basket.vo.TeamRankListVo;
import com.zhs.basket.vo.TeamSeasonStatListVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class BasketRankingServiceImpl implements IBasketRankingService {

    private static final long SEASON_STAGE_ID = 0L;

    @Autowired
    private BasketTeamRankAdminMapper teamRankAdminMapper;
    @Autowired
    private BasketPlayerRankAdminMapper playerRankAdminMapper;
    @Autowired
    private BasketTeamSeasonStatAdminMapper teamSeasonStatAdminMapper;
    @Autowired
    private BasketPlayerSeasonStatAdminMapper playerSeasonStatAdminMapper;
    @Autowired
    private BasketTournamentSeasonMapper seasonMapper;

    @Override
    public List<TeamRankListVo> selectTeamLatest(RankingQuery query) {
        List<TeamRankListVo> list = teamRankAdminMapper.selectLatestList(query);
        if (!CollectionUtils.isEmpty(list)) {
            for (TeamRankListVo vo : list) {
                if (vo.getRankType() != null) {
                    try {
                        vo.setRankTypeName(TeamRankTypeEnum.fromCode(vo.getRankType()).getDescription());
                    } catch (ServiceException ignored) {
                        // keep null name for unknown types
                    }
                }
            }
        }
        return list;
    }

    @Override
    public List<PlayerRankListVo> selectPlayerLatest(RankingQuery query) {
        List<PlayerRankListVo> list = playerRankAdminMapper.selectLatestList(query);
        if (!CollectionUtils.isEmpty(list)) {
            for (PlayerRankListVo vo : list) {
                if (vo.getRankType() != null) {
                    try {
                        vo.setRankTypeName(PlayerRankTypeEnum.fromCode(vo.getRankType()).getDescription());
                    } catch (ServiceException ignored) {
                        // keep null name for unknown types
                    }
                }
            }
        }
        return list;
    }

    @Override
    public int rebuildSeasonRankings(Long seasonId, Date snapshotDate) {
        requireSeason(seasonId);
        Date snap = normalizeSnapshotDate(snapshotDate == null ? new Date() : snapshotDate);
        teamRankAdminMapper.softDeleteByScope(seasonId, SEASON_STAGE_ID, snap);
        playerRankAdminMapper.softDeleteByScope(seasonId, SEASON_STAGE_ID, snap);

        List<TeamSeasonStatListVo> teams = teamSeasonStatAdminMapper.selectForRanking(seasonId, SEASON_STAGE_ID);
        List<PlayerSeasonStatListVo> players = playerSeasonStatAdminMapper.selectForRanking(seasonId, SEASON_STAGE_ID);

        List<BasketTeamRank> teamRanks = new ArrayList<>();
        for (TeamRankTypeEnum type : TeamRankTypeEnum.values()) {
            teamRanks.addAll(buildTeamRanks(seasonId, type, teams, snap));
        }
        List<BasketPlayerRank> playerRanks = new ArrayList<>();
        for (PlayerRankTypeEnum type : PlayerRankTypeEnum.values()) {
            playerRanks.addAll(buildPlayerRanks(seasonId, type, players, snap));
        }

        int count = 0;
        if (!teamRanks.isEmpty()) {
            teamRankAdminMapper.batchInsert(teamRanks);
            count += teamRanks.size();
        }
        if (!playerRanks.isEmpty()) {
            playerRankAdminMapper.batchInsert(playerRanks);
            count += playerRanks.size();
        }
        return count;
    }

    private List<BasketTeamRank> buildTeamRanks(Long seasonId, TeamRankTypeEnum type,
                                                List<TeamSeasonStatListVo> teams, Date snap) {
        List<BasketTeamRank> rows = new ArrayList<>();
        if (CollectionUtils.isEmpty(teams)) {
            return rows;
        }
        boolean ascending = type == TeamRankTypeEnum.AVG_POINTS_AGAINST;
        List<TeamSeasonStatListVo> sorted = teams.stream()
                .filter(t -> t.getGames() != null && t.getGames() > 0)
                .sorted(buildTeamComparator(type, ascending))
                .toList();
        int rank = 1;
        for (TeamSeasonStatListVo team : sorted) {
            BigDecimal value = teamStatValue(team, type);
            if (value == null) {
                continue;
            }
            BasketTeamRank row = new BasketTeamRank();
            row.setSeasonId(seasonId);
            row.setStageId(SEASON_STAGE_ID);
            row.setRankType(type.getCode());
            row.setTeamId(team.getTeamId());
            row.setRankNo(rank);
            row.setPreviousRank(teamRankAdminMapper.selectPreviousRank(
                    seasonId, SEASON_STAGE_ID, type.getCode(), team.getTeamId(), snap));
            row.setStatValue(value);
            row.setSnapshotDate(snap);
            row.setIsDelete(0);
            rows.add(row);
            rank++;
        }
        return rows;
    }

    private List<BasketPlayerRank> buildPlayerRanks(Long seasonId, PlayerRankTypeEnum type,
                                                    List<PlayerSeasonStatListVo> players, Date snap) {
        List<BasketPlayerRank> rows = new ArrayList<>();
        if (CollectionUtils.isEmpty(players)) {
            return rows;
        }
        List<PlayerSeasonStatListVo> sorted = players.stream()
                .filter(p -> p.getGames() != null && p.getGames() > 0)
                .sorted(buildPlayerComparator(type))
                .toList();
        int rank = 1;
        for (PlayerSeasonStatListVo player : sorted) {
            BigDecimal value = playerStatValue(player, type);
            if (value == null) {
                continue;
            }
            BasketPlayerRank row = new BasketPlayerRank();
            row.setSeasonId(seasonId);
            row.setStageId(SEASON_STAGE_ID);
            row.setRankType(type.getCode());
            row.setPlayerId(player.getPlayerId());
            row.setTeamId(player.getTeamId());
            row.setRankNo(rank);
            row.setPreviousRank(playerRankAdminMapper.selectPreviousRank(
                    seasonId, SEASON_STAGE_ID, type.getCode(), player.getPlayerId(), snap));
            row.setStatValue(value);
            row.setSnapshotDate(snap);
            row.setIsDelete(0);
            rows.add(row);
            rank++;
        }
        return rows;
    }

    private Comparator<TeamSeasonStatListVo> buildTeamComparator(TeamRankTypeEnum type, boolean ascending) {
        Function<TeamSeasonStatListVo, BigDecimal> getter = t -> teamStatValue(t, type);
        Comparator<TeamSeasonStatListVo> byValue = Comparator.comparing(
                getter, Comparator.nullsLast(BigDecimal::compareTo));
        if (!ascending) {
            byValue = byValue.reversed();
        }
        return byValue.thenComparing(t -> t.getTeamId() == null ? 0L : t.getTeamId());
    }

    private Comparator<PlayerSeasonStatListVo> buildPlayerComparator(PlayerRankTypeEnum type) {
        Function<PlayerSeasonStatListVo, BigDecimal> getter = p -> playerStatValue(p, type);
        return Comparator.comparing(getter, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
                .thenComparing(p -> p.getPlayerId() == null ? 0L : p.getPlayerId());
    }

    private BigDecimal teamStatValue(TeamSeasonStatListVo t, TeamRankTypeEnum type) {
        return switch (type) {
            case WIN_RATE -> t.getWinRate();
            case AVG_POINTS -> t.getAvgPoints();
            case AVG_POINTS_AGAINST -> t.getAvgPointsAgainst();
            case AVG_REBOUNDS -> t.getAvgRebounds();
            case AVG_ASSISTS -> t.getAvgAssists();
            case AVG_STEALS -> t.getAvgSteals();
            case AVG_BLOCKS -> t.getAvgBlocks();
            case FIELD_GOAL_PCT -> t.getFieldGoalPct();
            case THREE_POINT_PCT -> t.getThreePointPct();
            case FREE_THROW_PCT -> t.getFreeThrowPct();
        };
    }

    private BigDecimal playerStatValue(PlayerSeasonStatListVo p, PlayerRankTypeEnum type) {
        return switch (type) {
            case AVG_POINTS -> p.getAvgPoints();
            case AVG_REBOUNDS -> p.getAvgRebounds();
            case AVG_ASSISTS -> p.getAvgAssists();
            case AVG_STEALS -> p.getAvgSteals();
            case AVG_BLOCKS -> p.getAvgBlocks();
            case FIELD_GOAL_PCT -> p.getFieldGoalPct();
            case THREE_POINT_PCT -> p.getThreePointPct();
            case FREE_THROW_PCT -> p.getFreeThrowPct();
            case AVG_EFFICIENCY -> p.getAvgEfficiency();
        };
    }

    private Date normalizeSnapshotDate(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
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
}
