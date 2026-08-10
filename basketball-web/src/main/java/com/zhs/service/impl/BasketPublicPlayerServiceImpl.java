package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.dao.BasketPlayerRankDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.dao.v4.BasketEventPlayerDao;
import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.dao.v4.BasketPlayerStatDao;
import com.zhs.dao.v4.BasketSeasonDao;
import com.zhs.dao.v4.BasketSeriesDao;
import com.zhs.dao.v4.BasketV4MatchDao;
import com.zhs.dao.v4.BasketV4MatchPlayerStatDao;
import com.zhs.enums.PublicPlayerRankTypeEnum;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.enums.v4.DivisionStatusEnum;
import com.zhs.enums.v4.MatchPublishStatusEnum;
import com.zhs.enums.v4.MatchStatusEnum;
import com.zhs.enums.v4.PublishStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketPlayerRank;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.v4.BasketDivision;
import com.zhs.model.v4.BasketEventPlayer;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketPlayerStat;
import com.zhs.model.v4.BasketSeason;
import com.zhs.model.v4.BasketSeries;
import com.zhs.model.v4.BasketV4Match;
import com.zhs.model.v4.BasketV4MatchPlayerStat;
import com.zhs.response.publicapi.PublicHonorItemResp;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.response.publicapi.PublicPlayerCurrentTeamVo;
import com.zhs.response.publicapi.PublicPlayerDetailVo;
import com.zhs.response.publicapi.PublicPlayerRankItemVo;
import com.zhs.response.publicapi.PublicPlayerSeasonStatVo;
import com.zhs.service.IBasketPublicPlayerService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicHonorSupport;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketPublicPlayerServiceImpl implements IBasketPublicPlayerService {

    @Resource
    private BasketV4MatchPlayerStatDao matchPlayerStatDao;
    @Resource
    private BasketV4MatchDao matchDao;
    @Resource
    private BasketEventPlayerDao eventPlayerDao;
    @Resource
    private BasketEventTeamDao eventTeamDao;
    @Resource
    private BasketPlayerStatDao playerStatDao;
    @Resource
    private BasketSeasonDao seasonDao;
    @Resource
    private BasketPlayerRankDao playerRankDao;
    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketDivisionDao divisionDao;
    @Resource
    private BasketSeriesDao seriesDao;
    @Resource
    private BasketPublicSupport publicSupport;
    @Resource
    private BasketPublicHonorSupport honorSupport;
    @Resource
    private BasketPublicMatchServiceImpl matchService;
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
        vo.setAvatarUrl(com.zhs.support.MemberAvatarSupport.resolve(avatarMap.get(player.getId())));
        vo.setPositionCode(player.getPositionCode());
        vo.setDominantHand(player.getDominantHand());
        vo.setHeightCm(player.getHeightCm());
        vo.setWeightKg(player.getWeightKg());
        vo.setCertificationStatus(player.getCertificationStatus());
        vo.setPlayerStatus(player.getPlayerStatus());
        List<PublicPlayerCurrentTeamVo> currentTeams = loadCurrentTeams(player);
        vo.setCurrentTeams(currentTeams);
        if (!currentTeams.isEmpty()) {
            PublicPlayerCurrentTeamVo primary = currentTeams.get(0);
            vo.setCurrentTeamId(primary.getTeamId());
            vo.setCurrentTeamName(primary.getTeamName());
            vo.setCurrentTeamLogoUrl(primary.getLogoUrl());
        } else if (player.getCurrentTeamId() != null) {
            // 兼容遗留 current_team_id：仅作降级展示，不代表唯一所属
            vo.setCurrentTeamId(player.getCurrentTeamId());
            try {
                BasketTeam team = publicSupport.requirePublicTeam(player.getCurrentTeamId());
                vo.setCurrentTeamName(team.getTeamName());
                if (team.getLogoFileId() != null) {
                    Map<Long, String> logoMap = publicSupport.loadAccessUrlMap(Collections.singleton(team.getLogoFileId()));
                    vo.setCurrentTeamLogoUrl(logoMap.get(team.getLogoFileId()));
                }
            } catch (Exception ignored) {
                // 当前球队未公开时不暴露球队详情
            }
        }
        return vo;
    }

    private List<PublicPlayerCurrentTeamVo> loadCurrentTeams(BasketPlayerProfile player) {
        if (player == null || player.getId() == null) {
            return Collections.emptyList();
        }
        List<BasketTeamMember> memberships = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getPlayerId, player.getId())
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsCurrent, 1)
                .eq(BasketTeamMember::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByDesc(BasketTeamMember::getJoinTime)
                .orderByDesc(BasketTeamMember::getId));
        if (CollectionUtils.isEmpty(memberships)) {
            return Collections.emptyList();
        }
        Set<Long> teamIds = memberships.stream().map(BasketTeamMember::getTeamId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketTeam> teamMap = new HashMap<>();
        if (!teamIds.isEmpty()) {
            for (BasketTeam team : teamDao.selectByIds(teamIds)) {
                if (team != null && Objects.equals(team.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                    teamMap.put(team.getId(), team);
                }
            }
        }
        Set<Long> divisionIds = teamMap.values().stream().map(BasketTeam::getDivisionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketDivision> divisionMap = new HashMap<>();
        if (!divisionIds.isEmpty()) {
            for (BasketDivision division : divisionDao.selectByIds(divisionIds)) {
                if (division != null && Objects.equals(division.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                    divisionMap.put(division.getId(), division);
                }
            }
        }
        Set<Long> seasonIds = divisionMap.values().stream().map(BasketDivision::getSeasonId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketSeason> seasonMap = publicSupport.loadSeasonMap(seasonIds);
        Set<Long> seriesIds = seasonMap.values().stream().map(BasketSeason::getSeriesId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketSeries> seriesMap = publicSupport.loadSeriesMap(seriesIds);
        Set<Long> logoIds = teamMap.values().stream().map(BasketTeam::getLogoFileId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> logoMap = publicSupport.loadAccessUrlMap(logoIds);

        List<PublicPlayerCurrentTeamVo> rows = new ArrayList<>();
        for (BasketTeamMember membership : memberships) {
            BasketTeam team = teamMap.get(membership.getTeamId());
            if (team == null || !Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
                continue;
            }
            if (team.getDivisionId() == null) {
                continue;
            }
            BasketDivision division = divisionMap.get(team.getDivisionId());
            if (division == null
                    || DivisionStatusEnum.FINISHED.getCode().equals(division.getDivisionStatus())
                    || DivisionStatusEnum.ARCHIVED.getCode().equals(division.getDivisionStatus())) {
                continue;
            }
            PublicPlayerCurrentTeamVo item = new PublicPlayerCurrentTeamVo();
            item.setTeamId(team.getId());
            item.setTeamName(team.getTeamName());
            item.setShortName(team.getShortName());
            item.setLogoUrl(logoMap.get(team.getLogoFileId()));
            item.setDivisionId(division.getId());
            item.setDivisionName(division.getDivisionName());
            item.setPlatformCode(division.getPlatformCode());
            item.setGameMode(division.getGameMode());
            item.setMyRole(membership.getRoleType());
            BasketSeason season = seasonMap.get(division.getSeasonId());
            if (season != null) {
                item.setSeasonId(season.getId());
                item.setSeasonName(season.getDisplayTitle() != null ? season.getDisplayTitle() : season.getSeasonName());
                BasketSeries series = seriesMap.get(season.getSeriesId());
                if (series != null) {
                    item.setSeriesName(series.getSeriesName());
                }
            }
            rows.add(item);
        }
        return rows;
    }

    @Override
    public List<PublicPlayerSeasonStatVo> listSeasonStat(Long playerId, Long seasonId) {
        BasketPlayerProfile player = publicSupport.requirePlayer(playerId);
        if (!publicSupport.isPlayerPubliclyVisible(player) || !publicSupport.canShowMatchData(player)) {
            return Collections.emptyList();
        }
        if (seasonId != null) {
            requirePublishedV4Season(seasonId);
        }

        // sourcePlayerId → EventPlayer → PlayerStat（按 division 一行，含历史 FINISHED）
        List<BasketEventPlayer> eventPlayers = eventPlayerDao.selectList(new LambdaQueryWrapper<BasketEventPlayer>()
                .eq(BasketEventPlayer::getSourcePlayerId, playerId)
                .eq(BasketEventPlayer::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(seasonId != null, BasketEventPlayer::getSeasonId, seasonId));
        if (CollectionUtils.isEmpty(eventPlayers)) {
            return Collections.emptyList();
        }

        Set<Long> eventPlayerIds = eventPlayers.stream()
                .map(BasketEventPlayer::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<BasketPlayerStat> stats = playerStatDao.selectList(new LambdaQueryWrapper<BasketPlayerStat>()
                .in(BasketPlayerStat::getEventPlayerId, eventPlayerIds)
                .eq(BasketPlayerStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(seasonId != null, BasketPlayerStat::getSeasonId, seasonId));
        if (CollectionUtils.isEmpty(stats)) {
            return Collections.emptyList();
        }

        Map<Long, BasketEventPlayer> eventPlayerMap = eventPlayers.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(BasketEventPlayer::getId, item -> item, (a, b) -> a));

        Set<Long> eventTeamIds = stats.stream().map(BasketPlayerStat::getEventTeamId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketEventTeam> eventTeamMap = new HashMap<>();
        if (!eventTeamIds.isEmpty()) {
            for (BasketEventTeam team : eventTeamDao.selectByIds(eventTeamIds)) {
                if (team != null && Objects.equals(team.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                    eventTeamMap.put(team.getId(), team);
                }
            }
        }

        Set<Long> divisionIds = stats.stream().map(BasketPlayerStat::getDivisionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketDivision> divisionMap = new HashMap<>();
        if (!divisionIds.isEmpty()) {
            for (BasketDivision division : divisionDao.selectByIds(divisionIds)) {
                if (division != null && Objects.equals(division.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                    divisionMap.put(division.getId(), division);
                }
            }
        }

        Set<Long> seasonIds = stats.stream().map(BasketPlayerStat::getSeasonId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketSeason> seasonMap = publicSupport.loadSeasonMap(seasonIds);
        Set<Long> seriesIds = seasonMap.values().stream().map(BasketSeason::getSeriesId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketSeries> seriesMap = publicSupport.loadSeriesMap(seriesIds);

        Set<Long> logoIds = eventTeamMap.values().stream().map(BasketEventTeam::getTeamLogoFileId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> avatarIds = eventPlayers.stream().map(BasketEventPlayer::getAvatarFileId).filter(Objects::nonNull).collect(Collectors.toSet());
        logoIds.addAll(avatarIds);
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(logoIds);

        Map<Long, String> profileAvatarMap = publicSupport.loadPlayerAvatarUrlMap(Collections.singletonList(player));

        List<PublicPlayerSeasonStatVo> rows = new ArrayList<>();
        for (BasketPlayerStat stat : stats) {
            BasketEventPlayer eventPlayer = eventPlayerMap.get(stat.getEventPlayerId());
            if (eventPlayer == null) {
                continue;
            }
            BasketSeason season = seasonMap.get(stat.getSeasonId());
            // 仅公开已发布赛季；历史 FINISHED 仍可返回
            if (season == null || !PublishStatusEnum.PUBLISHED.getCode().equals(season.getPublishStatus())) {
                continue;
            }
            BasketDivision division = divisionMap.get(stat.getDivisionId());
            BasketEventTeam eventTeam = eventTeamMap.get(stat.getEventTeamId());
            BasketSeries series = season.getSeriesId() != null ? seriesMap.get(season.getSeriesId()) : null;

            PublicPlayerSeasonStatVo vo = new PublicPlayerSeasonStatVo();
            if (series != null) {
                vo.setSeriesId(series.getId());
                vo.setSeriesName(series.getSeriesName());
            }
            vo.setSeasonId(season.getId());
            vo.setSeasonName(season.getDisplayTitle() != null ? season.getDisplayTitle() : season.getSeasonName());
            if (division != null) {
                vo.setDivisionId(division.getId());
                vo.setDivisionName(division.getDivisionName());
                vo.setPlatformCode(division.getPlatformCode());
                vo.setGameMode(division.getGameMode());
            } else {
                vo.setDivisionId(stat.getDivisionId());
            }
            vo.setEventTeamId(stat.getEventTeamId());
            vo.setTeamId(stat.getEventTeamId());
            if (eventTeam != null) {
                vo.setTeamName(eventTeam.getTeamNameSnapshot());
                vo.setTeamLogoUrl(urlMap.get(eventTeam.getTeamLogoFileId()));
            }
            vo.setPlayerId(playerId);
            vo.setSourcePlayerId(playerId);
            vo.setEventPlayerId(eventPlayer.getId());
            vo.setPlayerName(eventPlayer.getPlayerNameSnapshot() != null
                    ? eventPlayer.getPlayerNameSnapshot()
                    : publicSupport.resolveDisplayName(player));
            String avatar = urlMap.get(eventPlayer.getAvatarFileId());
            if (avatar == null) {
                avatar = com.zhs.support.MemberAvatarSupport.resolve(profileAvatarMap.get(player.getId()));
            }
            vo.setAvatarUrl(avatar);

            int games = stat.getMatchCount() == null ? 0 : stat.getMatchCount();
            vo.setGames(games);
            vo.setPoints(stat.getPoints());
            vo.setRebounds(stat.getRebound());
            vo.setAssists(stat.getAssist());
            vo.setSteals(stat.getSteal());
            vo.setBlocks(stat.getBlock());
            vo.setTurnovers(stat.getTurnover());

            vo.setAvgPoints(stat.getAvgPoints() != null ? stat.getAvgPoints() : avgOf(stat.getPoints(), games));
            vo.setAvgRebounds(stat.getAvgRebound() != null ? stat.getAvgRebound() : avgOf(stat.getRebound(), games));
            vo.setAvgAssists(stat.getAvgAssist() != null ? stat.getAvgAssist() : avgOf(stat.getAssist(), games));
            vo.setAvgSteals(avgOf(stat.getSteal(), games));
            vo.setAvgBlocks(avgOf(stat.getBlock(), games));
            vo.setAvgTurnovers(avgOf(stat.getTurnover(), games));

            // 球员级 FG/3P/FT：正式表无字段，保持 null（前端显示 --）
            vo.setFieldGoalMade(null);
            vo.setFieldGoalAttempt(null);
            vo.setFieldGoalPct(null);
            vo.setThreePointMade(null);
            vo.setThreePointAttempt(null);
            vo.setThreePointPct(null);
            vo.setFreeThrowMade(null);
            vo.setFreeThrowAttempt(null);
            vo.setFreeThrowPct(null);

            rows.add(vo);
        }

        rows.sort(Comparator
                .comparing((PublicPlayerSeasonStatVo item) -> {
                    BasketSeason season = seasonMap.get(item.getSeasonId());
                    Date t = season == null ? null : (season.getMatchStartTime() != null
                            ? season.getMatchStartTime()
                            : season.getCreateTime());
                    return t == null ? 0L : t.getTime();
                })
                .reversed()
                .thenComparing(item -> item.getDivisionId() == null ? 0L : item.getDivisionId(), Comparator.reverseOrder()));
        return rows;
    }

    private BigDecimal avgOf(Integer total, int games) {
        if (total == null || games <= 0) {
            return null;
        }
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(games), 2, RoundingMode.HALF_UP);
    }

    @Override
    public IPage<PublicMatchItemVo> pageMatches(Long playerId, Integer currentPage, Integer pageSize, Long seasonId) {
        BasketPlayerProfile player = publicSupport.requirePlayer(playerId);
        if (!publicSupport.isPlayerPubliclyVisible(player) || !publicSupport.canShowMatchData(player)) {
            return publicSupport.emptyPage(currentPage, pageSize);
        }
        if (seasonId != null) {
            requirePublishedV4Season(seasonId);
        }
        List<BasketEventPlayer> eventPlayers = eventPlayerDao.selectList(new LambdaQueryWrapper<BasketEventPlayer>()
                .eq(BasketEventPlayer::getSourcePlayerId, playerId)
                .eq(BasketEventPlayer::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(seasonId != null, BasketEventPlayer::getSeasonId, seasonId));
        if (CollectionUtils.isEmpty(eventPlayers)) {
            return publicSupport.emptyPage(currentPage, pageSize);
        }
        Set<Long> eventPlayerIds = eventPlayers.stream()
                .map(BasketEventPlayer::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<BasketV4MatchPlayerStat> playerStats = matchPlayerStatDao.selectList(new LambdaQueryWrapper<BasketV4MatchPlayerStat>()
                .in(BasketV4MatchPlayerStat::getEventPlayerId, eventPlayerIds)
                .eq(BasketV4MatchPlayerStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(seasonId != null, BasketV4MatchPlayerStat::getSeasonId, seasonId)
                .orderByDesc(BasketV4MatchPlayerStat::getMatchId));
        if (CollectionUtils.isEmpty(playerStats)) {
            return publicSupport.emptyPage(currentPage, pageSize);
        }
        Set<Long> matchIds = playerStats.stream()
                .map(BasketV4MatchPlayerStat::getMatchId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> publishedSeasonIds = loadPublishedV4SeasonIds();
        if (publishedSeasonIds.isEmpty()) {
            return publicSupport.emptyPage(currentPage, pageSize);
        }
        Page<BasketV4Match> page = publicSupport.newPage(currentPage, pageSize);
        IPage<BasketV4Match> entityPage = matchDao.selectPage(page, new LambdaQueryWrapper<BasketV4Match>()
                .in(BasketV4Match::getId, matchIds)
                .in(BasketV4Match::getSeasonId, publishedSeasonIds)
                .eq(BasketV4Match::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketV4Match::getPublishStatus, MatchPublishStatusEnum.PUBLISHED.getCode())
                .eq(BasketV4Match::getMatchStatus, MatchStatusEnum.FINISHED.getCode())
                .orderByDesc(BasketV4Match::getMatchTime)
                .orderByDesc(BasketV4Match::getId));
        Page<PublicMatchItemVo> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(matchService.enrichMatches(entityPage.getRecords()));
        return result;
    }

    private void requirePublishedV4Season(Long seasonId) {
        BasketSeason season = seasonDao.selectById(seasonId);
        if (season == null
                || !Objects.equals(season.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                || !PublishStatusEnum.PUBLISHED.getCode().equals(season.getPublishStatus())) {
            throw new ServiceException("赛季不存在或未公开");
        }
    }

    private Set<Long> loadPublishedV4SeasonIds() {
        List<BasketSeason> seasons = seasonDao.selectList(new LambdaQueryWrapper<BasketSeason>()
                .eq(BasketSeason::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketSeason::getPublishStatus, PublishStatusEnum.PUBLISHED.getCode()));
        if (CollectionUtils.isEmpty(seasons)) {
            return Collections.emptySet();
        }
        return seasons.stream()
                .map(BasketSeason::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public List<PublicPlayerRankItemVo> listRanking(Long playerId, Long seasonId, String rankType) {
        BasketPlayerProfile player = publicSupport.requirePlayer(playerId);
        if (!publicSupport.isPlayerPubliclyVisible(player) || !publicSupport.canShowMatchData(player)) {
            return Collections.emptyList();
        }
        Long resolvedSeasonId = seasonId;
        if (resolvedSeasonId == null) {
            BasketSeason current = publicSupport.findCurrentPublicSeason();
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

    @Override
    public IPage<PublicHonorItemResp> pageHonors(Long playerId, Integer currentPage, Integer pageSize) {
        BasketPlayerProfile player = publicSupport.requirePlayer(playerId);
        if (!publicSupport.isPlayerPubliclyVisible(player)) {
            return publicSupport.emptyPage(currentPage, pageSize);
        }
        return honorSupport.pageByPlayer(playerId, currentPage, pageSize);
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
