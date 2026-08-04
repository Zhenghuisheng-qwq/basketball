package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.dao.v4.BasketEventPlayerDao;
import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.dao.v4.BasketSeasonDao;
import com.zhs.dao.v4.BasketV4MatchDao;
import com.zhs.dao.v4.BasketV4MatchPlayerStatDao;
import com.zhs.dao.v4.BasketV4MatchTeamStatDao;
import com.zhs.enums.v4.MatchPublishStatusEnum;
import com.zhs.enums.v4.MatchStatusEnum;
import com.zhs.enums.v4.PublishStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.v4.BasketEventPlayer;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketSeason;
import com.zhs.model.v4.BasketV4Match;
import com.zhs.model.v4.BasketV4MatchPlayerStat;
import com.zhs.model.v4.BasketV4MatchTeamStat;
import com.zhs.response.publicapi.PublicMatchDetailVo;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.response.publicapi.PublicMatchPlayerStatVo;
import com.zhs.response.publicapi.PublicMatchTeamStatVo;
import com.zhs.service.IBasketPublicMatchService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketPublicMatchServiceImpl implements IBasketPublicMatchService {

    @Resource
    private BasketV4MatchDao matchDao;
    @Resource
    private BasketEventTeamDao eventTeamDao;
    @Resource
    private BasketSeasonDao seasonDao;
    @Resource
    private BasketV4MatchTeamStatDao matchTeamStatDao;
    @Resource
    private BasketV4MatchPlayerStatDao matchPlayerStatDao;
    @Resource
    private BasketPublicSupport publicSupport;

    @Override
    public IPage<PublicMatchItemVo> pageSchedule(Long seasonId, Integer currentPage, Integer pageSize,
                                                 Long stageId, Long groupId, Long teamId) {
        requirePublishedV4Season(seasonId);
        Page<BasketV4Match> page = publicSupport.newPage(currentPage, pageSize);
        LambdaQueryWrapper<BasketV4Match> wrapper = basePublicMatchWrapper(seasonId, groupId, teamId)
                .in(BasketV4Match::getMatchStatus,
                        MatchStatusEnum.SCHEDULED.getCode(), MatchStatusEnum.PLAYING.getCode())
                .orderByAsc(BasketV4Match::getMatchTime)
                .orderByAsc(BasketV4Match::getRoundNo)
                .orderByAsc(BasketV4Match::getMatchNo)
                .orderByAsc(BasketV4Match::getId);
        return toMatchPage(matchDao.selectPage(page, wrapper));
    }

    @Override
    public IPage<PublicMatchItemVo> pageResult(Long seasonId, Integer currentPage, Integer pageSize,
                                               Long stageId, Long groupId, Long teamId) {
        requirePublishedV4Season(seasonId);
        Page<BasketV4Match> page = publicSupport.newPage(currentPage, pageSize);
        LambdaQueryWrapper<BasketV4Match> wrapper = basePublicMatchWrapper(seasonId, groupId, teamId)
                .eq(BasketV4Match::getMatchStatus, MatchStatusEnum.FINISHED.getCode())
                .orderByDesc(BasketV4Match::getMatchTime)
                .orderByDesc(BasketV4Match::getRoundNo)
                .orderByDesc(BasketV4Match::getMatchNo)
                .orderByDesc(BasketV4Match::getId);
        return toMatchPage(matchDao.selectPage(page, wrapper));
    }

    @Override
    public PublicMatchDetailVo getMatch(Long matchId) {
        if (matchId == null) {
            throw new ServiceException("比赛主键不能为空");
        }
        BasketV4Match match = matchDao.selectById(matchId);
        if (match == null
                || !Objects.equals(match.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                || !isPubliclyVisible(match)) {
            throw new ServiceException("比赛不存在或未公开");
        }
        BasketSeason season = requirePublishedV4Season(match.getSeasonId());

        List<PublicMatchItemVo> enriched = enrichMatches(Collections.singletonList(match));
        PublicMatchItemVo base = enriched.isEmpty() ? null : enriched.get(0);
        PublicMatchDetailVo vo = new PublicMatchDetailVo();
        vo.setMatchId(match.getId());
        vo.setMatchNo(match.getMatchNo());
        vo.setSeasonId(match.getSeasonId());
        vo.setHomeTeamId(match.getHomeEventTeamId());
        vo.setAwayTeamId(match.getAwayEventTeamId());
        vo.setHomeScore(match.getHomeScore());
        vo.setAwayScore(match.getAwayScore());
        vo.setWinnerTeamId(match.getWinnerEventTeamId());
        vo.setScheduledStartTime(match.getMatchTime());
        if (MatchStatusEnum.FINISHED.getCode().equals(match.getMatchStatus())) {
            vo.setActualEndTime(match.getMatchTime());
        }
        if (base != null) {
            vo.setHomeTeamName(base.getHomeTeamName());
            vo.setHomeTeamLogoUrl(base.getHomeTeamLogoUrl());
            vo.setAwayTeamName(base.getAwayTeamName());
            vo.setAwayTeamLogoUrl(base.getAwayTeamLogoUrl());
            vo.setVenueName(base.getVenueName());
        }
        vo.setSeasonName(resolveSeasonName(season));

        boolean statsAvailable = MatchStatusEnum.FINISHED.getCode().equals(match.getMatchStatus());
        vo.setStatsAvailable(statsAvailable);
        if (statsAvailable) {
            Map<Long, BasketEventTeam> teamMap = loadEventTeamMap(
                    List.of(match.getHomeEventTeamId(), match.getAwayEventTeamId()));
            vo.setTeamStats(loadTeamStats(match.getId(), teamMap));
            vo.setPlayerStats(loadPlayerStats(match.getId(), teamMap));
        } else {
            vo.setTeamStats(Collections.emptyList());
            vo.setPlayerStats(Collections.emptyList());
        }
        return vo;
    }

    private LambdaQueryWrapper<BasketV4Match> basePublicMatchWrapper(Long seasonId, Long divisionId, Long teamId) {
        LambdaQueryWrapper<BasketV4Match> wrapper = new LambdaQueryWrapper<BasketV4Match>()
                .eq(BasketV4Match::getSeasonId, seasonId)
                .eq(BasketV4Match::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketV4Match::getPublishStatus, MatchPublishStatusEnum.PUBLISHED.getCode());
        if (divisionId != null) {
            wrapper.eq(BasketV4Match::getDivisionId, divisionId);
        }
        if (teamId != null) {
            Set<Long> eventTeamIds = findEventTeamIdsBySourceTeam(seasonId, teamId);
            if (eventTeamIds.isEmpty()) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.and(w -> w.in(BasketV4Match::getHomeEventTeamId, eventTeamIds)
                        .or().in(BasketV4Match::getAwayEventTeamId, eventTeamIds));
            }
        }
        return wrapper;
    }

    private IPage<PublicMatchItemVo> toMatchPage(IPage<BasketV4Match> entityPage) {
        Page<PublicMatchItemVo> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(enrichMatches(entityPage.getRecords()));
        return result;
    }

    public List<PublicMatchItemVo> enrichMatches(List<BasketV4Match> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return Collections.emptyList();
        }
        Set<Long> eventTeamIds = new HashSet<>();
        for (BasketV4Match match : matches) {
            if (match.getHomeEventTeamId() != null) {
                eventTeamIds.add(match.getHomeEventTeamId());
            }
            if (match.getAwayEventTeamId() != null) {
                eventTeamIds.add(match.getAwayEventTeamId());
            }
        }
        Map<Long, BasketEventTeam> teamMap = loadEventTeamMap(eventTeamIds);
        Map<Long, String> logoMap = publicSupport.loadAccessUrlMap(teamMap.values().stream()
                .map(BasketEventTeam::getTeamLogoFileId).filter(Objects::nonNull).collect(Collectors.toSet()));

        List<PublicMatchItemVo> vos = new ArrayList<>(matches.size());
        for (BasketV4Match match : matches) {
            PublicMatchItemVo vo = new PublicMatchItemVo();
            vo.setMatchId(match.getId());
            vo.setMatchNo(match.getMatchNo());
            vo.setSeasonId(match.getSeasonId());
            vo.setHomeTeamId(match.getHomeEventTeamId());
            vo.setAwayTeamId(match.getAwayEventTeamId());
            vo.setHomeScore(match.getHomeScore());
            vo.setAwayScore(match.getAwayScore());
            vo.setScheduledStartTime(match.getMatchTime());
            if (MatchStatusEnum.FINISHED.getCode().equals(match.getMatchStatus())) {
                vo.setActualEndTime(match.getMatchTime());
            }
            vo.setVenueName(match.getVenueText());

            BasketEventTeam home = teamMap.get(match.getHomeEventTeamId());
            if (home != null) {
                vo.setHomeTeamName(home.getTeamNameSnapshot());
                vo.setHomeTeamLogoUrl(logoMap.get(home.getTeamLogoFileId()));
            }
            BasketEventTeam away = teamMap.get(match.getAwayEventTeamId());
            if (away != null) {
                vo.setAwayTeamName(away.getTeamNameSnapshot());
                vo.setAwayTeamLogoUrl(logoMap.get(away.getTeamLogoFileId()));
            }
            vos.add(vo);
        }
        return vos;
    }

    private boolean isPubliclyVisible(BasketV4Match match) {
        return MatchPublishStatusEnum.PUBLISHED.getCode().equals(match.getPublishStatus());
    }

    private BasketSeason requirePublishedV4Season(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("赛季主键不能为空");
        }
        BasketSeason season = seasonDao.selectById(seasonId);
        if (season == null
                || !Objects.equals(season.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                || !PublishStatusEnum.PUBLISHED.getCode().equals(season.getPublishStatus())) {
            throw new ServiceException("赛季不存在或未公开");
        }
        return season;
    }

    private String resolveSeasonName(BasketSeason season) {
        if (season == null) {
            return null;
        }
        if (season.getDisplayTitle() != null && !season.getDisplayTitle().isBlank()) {
            return season.getDisplayTitle();
        }
        return season.getSeasonName();
    }

    private Set<Long> findEventTeamIdsBySourceTeam(Long seasonId, Long sourceTeamId) {
        List<BasketEventTeam> teams = eventTeamDao.selectList(new LambdaQueryWrapper<BasketEventTeam>()
                .eq(BasketEventTeam::getSeasonId, seasonId)
                .eq(BasketEventTeam::getSourceTeamId, sourceTeamId)
                .eq(BasketEventTeam::getIsDelete, BasketPublicConstants.NOT_DELETED));
        if (CollectionUtils.isEmpty(teams)) {
            return Collections.emptySet();
        }
        return teams.stream().map(BasketEventTeam::getId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Map<Long, BasketEventTeam> loadEventTeamMap(Collection<Long> teamIds) {
        if (CollectionUtils.isEmpty(teamIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = teamIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketEventTeam> list = eventTeamDao.selectBatchIds(ids);
        Map<Long, BasketEventTeam> map = new HashMap<>();
        for (BasketEventTeam item : list) {
            if (item != null && Objects.equals(item.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    private List<PublicMatchTeamStatVo> loadTeamStats(Long matchId, Map<Long, BasketEventTeam> teamMap) {
        List<BasketV4MatchTeamStat> stats = matchTeamStatDao.selectList(new LambdaQueryWrapper<BasketV4MatchTeamStat>()
                .eq(BasketV4MatchTeamStat::getMatchId, matchId)
                .eq(BasketV4MatchTeamStat::getIsDelete, BasketPublicConstants.NOT_DELETED));
        if (CollectionUtils.isEmpty(stats)) {
            return Collections.emptyList();
        }
        List<PublicMatchTeamStatVo> vos = new ArrayList<>(stats.size());
        for (BasketV4MatchTeamStat stat : stats) {
            PublicMatchTeamStatVo vo = new PublicMatchTeamStatVo();
            vo.setTeamId(stat.getEventTeamId());
            BasketEventTeam team = teamMap.get(stat.getEventTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamNameSnapshot());
            }
            vo.setPoints(stat.getPointsFor());
            vo.setRebounds(stat.getRebound());
            vo.setAssists(stat.getAssist());
            vo.setSteals(stat.getSteal());
            vo.setBlocks(stat.getBlock());
            vo.setTurnovers(stat.getTurnover());
            vos.add(vo);
        }
        return vos;
    }

    private List<PublicMatchPlayerStatVo> loadPlayerStats(Long matchId, Map<Long, BasketEventTeam> teamMap) {
        List<BasketV4MatchPlayerStat> stats = matchPlayerStatDao.selectList(new LambdaQueryWrapper<BasketV4MatchPlayerStat>()
                .eq(BasketV4MatchPlayerStat::getMatchId, matchId)
                .eq(BasketV4MatchPlayerStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByDesc(BasketV4MatchPlayerStat::getPoints)
                .orderByAsc(BasketV4MatchPlayerStat::getId));
        if (CollectionUtils.isEmpty(stats)) {
            return Collections.emptyList();
        }
        Map<Long, BasketEventPlayer> playerMap = loadEventPlayerMap(
                stats.stream().map(BasketV4MatchPlayerStat::getEventPlayerId).collect(Collectors.toSet()));
        List<PublicMatchPlayerStatVo> vos = new ArrayList<>(stats.size());
        for (BasketV4MatchPlayerStat stat : stats) {
            BasketEventPlayer player = playerMap.get(stat.getEventPlayerId());
            if (player == null) {
                continue;
            }
            PublicMatchPlayerStatVo vo = new PublicMatchPlayerStatVo();
            vo.setPlayerId(stat.getEventPlayerId());
            vo.setPlayerName(player.getPlayerNameSnapshot());
            vo.setTeamId(stat.getEventTeamId());
            BasketEventTeam team = teamMap.get(stat.getEventTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamNameSnapshot());
            }
            vo.setPoints(stat.getPoints());
            vo.setRebounds(stat.getRebound());
            vo.setAssists(stat.getAssist());
            vo.setSteals(stat.getSteal());
            vo.setBlocks(stat.getBlock());
            vo.setTurnovers(stat.getTurnover());
            vo.setFouls(stat.getFoul());
            if (stat.getMinutes() != null) {
                vo.setMinutesSeconds(stat.getMinutes().multiply(java.math.BigDecimal.valueOf(60)).intValue());
            }
            vos.add(vo);
        }
        return vos;
    }

    private Map<Long, BasketEventPlayer> loadEventPlayerMap(Collection<Long> playerIds) {
        if (CollectionUtils.isEmpty(playerIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = playerIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketEventPlayer> list = eventPlayerDao.selectBatchIds(ids);
        Map<Long, BasketEventPlayer> map = new HashMap<>();
        for (BasketEventPlayer item : list) {
            if (item != null && Objects.equals(item.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    @Resource
    private BasketEventPlayerDao eventPlayerDao;
}
