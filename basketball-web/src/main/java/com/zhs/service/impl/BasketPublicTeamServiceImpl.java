package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.BasketTeamRankDao;
import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.dao.v4.BasketSeasonDao;
import com.zhs.dao.v4.BasketV4MatchDao;
import com.zhs.enums.PublicTeamRankTypeEnum;
import com.zhs.enums.v4.MatchPublishStatusEnum;
import com.zhs.enums.v4.MatchStatusEnum;
import com.zhs.enums.v4.PublishStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.BasketTeamRank;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketSeason;
import com.zhs.model.v4.BasketSeries;
import com.zhs.model.v4.BasketV4Match;
import com.zhs.response.publicapi.PublicHonorItemResp;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.response.publicapi.PublicTeamCompareVo;
import com.zhs.response.publicapi.PublicTeamDetailVo;
import com.zhs.response.publicapi.PublicTeamRankItemVo;
import com.zhs.response.publicapi.PublicTeamRosterItemVo;
import com.zhs.response.publicapi.PublicTeamSeasonHistoryVo;
import com.zhs.response.publicapi.PublicTeamSeasonStatVo;
import com.zhs.service.IBasketPublicTeamService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicHonorSupport;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketPublicTeamServiceImpl implements IBasketPublicTeamService {

    @Resource
    private BasketEventTeamDao eventTeamDao;
    @Resource
    private BasketV4MatchDao matchDao;
    @Resource
    private BasketSeasonDao seasonDao;
    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketTeamRankDao teamRankDao;
    @Resource
    private BasketPublicSupport publicSupport;
    @Resource
    private BasketPublicHonorSupport honorSupport;
    @Resource
    private BasketPublicMatchServiceImpl matchService;
    @Resource
    private BasketPublicDataCenterServiceImpl dataCenterService;

    @Override
    public PublicTeamDetailVo getTeam(Long teamId) {
        BasketTeam team = publicSupport.requirePublicTeam(teamId);
        return toDetail(team);
    }

    @Override
    public List<PublicTeamSeasonHistoryVo> listSeasonHistory(Long teamId) {
        publicSupport.requirePublicTeam(teamId);
        List<BasketEventTeam> eventTeams = eventTeamDao.selectList(new LambdaQueryWrapper<BasketEventTeam>()
                .eq(BasketEventTeam::getSourceTeamId, teamId)
                .eq(BasketEventTeam::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByDesc(BasketEventTeam::getSeasonId));
        if (CollectionUtils.isEmpty(eventTeams)) {
            return Collections.emptyList();
        }
        Set<Long> seasonIds = eventTeams.stream().map(BasketEventTeam::getSeasonId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketSeason> seasonMap = publicSupport.loadSeasonMap(seasonIds);
        Map<Long, BasketSeries> seriesMap = publicSupport.loadSeriesMap(seasonMap.values().stream()
                .map(BasketSeason::getSeriesId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<PublicTeamSeasonHistoryVo> vos = new ArrayList<>();
        for (BasketEventTeam eventTeam : eventTeams) {
            BasketSeason season = seasonMap.get(eventTeam.getSeasonId());
            if (season == null) {
                continue;
            }
            BasketSeries series = seriesMap.get(season.getSeriesId());
            PublicTeamSeasonHistoryVo vo = new PublicTeamSeasonHistoryVo();
            vo.setSeasonId(season.getId());
            vo.setSeasonName(season.getDisplayTitle() != null ? season.getDisplayTitle() : season.getSeasonName());
            vo.setTournamentId(season.getSeriesId());
            vo.setTournamentName(series == null ? null : series.getSeriesName());
            vo.setDisplayName(eventTeam.getEventTeamName());
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public IPage<PublicMatchItemVo> pageMatches(Long teamId, Integer currentPage, Integer pageSize, Long seasonId) {
        publicSupport.requirePublicTeam(teamId);
        if (seasonId != null) {
            requirePublishedV4Season(seasonId);
        }
        Set<Long> eventTeamIds = findEventTeamIdsBySourceTeam(seasonId, teamId);
        if (eventTeamIds.isEmpty()) {
            return publicSupport.emptyPage(currentPage, pageSize);
        }
        Set<Long> publishedSeasonIds = loadPublishedV4SeasonIds();
        if (publishedSeasonIds.isEmpty()) {
            return publicSupport.emptyPage(currentPage, pageSize);
        }
        Page<BasketV4Match> page = publicSupport.newPage(currentPage, pageSize);
        LambdaQueryWrapper<BasketV4Match> wrapper = new LambdaQueryWrapper<BasketV4Match>()
                .eq(BasketV4Match::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketV4Match::getPublishStatus, MatchPublishStatusEnum.PUBLISHED.getCode())
                .eq(BasketV4Match::getMatchStatus, MatchStatusEnum.FINISHED.getCode())
                .in(BasketV4Match::getSeasonId, publishedSeasonIds)
                .eq(seasonId != null, BasketV4Match::getSeasonId, seasonId)
                .and(w -> w.in(BasketV4Match::getHomeEventTeamId, eventTeamIds)
                        .or().in(BasketV4Match::getAwayEventTeamId, eventTeamIds))
                .orderByDesc(BasketV4Match::getMatchTime)
                .orderByDesc(BasketV4Match::getRoundNo)
                .orderByDesc(BasketV4Match::getId);
        IPage<BasketV4Match> entityPage = matchDao.selectPage(page, wrapper);
        Page<PublicMatchItemVo> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(matchService.enrichMatches(entityPage.getRecords()));
        return result;
    }

    @Override
    public List<PublicTeamRosterItemVo> listRoster(Long teamId) {
        publicSupport.requirePublicTeam(teamId);
        List<BasketTeamMember> members = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketTeamMember::getIsCurrent, BasketPublicConstants.MEMBERSHIP_CURRENT)
                .orderByAsc(BasketTeamMember::getJerseyNo)
                .orderByAsc(BasketTeamMember::getId));
        if (CollectionUtils.isEmpty(members)) {
            return Collections.emptyList();
        }
        Map<Long, BasketPlayerProfile> playerMap = publicSupport.loadPlayerMap(members.stream()
                .map(BasketTeamMember::getPlayerId).collect(Collectors.toSet()));
        Map<Long, String> avatarMap = publicSupport.loadPlayerAvatarUrlMap(playerMap.values());
        List<PublicTeamRosterItemVo> vos = new ArrayList<>();
        for (BasketTeamMember member : members) {
            BasketPlayerProfile player = playerMap.get(member.getPlayerId());
            if (player == null || !publicSupport.isPlayerPubliclyVisible(player)) {
                continue;
            }
            PublicTeamRosterItemVo vo = new PublicTeamRosterItemVo();
            vo.setPlayerId(player.getId());
            vo.setPlayerName(publicSupport.resolveDisplayName(player));
            vo.setAvatarUrl(com.zhs.support.MemberAvatarSupport.resolve(avatarMap.get(player.getId())));
            vo.setPositionCode(player.getPositionCode());
            vo.setJerseyNo(member.getJerseyNo());
            vo.setRoleType(member.getRoleType());
            vo.setRosterType(member.getRosterType());
            vo.setCertificationStatus(player.getCertificationStatus());
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public List<PublicTeamSeasonStatVo> listStat(Long teamId, Long seasonId) {
        publicSupport.requirePublicTeam(teamId);
        if (seasonId != null) {
            publicSupport.requirePublicSeason(seasonId);
        }
        return Collections.emptyList();
    }

    @Override
    public List<PublicTeamRankItemVo> listRanking(Long teamId, Long seasonId, String rankType) {
        publicSupport.requirePublicTeam(teamId);
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
        PublicTeamRankTypeEnum typeEnum = PublicTeamRankTypeEnum.fromCode(
                rankType == null || rankType.isBlank() ? PublicTeamRankTypeEnum.WIN_RATE.getCode() : rankType);
        Date snapshotDate = latestSnapshot(resolvedSeasonId, typeEnum.getCode());
        if (snapshotDate == null) {
            return Collections.emptyList();
        }
        List<BasketTeamRank> ranks = teamRankDao.selectList(new LambdaQueryWrapper<BasketTeamRank>()
                .eq(BasketTeamRank::getSeasonId, resolvedSeasonId)
                .eq(BasketTeamRank::getRankType, typeEnum.getCode())
                .eq(BasketTeamRank::getSnapshotDate, snapshotDate)
                .eq(BasketTeamRank::getTeamId, teamId)
                .eq(BasketTeamRank::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketTeamRank::getRankNo));
        return dataCenterService.toTeamRankVos(ranks, typeEnum);
    }

    @Override
    public PublicTeamCompareVo compare(Long leftTeamId, Long rightTeamId, Long seasonId) {
        if (leftTeamId == null || rightTeamId == null) {
            throw new ServiceException("对比球队主键不能为空");
        }
        if (Objects.equals(leftTeamId, rightTeamId)) {
            throw new ServiceException("对比球队不能相同");
        }
        BasketSeason season;
        if (seasonId == null) {
            season = publicSupport.findCurrentPublicSeason();
            if (season == null) {
                throw new ServiceException("暂无公开赛季");
            }
        } else {
            season = publicSupport.requirePublicSeason(seasonId);
        }
        PublicTeamCompareVo vo = new PublicTeamCompareVo();
        vo.setLeftTeam(toDetail(publicSupport.requirePublicTeam(leftTeamId)));
        vo.setRightTeam(toDetail(publicSupport.requirePublicTeam(rightTeamId)));
        vo.setSeasonId(season.getId());
        vo.setSeasonName(season.getDisplayTitle() != null ? season.getDisplayTitle() : season.getSeasonName());
        return vo;
    }

    private PublicTeamSeasonStatVo findSeasonTeamStat(Long teamId, Long seasonId) {
        return null;
    }

    private PublicTeamDetailVo toDetail(BasketTeam team) {
        Map<Long, String> logoMap = publicSupport.loadAccessUrlMap(Collections.singletonList(team.getLogoFileId()));
        PublicTeamDetailVo vo = new PublicTeamDetailVo();
        vo.setTeamId(team.getId());
        vo.setTeamNo(team.getTeamNo());
        vo.setTeamName(team.getTeamName());
        vo.setShortName(team.getShortName());
        vo.setEnglishName(team.getEnglishName());
        vo.setLogoUrl(logoMap.get(team.getLogoFileId()));
        vo.setRegionCode(team.getRegionCode());
        vo.setRegionName(team.getRegionName());
        vo.setGameTerminal(team.getGameTerminal());
        vo.setDescription(team.getDescription());
        vo.setTeamStatus(team.getTeamStatus());
        vo.setCurrentMemberCount(team.getCurrentMemberCount());
        vo.setEstablishedDate(team.getEstablishedDate());
        return vo;
    }

    private Set<Long> findEventTeamIdsBySourceTeam(Long seasonId, Long sourceTeamId) {
        LambdaQueryWrapper<BasketEventTeam> wrapper = new LambdaQueryWrapper<BasketEventTeam>()
                .eq(BasketEventTeam::getSourceTeamId, sourceTeamId)
                .eq(BasketEventTeam::getIsDelete, BasketPublicConstants.NOT_DELETED);
        if (seasonId != null) {
            wrapper.eq(BasketEventTeam::getSeasonId, seasonId);
        }
        List<BasketEventTeam> teams = eventTeamDao.selectList(wrapper);
        if (CollectionUtils.isEmpty(teams)) {
            return Collections.emptySet();
        }
        return teams.stream().map(BasketEventTeam::getId).filter(Objects::nonNull).collect(Collectors.toSet());
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
    public IPage<PublicHonorItemResp> pageHonors(Long teamId, Integer currentPage, Integer pageSize) {
        publicSupport.requirePublicTeam(teamId);
        return honorSupport.pageByTeam(teamId, currentPage, pageSize);
    }

    private Date latestSnapshot(Long seasonId, String rankType) {
        BasketTeamRank latest = teamRankDao.selectOne(new LambdaQueryWrapper<BasketTeamRank>()
                .eq(BasketTeamRank::getSeasonId, seasonId)
                .eq(BasketTeamRank::getRankType, rankType)
                .eq(BasketTeamRank::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByDesc(BasketTeamRank::getSnapshotDate)
                .last("limit 1"));
        return latest == null ? null : latest.getSnapshotDate();
    }
}
