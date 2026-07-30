package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.dao.BasketMatchDao;
import com.zhs.dao.BasketMatchPlayerStatDao;
import com.zhs.dao.BasketMatchTeamStatDao;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketMatch;
import com.zhs.model.BasketMatchPlayerStat;
import com.zhs.model.BasketMatchTeamStat;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTournamentGroup;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.model.BasketTournamentStage;
import com.zhs.model.BasketVenue;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketPublicMatchServiceImpl implements IBasketPublicMatchService {

    @Resource
    private BasketMatchDao matchDao;
    @Resource
    private BasketMatchTeamStatDao matchTeamStatDao;
    @Resource
    private BasketMatchPlayerStatDao matchPlayerStatDao;
    @Resource
    private BasketPublicSupport publicSupport;

    @Override
    public IPage<PublicMatchItemVo> pageSchedule(Long seasonId, Integer currentPage, Integer pageSize,
                                                 Long stageId, Long groupId, Long teamId) {
        publicSupport.requirePublicSeason(seasonId);
        Page<BasketMatch> page = publicSupport.newPage(currentPage, pageSize);
        LambdaQueryWrapper<BasketMatch> wrapper = basePublicMatchWrapper(seasonId, stageId, groupId, teamId)
                .orderByAsc(BasketMatch::getScheduledStartTime)
                .orderByAsc(BasketMatch::getSortNo)
                .orderByAsc(BasketMatch::getId);
        return toMatchPage(matchDao.selectPage(page, wrapper));
    }

    @Override
    public IPage<PublicMatchItemVo> pageResult(Long seasonId, Integer currentPage, Integer pageSize,
                                               Long stageId, Long groupId, Long teamId) {
        publicSupport.requirePublicSeason(seasonId);
        Page<BasketMatch> page = publicSupport.newPage(currentPage, pageSize);
        LambdaQueryWrapper<BasketMatch> wrapper = basePublicMatchWrapper(seasonId, stageId, groupId, teamId)
                .eq(BasketMatch::getMatchStatus, BasketPublicConstants.MATCH_FINISHED)
                .eq(BasketMatch::getDataStatus, BasketPublicConstants.DATA_CONFIRMED)
                .orderByDesc(BasketMatch::getActualEndTime)
                .orderByDesc(BasketMatch::getScheduledStartTime)
                .orderByDesc(BasketMatch::getId);
        return toMatchPage(matchDao.selectPage(page, wrapper));
    }

    @Override
    public PublicMatchDetailVo getMatch(Long matchId) {
        if (matchId == null) {
            throw new ServiceException("比赛主键不能为空");
        }
        BasketMatch match = matchDao.selectById(matchId);
        if (match == null
                || !Objects.equals(match.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                || !isPubliclyVisible(match)) {
            throw new ServiceException("比赛不存在或未公开");
        }
        publicSupport.requirePublicSeason(match.getSeasonId());

        List<PublicMatchItemVo> enriched = enrichMatches(Collections.singletonList(match));
        PublicMatchItemVo base = enriched.isEmpty() ? null : enriched.get(0);
        PublicMatchDetailVo vo = new PublicMatchDetailVo();
        vo.setMatchId(match.getId());
        vo.setMatchNo(match.getMatchNo());
        vo.setSeasonId(match.getSeasonId());
        vo.setStageId(match.getStageId());
        vo.setGroupId(match.getGroupId());
        vo.setHomeTeamId(match.getHomeTeamId());
        vo.setAwayTeamId(match.getAwayTeamId());
        vo.setHomeScore(match.getHomeScore());
        vo.setAwayScore(match.getAwayScore());
        vo.setWinnerTeamId(match.getWinnerTeamId());
        vo.setMatchStatus(match.getMatchStatus());
        vo.setDataStatus(match.getDataStatus());
        vo.setScheduledStartTime(match.getScheduledStartTime());
        vo.setActualStartTime(match.getActualStartTime());
        vo.setActualEndTime(match.getActualEndTime());
        if (base != null) {
            vo.setStageName(base.getStageName());
            vo.setGroupName(base.getGroupName());
            vo.setHomeTeamName(base.getHomeTeamName());
            vo.setHomeTeamLogoUrl(base.getHomeTeamLogoUrl());
            vo.setAwayTeamName(base.getAwayTeamName());
            vo.setAwayTeamLogoUrl(base.getAwayTeamLogoUrl());
            vo.setVenueName(base.getVenueName());
        }
        BasketTournamentSeason season = publicSupport.requirePublicSeason(match.getSeasonId());
        vo.setSeasonName(season.getSeasonName());

        boolean statsAvailable = Objects.equals(match.getMatchStatus(), BasketPublicConstants.MATCH_FINISHED)
                && Objects.equals(match.getDataStatus(), BasketPublicConstants.DATA_CONFIRMED);
        vo.setStatsAvailable(statsAvailable);
        if (statsAvailable) {
            vo.setTeamStats(loadTeamStats(match));
            vo.setPlayerStats(loadPlayerStats(match));
        } else {
            vo.setTeamStats(Collections.emptyList());
            vo.setPlayerStats(Collections.emptyList());
        }
        return vo;
    }

    private LambdaQueryWrapper<BasketMatch> basePublicMatchWrapper(Long seasonId, Long stageId, Long groupId, Long teamId) {
        LambdaQueryWrapper<BasketMatch> wrapper = new LambdaQueryWrapper<BasketMatch>()
                .eq(BasketMatch::getSeasonId, seasonId)
                .eq(BasketMatch::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .and(w -> w.eq(BasketMatch::getPublishStatus, BasketPublicConstants.PUBLISH_PUBLISHED)
                        .or(x -> x.eq(BasketMatch::getPublishStatus, 0).eq(BasketMatch::getPublicStatus, BasketPublicConstants.PUBLIC)))
                .eq(stageId != null, BasketMatch::getStageId, stageId)
                .eq(groupId != null, BasketMatch::getGroupId, groupId);
        if (teamId != null) {
            wrapper.and(w -> w.eq(BasketMatch::getHomeTeamId, teamId).or().eq(BasketMatch::getAwayTeamId, teamId));
        }
        return wrapper;
    }

    private IPage<PublicMatchItemVo> toMatchPage(IPage<BasketMatch> entityPage) {
        Page<PublicMatchItemVo> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(enrichMatches(entityPage.getRecords()));
        return result;
    }

    public List<PublicMatchItemVo> enrichMatches(List<BasketMatch> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return Collections.emptyList();
        }
        Set<Long> teamIds = new HashSet<>();
        Set<Long> stageIds = new HashSet<>();
        Set<Long> groupIds = new HashSet<>();
        Set<Long> venueIds = new HashSet<>();
        for (BasketMatch match : matches) {
            if (match.getHomeTeamId() != null) {
                teamIds.add(match.getHomeTeamId());
            }
            if (match.getAwayTeamId() != null) {
                teamIds.add(match.getAwayTeamId());
            }
            if (match.getStageId() != null) {
                stageIds.add(match.getStageId());
            }
            if (match.getGroupId() != null) {
                groupIds.add(match.getGroupId());
            }
            if (match.getVenueId() != null) {
                venueIds.add(match.getVenueId());
            }
        }
        Map<Long, BasketTeam> teamMap = publicSupport.loadPublicTeamMap(teamIds);
        Map<Long, String> logoMap = publicSupport.loadAccessUrlMap(teamMap.values().stream()
                .map(BasketTeam::getLogoFileId).collect(Collectors.toSet()));
        Map<Long, BasketTournamentStage> stageMap = publicSupport.loadStageMap(stageIds);
        Map<Long, BasketTournamentGroup> groupMap = publicSupport.loadGroupMap(groupIds);
        Map<Long, BasketVenue> venueMap = publicSupport.loadVenueMap(venueIds);

        List<PublicMatchItemVo> vos = new ArrayList<>(matches.size());
        for (BasketMatch match : matches) {
            PublicMatchItemVo vo = new PublicMatchItemVo();
            vo.setMatchId(match.getId());
            vo.setMatchNo(match.getMatchNo());
            vo.setSeasonId(match.getSeasonId());
            vo.setStageId(match.getStageId());
            vo.setGroupId(match.getGroupId());
            vo.setHomeTeamId(match.getHomeTeamId());
            vo.setAwayTeamId(match.getAwayTeamId());
            vo.setHomeScore(match.getHomeScore());
            vo.setAwayScore(match.getAwayScore());
            vo.setMatchStatus(match.getMatchStatus());
            vo.setDataStatus(match.getDataStatus());
            vo.setScheduledStartTime(match.getScheduledStartTime());
            vo.setActualEndTime(match.getActualEndTime());

            BasketTeam home = teamMap.get(match.getHomeTeamId());
            if (homeTeamNameFromSnapshot(match) != null) {
                vo.setHomeTeamName(homeTeamNameFromSnapshot(match));
                vo.setHomeTeamLogoUrl(homeTeamLogoFromSnapshot(match));
            } else if (home != null) {
                vo.setHomeTeamName(home.getTeamName());
                vo.setHomeTeamLogoUrl(logoMap.get(home.getLogoFileId()));
            }
            BasketTeam away = teamMap.get(match.getAwayTeamId());
            if (awayTeamNameFromSnapshot(match) != null) {
                vo.setAwayTeamName(awayTeamNameFromSnapshot(match));
                vo.setAwayTeamLogoUrl(awayTeamLogoFromSnapshot(match));
            } else if (away != null) {
                vo.setAwayTeamName(away.getTeamName());
                vo.setAwayTeamLogoUrl(logoMap.get(away.getLogoFileId()));
            }
            BasketTournamentStage stage = stageMap.get(match.getStageId());
            if (stage != null) {
                vo.setStageName(stage.getStageName());
            }
            BasketTournamentGroup group = groupMap.get(match.getGroupId());
            if (group != null) {
                vo.setGroupName(group.getGroupName());
            }
            BasketVenue venue = venueMap.get(match.getVenueId());
            if (venue != null) {
                vo.setVenueName(venue.getVenueName());
            }
            vos.add(vo);
        }
        return vos;
    }

    private boolean isPubliclyVisible(BasketMatch match) {
        if (Objects.equals(match.getPublishStatus(), BasketPublicConstants.PUBLISH_PUBLISHED)) {
            return true;
        }
        return Objects.equals(match.getPublishStatus(), 0)
                && Objects.equals(match.getPublicStatus(), BasketPublicConstants.PUBLIC);
    }

    private String homeTeamNameFromSnapshot(BasketMatch match) {
        return match.getHomeTeamNameSnapshot();
    }

    private String awayTeamNameFromSnapshot(BasketMatch match) {
        return match.getAwayTeamNameSnapshot();
    }

    private String homeTeamLogoFromSnapshot(BasketMatch match) {
        return match.getHomeTeamLogoSnapshot();
    }

    private String awayTeamLogoFromSnapshot(BasketMatch match) {
        return match.getAwayTeamLogoSnapshot();
    }

    private List<PublicMatchTeamStatVo> loadTeamStats(BasketMatch match) {
        List<BasketMatchTeamStat> stats = matchTeamStatDao.selectList(new LambdaQueryWrapper<BasketMatchTeamStat>()
                .eq(BasketMatchTeamStat::getMatchId, match.getId())
                .eq(BasketMatchTeamStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketMatchTeamStat::getDataStatus, BasketPublicConstants.DATA_CONFIRMED));
        if (CollectionUtils.isEmpty(stats)) {
            return Collections.emptyList();
        }
        Map<Long, BasketTeam> teamMap = publicSupport.loadPublicTeamMap(stats.stream()
                .map(BasketMatchTeamStat::getTeamId).collect(Collectors.toSet()));
        List<PublicMatchTeamStatVo> vos = new ArrayList<>(stats.size());
        for (BasketMatchTeamStat stat : stats) {
            PublicMatchTeamStatVo vo = new PublicMatchTeamStatVo();
            vo.setTeamId(stat.getTeamId());
            BasketTeam team = teamMap.get(stat.getTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamName());
            }
            vo.setPoints(stat.getPoints());
            vo.setRebounds(stat.getRebounds());
            vo.setAssists(stat.getAssists());
            vo.setSteals(stat.getSteals());
            vo.setBlocks(stat.getBlocks());
            vo.setTurnovers(stat.getTurnovers());
            vo.setFouls(stat.getFouls());
            vo.setFieldGoalPct(stat.getFieldGoalPct());
            vo.setThreePointPct(stat.getThreePointPct());
            vo.setFreeThrowPct(stat.getFreeThrowPct());
            vos.add(vo);
        }
        return vos;
    }

    private List<PublicMatchPlayerStatVo> loadPlayerStats(BasketMatch match) {
        List<BasketMatchPlayerStat> stats = matchPlayerStatDao.selectList(new LambdaQueryWrapper<BasketMatchPlayerStat>()
                .eq(BasketMatchPlayerStat::getMatchId, match.getId())
                .eq(BasketMatchPlayerStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketMatchPlayerStat::getDataStatus, BasketPublicConstants.DATA_CONFIRMED)
                .orderByDesc(BasketMatchPlayerStat::getPoints)
                .orderByAsc(BasketMatchPlayerStat::getId));
        if (CollectionUtils.isEmpty(stats)) {
            return Collections.emptyList();
        }
        Map<Long, BasketPlayerProfile> playerMap = publicSupport.loadPlayerMap(stats.stream()
                .map(BasketMatchPlayerStat::getPlayerId).collect(Collectors.toSet()));
        Map<Long, String> avatarMap = publicSupport.loadPlayerAvatarUrlMap(playerMap.values());
        Map<Long, BasketTeam> teamMap = publicSupport.loadPublicTeamMap(stats.stream()
                .map(BasketMatchPlayerStat::getTeamId).collect(Collectors.toSet()));
        List<PublicMatchPlayerStatVo> vos = new ArrayList<>(stats.size());
        for (BasketMatchPlayerStat stat : stats) {
            BasketPlayerProfile player = playerMap.get(stat.getPlayerId());
            if (player == null || !publicSupport.isPlayerPubliclyVisible(player)) {
                continue;
            }
            PublicMatchPlayerStatVo vo = new PublicMatchPlayerStatVo();
            vo.setPlayerId(stat.getPlayerId());
            vo.setPlayerName(publicSupport.resolveDisplayName(player));
            vo.setAvatarUrl(avatarMap.get(player.getId()));
            vo.setTeamId(stat.getTeamId());
            BasketTeam team = teamMap.get(stat.getTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamName());
            }
            vo.setStartedFlag(stat.getStartedFlag());
            vo.setMinutesSeconds(stat.getMinutesSeconds());
            vo.setPoints(stat.getPoints());
            vo.setRebounds(stat.getRebounds());
            vo.setAssists(stat.getAssists());
            vo.setSteals(stat.getSteals());
            vo.setBlocks(stat.getBlocks());
            vo.setTurnovers(stat.getTurnovers());
            vo.setFouls(stat.getFouls());
            vo.setFieldGoalPct(stat.getFieldGoalPct());
            vo.setThreePointPct(stat.getThreePointPct());
            vo.setFreeThrowPct(stat.getFreeThrowPct());
            vo.setEfficiency(stat.getEfficiency());
            vos.add(vo);
        }
        return vos;
    }
}
