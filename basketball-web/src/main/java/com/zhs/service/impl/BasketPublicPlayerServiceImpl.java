package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.dao.BasketMatchDao;
import com.zhs.dao.BasketMatchPlayerStatDao;
import com.zhs.dao.BasketPlayerRankDao;
import com.zhs.dao.BasketPlayerSeasonStatDao;
import com.zhs.enums.PublicPlayerRankTypeEnum;
import com.zhs.model.BasketMatch;
import com.zhs.model.BasketMatchPlayerStat;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketPlayerRank;
import com.zhs.model.BasketPlayerSeasonStat;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.response.publicapi.PublicPlayerDetailVo;
import com.zhs.response.publicapi.PublicPlayerRankItemVo;
import com.zhs.response.publicapi.PublicPlayerSeasonStatVo;
import com.zhs.service.IBasketPublicPlayerService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketPublicPlayerServiceImpl implements IBasketPublicPlayerService {

    @Resource
    private BasketPlayerSeasonStatDao playerSeasonStatDao;
    @Resource
    private BasketMatchPlayerStatDao matchPlayerStatDao;
    @Resource
    private BasketMatchDao matchDao;
    @Resource
    private BasketPlayerRankDao playerRankDao;
    @Resource
    private BasketPublicSupport publicSupport;
    @Resource
    private BasketPublicMatchServiceImpl matchService;
    @Resource
    private BasketPublicSeasonStatServiceImpl seasonStatService;
    @Resource
    private BasketPublicDataCenterServiceImpl dataCenterService;

    @Override
    public PublicPlayerDetailVo getPlayer(Long playerId) {
        BasketPlayerProfile player = publicSupport.requirePlayer(playerId);
        PublicPlayerDetailVo vo = new PublicPlayerDetailVo();
        vo.setPlayerId(player.getId());
        vo.setPlayerNo(player.getPlayerNo());
        boolean visible = publicSupport.isPlayerPubliclyVisible(player);
        vo.setProfilePublic(visible);
        if (!visible) {
            vo.setDisplayName(publicSupport.resolveDisplayName(player));
            vo.setPrivacyMessage("该球员资料未公开");
            return vo;
        }
        Map<Long, String> avatarMap = publicSupport.loadPlayerAvatarUrlMap(Collections.singletonList(player));
        vo.setDisplayName(publicSupport.resolveDisplayName(player));
        vo.setAvatarUrl(avatarMap.get(player.getId()));
        vo.setPositionCode(player.getPositionCode());
        vo.setDominantHand(player.getDominantHand());
        vo.setHeightCm(player.getHeightCm());
        vo.setWeightKg(player.getWeightKg());
        vo.setCurrentTeamId(player.getCurrentTeamId());
        vo.setCertificationStatus(player.getCertificationStatus());
        vo.setPlayerStatus(player.getPlayerStatus());
        if (player.getCurrentTeamId() != null) {
            try {
                BasketTeam team = publicSupport.requirePublicTeam(player.getCurrentTeamId());
                vo.setCurrentTeamName(team.getTeamName());
            } catch (Exception ignored) {
                // 当前球队未公开时不暴露球队详情
            }
        }
        return vo;
    }

    @Override
    public List<PublicPlayerSeasonStatVo> listSeasonStat(Long playerId, Long seasonId) {
        BasketPlayerProfile player = publicSupport.requirePlayer(playerId);
        if (!publicSupport.isPlayerPubliclyVisible(player) || !publicSupport.canShowMatchData(player)) {
            return Collections.emptyList();
        }
        if (seasonId != null) {
            publicSupport.requirePublicSeason(seasonId);
        }
        LambdaQueryWrapper<BasketPlayerSeasonStat> wrapper = new LambdaQueryWrapper<BasketPlayerSeasonStat>()
                .eq(BasketPlayerSeasonStat::getPlayerId, playerId)
                .eq(BasketPlayerSeasonStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(seasonId != null, BasketPlayerSeasonStat::getSeasonId, seasonId)
                .orderByDesc(BasketPlayerSeasonStat::getSeasonId)
                .orderByAsc(BasketPlayerSeasonStat::getStageId);
        List<BasketPlayerSeasonStat> stats = playerSeasonStatDao.selectList(wrapper);
        if (CollectionUtils.isEmpty(stats)) {
            return Collections.emptyList();
        }
        Set<Long> publicSeasonIds = publicSupport.loadSeasonMap(stats.stream()
                .map(BasketPlayerSeasonStat::getSeasonId).collect(Collectors.toSet())).keySet();
        List<BasketPlayerSeasonStat> filtered = stats.stream()
                .filter(s -> publicSeasonIds.contains(s.getSeasonId()))
                .collect(Collectors.toList());
        return seasonStatService.toPlayerStatVos(filtered);
    }

    @Override
    public IPage<PublicMatchItemVo> pageMatches(Long playerId, Integer currentPage, Integer pageSize, Long seasonId) {
        BasketPlayerProfile player = publicSupport.requirePlayer(playerId);
        if (!publicSupport.isPlayerPubliclyVisible(player) || !publicSupport.canShowMatchData(player)) {
            return publicSupport.emptyPage(currentPage, pageSize);
        }
        if (seasonId != null) {
            publicSupport.requirePublicSeason(seasonId);
        }
        List<BasketMatchPlayerStat> playerStats = matchPlayerStatDao.selectList(new LambdaQueryWrapper<BasketMatchPlayerStat>()
                .eq(BasketMatchPlayerStat::getPlayerId, playerId)
                .eq(BasketMatchPlayerStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketMatchPlayerStat::getDataStatus, BasketPublicConstants.DATA_CONFIRMED)
                .eq(seasonId != null, BasketMatchPlayerStat::getSeasonId, seasonId)
                .orderByDesc(BasketMatchPlayerStat::getMatchId));
        if (CollectionUtils.isEmpty(playerStats)) {
            return publicSupport.emptyPage(currentPage, pageSize);
        }
        Set<Long> matchIds = playerStats.stream().map(BasketMatchPlayerStat::getMatchId).filter(Objects::nonNull).collect(Collectors.toSet());
        Page<BasketMatch> page = publicSupport.newPage(currentPage, pageSize);
        IPage<BasketMatch> entityPage = matchDao.selectPage(page, new LambdaQueryWrapper<BasketMatch>()
                .in(BasketMatch::getId, matchIds)
                .eq(BasketMatch::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketMatch::getPublicStatus, BasketPublicConstants.PUBLIC)
                .eq(BasketMatch::getMatchStatus, BasketPublicConstants.MATCH_FINISHED)
                .eq(BasketMatch::getDataStatus, BasketPublicConstants.DATA_CONFIRMED)
                .orderByDesc(BasketMatch::getActualEndTime)
                .orderByDesc(BasketMatch::getScheduledStartTime)
                .orderByDesc(BasketMatch::getId));
        Page<PublicMatchItemVo> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(matchService.enrichMatches(entityPage.getRecords()));
        return result;
    }

    @Override
    public List<PublicPlayerRankItemVo> listRanking(Long playerId, Long seasonId, String rankType) {
        BasketPlayerProfile player = publicSupport.requirePlayer(playerId);
        if (!publicSupport.isPlayerPubliclyVisible(player) || !publicSupport.canShowMatchData(player)) {
            return Collections.emptyList();
        }
        Long resolvedSeasonId = seasonId;
        if (resolvedSeasonId == null) {
            BasketTournamentSeason current = publicSupport.findCurrentPublicSeason();
            if (current == null) {
                return Collections.emptyList();
            }
            resolvedSeasonId = current.getId();
        } else {
            publicSupport.requirePublicSeason(resolvedSeasonId);
        }
        PublicPlayerRankTypeEnum typeEnum = PublicPlayerRankTypeEnum.fromCode(
                rankType == null || rankType.isBlank() ? PublicPlayerRankTypeEnum.AVG_POINTS.getCode() : rankType);
        Date snapshotDate = latestSnapshot(resolvedSeasonId, typeEnum.getCode());
        if (snapshotDate == null) {
            return Collections.emptyList();
        }
        List<BasketPlayerRank> ranks = playerRankDao.selectList(new LambdaQueryWrapper<BasketPlayerRank>()
                .eq(BasketPlayerRank::getSeasonId, resolvedSeasonId)
                .eq(BasketPlayerRank::getRankType, typeEnum.getCode())
                .eq(BasketPlayerRank::getSnapshotDate, snapshotDate)
                .eq(BasketPlayerRank::getPlayerId, playerId)
                .eq(BasketPlayerRank::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketPlayerRank::getRankNo));
        return dataCenterService.toPlayerRankVos(ranks, typeEnum);
    }

    private Date latestSnapshot(Long seasonId, String rankType) {
        BasketPlayerRank latest = playerRankDao.selectOne(new LambdaQueryWrapper<BasketPlayerRank>()
                .eq(BasketPlayerRank::getSeasonId, seasonId)
                .eq(BasketPlayerRank::getRankType, rankType)
                .eq(BasketPlayerRank::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByDesc(BasketPlayerRank::getSnapshotDate)
                .last("limit 1"));
        return latest == null ? null : latest.getSnapshotDate();
    }
}
