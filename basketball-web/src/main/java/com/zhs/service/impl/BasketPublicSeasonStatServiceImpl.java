package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.dao.BasketMatchDao;
import com.zhs.dao.BasketPlayerSeasonStatDao;
import com.zhs.dao.BasketStageStandingDao;
import com.zhs.dao.BasketTeamSeasonProfileDao;
import com.zhs.dao.BasketTeamSeasonStatDao;
import com.zhs.enums.PublicPlayerSortEnum;
import com.zhs.enums.PublicTeamSortEnum;
import com.zhs.model.BasketMatch;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketPlayerSeasonStat;
import com.zhs.model.BasketStageStanding;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamSeasonProfile;
import com.zhs.model.BasketTeamSeasonStat;
import com.zhs.model.BasketTournament;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.response.publicapi.PublicPlayerSeasonStatVo;
import com.zhs.response.publicapi.PublicSeasonSummaryVo;
import com.zhs.response.publicapi.PublicTeamSeasonStatVo;
import com.zhs.service.IBasketPublicSeasonStatService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BasketPublicSeasonStatServiceImpl implements IBasketPublicSeasonStatService {

    @Resource
    private BasketTeamSeasonStatDao teamSeasonStatDao;
    @Resource
    private BasketPlayerSeasonStatDao playerSeasonStatDao;
    @Resource
    private BasketTeamSeasonProfileDao teamSeasonProfileDao;
    @Resource
    private BasketMatchDao matchDao;
    @Resource
    private BasketStageStandingDao standingDao;
    @Resource
    private BasketPublicSupport publicSupport;

    @Override
    public IPage<PublicTeamSeasonStatVo> pageTeamStat(Long seasonId, Integer currentPage, Integer pageSize,
                                                      Long stageId, String sortField, String sortOrder) {
        publicSupport.requirePublicSeason(seasonId);
        PublicTeamSortEnum sortEnum = PublicTeamSortEnum.fromCode(sortField);
        boolean asc = publicSupport.isAsc(sortOrder);
        Page<BasketTeamSeasonStat> page = publicSupport.newPage(currentPage, pageSize);
        LambdaQueryWrapper<BasketTeamSeasonStat> wrapper = new LambdaQueryWrapper<BasketTeamSeasonStat>()
                .eq(BasketTeamSeasonStat::getSeasonId, seasonId)
                .eq(BasketTeamSeasonStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(stageId != null, BasketTeamSeasonStat::getStageId, stageId);
        if (asc) {
            wrapper.orderByAsc(sortEnum.getColumn()).orderByAsc(BasketTeamSeasonStat::getId);
        } else {
            wrapper.orderByDesc(sortEnum.getColumn()).orderByAsc(BasketTeamSeasonStat::getId);
        }
        IPage<BasketTeamSeasonStat> entityPage = teamSeasonStatDao.selectPage(page, wrapper);
        Page<PublicTeamSeasonStatVo> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(toTeamStatVos(entityPage.getRecords()));
        return result;
    }

    @Override
    public IPage<PublicPlayerSeasonStatVo> pagePlayerStat(Long seasonId, Integer currentPage, Integer pageSize,
                                                          Long stageId, Long teamId, String sortField, String sortOrder) {
        publicSupport.requirePublicSeason(seasonId);
        PublicPlayerSortEnum sortEnum = PublicPlayerSortEnum.fromCode(sortField);
        boolean asc = publicSupport.isAsc(sortOrder);
        Page<BasketPlayerSeasonStat> page = publicSupport.newPage(currentPage, pageSize);
        LambdaQueryWrapper<BasketPlayerSeasonStat> wrapper = new LambdaQueryWrapper<BasketPlayerSeasonStat>()
                .eq(BasketPlayerSeasonStat::getSeasonId, seasonId)
                .eq(BasketPlayerSeasonStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(stageId != null, BasketPlayerSeasonStat::getStageId, stageId)
                .eq(teamId != null, BasketPlayerSeasonStat::getTeamId, teamId);
        if (asc) {
            wrapper.orderByAsc(sortEnum.getColumn()).orderByAsc(BasketPlayerSeasonStat::getId);
        } else {
            wrapper.orderByDesc(sortEnum.getColumn()).orderByAsc(BasketPlayerSeasonStat::getId);
        }
        IPage<BasketPlayerSeasonStat> entityPage = playerSeasonStatDao.selectPage(page, wrapper);
        Page<PublicPlayerSeasonStatVo> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(toPlayerStatVos(entityPage.getRecords()));
        return result;
    }

    @Override
    public PublicSeasonSummaryVo getSummary(Long seasonId) {
        BasketTournamentSeason season = publicSupport.requirePublicSeason(seasonId);
        BasketTournament tournament = publicSupport.requirePublicTournament(season.getTournamentId());

        Long teamCount = teamSeasonProfileDao.selectCount(new LambdaQueryWrapper<BasketTeamSeasonProfile>()
                .eq(BasketTeamSeasonProfile::getSeasonId, seasonId)
                .eq(BasketTeamSeasonProfile::getIsDelete, BasketPublicConstants.NOT_DELETED));
        Long finishedMatchCount = matchDao.selectCount(new LambdaQueryWrapper<BasketMatch>()
                .eq(BasketMatch::getSeasonId, seasonId)
                .eq(BasketMatch::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketMatch::getPublicStatus, BasketPublicConstants.PUBLIC)
                .eq(BasketMatch::getMatchStatus, BasketPublicConstants.MATCH_FINISHED)
                .eq(BasketMatch::getDataStatus, BasketPublicConstants.DATA_CONFIRMED));
        Long standingCount = standingDao.selectCount(new LambdaQueryWrapper<BasketStageStanding>()
                .eq(BasketStageStanding::getSeasonId, seasonId)
                .eq(BasketStageStanding::getIsDelete, BasketPublicConstants.NOT_DELETED));
        Long teamStatCount = teamSeasonStatDao.selectCount(new LambdaQueryWrapper<BasketTeamSeasonStat>()
                .eq(BasketTeamSeasonStat::getSeasonId, seasonId)
                .eq(BasketTeamSeasonStat::getIsDelete, BasketPublicConstants.NOT_DELETED));
        Long playerStatCount = playerSeasonStatDao.selectCount(new LambdaQueryWrapper<BasketPlayerSeasonStat>()
                .eq(BasketPlayerSeasonStat::getSeasonId, seasonId)
                .eq(BasketPlayerSeasonStat::getIsDelete, BasketPublicConstants.NOT_DELETED));

        PublicSeasonSummaryVo vo = new PublicSeasonSummaryVo();
        vo.setSeasonId(season.getId());
        vo.setSeasonName(season.getSeasonName());
        vo.setTournamentId(tournament.getId());
        vo.setTournamentName(tournament.getTournamentName());
        vo.setSeasonStatus(season.getSeasonStatus());
        vo.setTeamCount(teamCount == null ? 0 : teamCount.intValue());
        vo.setFinishedMatchCount(finishedMatchCount == null ? 0 : finishedMatchCount.intValue());
        vo.setStandingCount(standingCount == null ? 0 : standingCount.intValue());
        vo.setTeamStatCount(teamStatCount == null ? 0 : teamStatCount.intValue());
        vo.setPlayerStatCount(playerStatCount == null ? 0 : playerStatCount.intValue());
        return vo;
    }

    public List<PublicTeamSeasonStatVo> toTeamStatVos(List<BasketTeamSeasonStat> stats) {
        if (CollectionUtils.isEmpty(stats)) {
            return Collections.emptyList();
        }
        Map<Long, BasketTeam> teamMap = publicSupport.loadPublicTeamMap(stats.stream()
                .map(BasketTeamSeasonStat::getTeamId).collect(Collectors.toSet()));
        Map<Long, String> logoMap = publicSupport.loadAccessUrlMap(teamMap.values().stream()
                .map(BasketTeam::getLogoFileId).collect(Collectors.toSet()));
        List<PublicTeamSeasonStatVo> vos = new ArrayList<>(stats.size());
        for (BasketTeamSeasonStat stat : stats) {
            PublicTeamSeasonStatVo vo = new PublicTeamSeasonStatVo();
            vo.setSeasonId(stat.getSeasonId());
            vo.setStageId(stat.getStageId());
            vo.setTeamId(stat.getTeamId());
            BasketTeam team = teamMap.get(stat.getTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamName());
                vo.setTeamLogoUrl(logoMap.get(team.getLogoFileId()));
            }
            vo.setGames(stat.getGames());
            vo.setWins(stat.getWins());
            vo.setLosses(stat.getLosses());
            vo.setPointsFor(stat.getPointsFor());
            vo.setPointsAgainst(stat.getPointsAgainst());
            vo.setAvgPoints(stat.getAvgPoints());
            vo.setAvgPointsAgainst(stat.getAvgPointsAgainst());
            vo.setAvgRebounds(stat.getAvgRebounds());
            vo.setAvgAssists(stat.getAvgAssists());
            vo.setAvgSteals(stat.getAvgSteals());
            vo.setAvgBlocks(stat.getAvgBlocks());
            vo.setAvgTurnovers(stat.getAvgTurnovers());
            vo.setFieldGoalPct(stat.getFieldGoalPct());
            vo.setThreePointPct(stat.getThreePointPct());
            vo.setFreeThrowPct(stat.getFreeThrowPct());
            vo.setWinRate(stat.getWinRate());
            vos.add(vo);
        }
        return vos;
    }

    public List<PublicPlayerSeasonStatVo> toPlayerStatVos(List<BasketPlayerSeasonStat> stats) {
        if (CollectionUtils.isEmpty(stats)) {
            return Collections.emptyList();
        }
        Map<Long, BasketPlayerProfile> playerMap = publicSupport.loadPlayerMap(stats.stream()
                .map(BasketPlayerSeasonStat::getPlayerId).collect(Collectors.toSet()));
        Map<Long, String> avatarMap = publicSupport.loadPlayerAvatarUrlMap(playerMap.values());
        Map<Long, BasketTeam> teamMap = publicSupport.loadPublicTeamMap(stats.stream()
                .map(BasketPlayerSeasonStat::getTeamId).collect(Collectors.toSet()));
        List<PublicPlayerSeasonStatVo> vos = new ArrayList<>();
        for (BasketPlayerSeasonStat stat : stats) {
            BasketPlayerProfile player = playerMap.get(stat.getPlayerId());
            if (player == null || !publicSupport.isPlayerPubliclyVisible(player)) {
                continue;
            }
            PublicPlayerSeasonStatVo vo = new PublicPlayerSeasonStatVo();
            vo.setSeasonId(stat.getSeasonId());
            vo.setStageId(stat.getStageId());
            vo.setTeamId(stat.getTeamId());
            BasketTeam team = teamMap.get(stat.getTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamName());
            }
            vo.setPlayerId(stat.getPlayerId());
            vo.setPlayerName(publicSupport.resolveDisplayName(player));
            vo.setAvatarUrl(com.zhs.support.MemberAvatarSupport.resolve(avatarMap.get(player.getId())));
            vo.setGames(stat.getGames());
            vo.setStarts(stat.getStarts());
            vo.setPoints(stat.getPoints());
            vo.setRebounds(stat.getRebounds());
            vo.setAssists(stat.getAssists());
            vo.setSteals(stat.getSteals());
            vo.setBlocks(stat.getBlocks());
            vo.setAvgMinutes(stat.getAvgMinutes());
            vo.setAvgPoints(stat.getAvgPoints());
            vo.setAvgRebounds(stat.getAvgRebounds());
            vo.setAvgAssists(stat.getAvgAssists());
            vo.setAvgSteals(stat.getAvgSteals());
            vo.setAvgBlocks(stat.getAvgBlocks());
            vo.setFieldGoalPct(stat.getFieldGoalPct());
            vo.setThreePointPct(stat.getThreePointPct());
            vo.setFreeThrowPct(stat.getFreeThrowPct());
            vo.setAvgEfficiency(stat.getAvgEfficiency());
            vos.add(vo);
        }
        return vos;
    }
}
