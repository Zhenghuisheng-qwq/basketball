package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.dao.BasketMatchDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.BasketTeamRankDao;
import com.zhs.dao.BasketTeamSeasonProfileDao;
import com.zhs.dao.BasketTeamSeasonStatDao;
import com.zhs.enums.PublicTeamRankTypeEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketMatch;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.BasketTeamRank;
import com.zhs.model.BasketTeamSeasonProfile;
import com.zhs.model.BasketTeamSeasonStat;
import com.zhs.model.BasketTournament;
import com.zhs.model.BasketTournamentSeason;
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
    private BasketTeamSeasonProfileDao teamSeasonProfileDao;
    @Resource
    private BasketTeamSeasonStatDao teamSeasonStatDao;
    @Resource
    private BasketMatchDao matchDao;
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
    private BasketPublicSeasonStatServiceImpl seasonStatService;
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
        List<BasketTeamSeasonProfile> profiles = teamSeasonProfileDao.selectList(new LambdaQueryWrapper<BasketTeamSeasonProfile>()
                .eq(BasketTeamSeasonProfile::getTeamId, teamId)
                .eq(BasketTeamSeasonProfile::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByDesc(BasketTeamSeasonProfile::getSeasonId));
        if (CollectionUtils.isEmpty(profiles)) {
            return Collections.emptyList();
        }
        Set<Long> seasonIds = profiles.stream().map(BasketTeamSeasonProfile::getSeasonId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketTournamentSeason> seasonMap = publicSupport.loadSeasonMap(seasonIds);
        Map<Long, BasketTournament> tournamentMap = publicSupport.loadTournamentMap(seasonMap.values().stream()
                .map(BasketTournamentSeason::getTournamentId).collect(Collectors.toSet()));
        List<BasketTeamSeasonStat> stats = teamSeasonStatDao.selectList(new LambdaQueryWrapper<BasketTeamSeasonStat>()
                .eq(BasketTeamSeasonStat::getTeamId, teamId)
                .in(BasketTeamSeasonStat::getSeasonId, seasonIds)
                .eq(BasketTeamSeasonStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .isNull(BasketTeamSeasonStat::getStageId));
        Map<Long, BasketTeamSeasonStat> seasonStatMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(stats)) {
            for (BasketTeamSeasonStat stat : stats) {
                seasonStatMap.putIfAbsent(stat.getSeasonId(), stat);
            }
        }
        List<PublicTeamSeasonHistoryVo> vos = new ArrayList<>();
        for (BasketTeamSeasonProfile profile : profiles) {
            BasketTournamentSeason season = seasonMap.get(profile.getSeasonId());
            if (season == null) {
                continue;
            }
            BasketTournament tournament = tournamentMap.get(season.getTournamentId());
            PublicTeamSeasonHistoryVo vo = new PublicTeamSeasonHistoryVo();
            vo.setSeasonId(season.getId());
            vo.setSeasonName(season.getSeasonName());
            vo.setTournamentId(season.getTournamentId());
            vo.setTournamentName(tournament == null ? null : tournament.getTournamentName());
            vo.setDisplayName(profile.getDisplayName());
            vo.setFinalRank(profile.getFinalRank());
            vo.setSeasonStatus(profile.getSeasonStatus());
            BasketTeamSeasonStat stat = seasonStatMap.get(season.getId());
            if (stat != null) {
                vo.setGames(stat.getGames());
                vo.setWins(stat.getWins());
                vo.setLosses(stat.getLosses());
                vo.setWinRate(stat.getWinRate());
            }
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public IPage<PublicMatchItemVo> pageMatches(Long teamId, Integer currentPage, Integer pageSize, Long seasonId) {
        publicSupport.requirePublicTeam(teamId);
        if (seasonId != null) {
            publicSupport.requirePublicSeason(seasonId);
        }
        Page<BasketMatch> page = publicSupport.newPage(currentPage, pageSize);
        LambdaQueryWrapper<BasketMatch> wrapper = new LambdaQueryWrapper<BasketMatch>()
                .eq(BasketMatch::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketMatch::getPublicStatus, BasketPublicConstants.PUBLIC)
                .eq(BasketMatch::getMatchStatus, BasketPublicConstants.MATCH_FINISHED)
                .eq(BasketMatch::getDataStatus, BasketPublicConstants.DATA_CONFIRMED)
                .eq(seasonId != null, BasketMatch::getSeasonId, seasonId)
                .and(w -> w.eq(BasketMatch::getHomeTeamId, teamId).or().eq(BasketMatch::getAwayTeamId, teamId))
                .orderByDesc(BasketMatch::getActualEndTime)
                .orderByDesc(BasketMatch::getScheduledStartTime)
                .orderByDesc(BasketMatch::getId);
        IPage<BasketMatch> entityPage = matchDao.selectPage(page, wrapper);
        // 仅保留所属赛季公开的比赛
        List<BasketMatch> filtered = filterPublicSeasonMatches(entityPage.getRecords());
        Page<PublicMatchItemVo> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(matchService.enrichMatches(filtered));
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
        LambdaQueryWrapper<BasketTeamSeasonStat> wrapper = new LambdaQueryWrapper<BasketTeamSeasonStat>()
                .eq(BasketTeamSeasonStat::getTeamId, teamId)
                .eq(BasketTeamSeasonStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(seasonId != null, BasketTeamSeasonStat::getSeasonId, seasonId)
                .orderByDesc(BasketTeamSeasonStat::getSeasonId)
                .orderByAsc(BasketTeamSeasonStat::getStageId);
        List<BasketTeamSeasonStat> stats = teamSeasonStatDao.selectList(wrapper);
        if (CollectionUtils.isEmpty(stats)) {
            return Collections.emptyList();
        }
        Set<Long> publicSeasonIds = publicSupport.loadSeasonMap(stats.stream()
                .map(BasketTeamSeasonStat::getSeasonId).collect(Collectors.toSet())).keySet();
        List<BasketTeamSeasonStat> filtered = stats.stream()
                .filter(s -> publicSeasonIds.contains(s.getSeasonId()))
                .collect(Collectors.toList());
        return seasonStatService.toTeamStatVos(filtered);
    }

    @Override
    public List<PublicTeamRankItemVo> listRanking(Long teamId, Long seasonId, String rankType) {
        publicSupport.requirePublicTeam(teamId);
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
        BasketTournamentSeason season;
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
        vo.setSeasonName(season.getSeasonName());
        vo.setLeftStat(findSeasonTeamStat(leftTeamId, season.getId()));
        vo.setRightStat(findSeasonTeamStat(rightTeamId, season.getId()));
        return vo;
    }

    private PublicTeamSeasonStatVo findSeasonTeamStat(Long teamId, Long seasonId) {
        BasketTeamSeasonStat stat = teamSeasonStatDao.selectOne(new LambdaQueryWrapper<BasketTeamSeasonStat>()
                .eq(BasketTeamSeasonStat::getTeamId, teamId)
                .eq(BasketTeamSeasonStat::getSeasonId, seasonId)
                .eq(BasketTeamSeasonStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .isNull(BasketTeamSeasonStat::getStageId)
                .last("limit 1"));
        if (stat == null) {
            stat = teamSeasonStatDao.selectOne(new LambdaQueryWrapper<BasketTeamSeasonStat>()
                    .eq(BasketTeamSeasonStat::getTeamId, teamId)
                    .eq(BasketTeamSeasonStat::getSeasonId, seasonId)
                    .eq(BasketTeamSeasonStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                    .orderByAsc(BasketTeamSeasonStat::getStageId)
                    .last("limit 1"));
        }
        if (stat == null) {
            return null;
        }
        List<PublicTeamSeasonStatVo> vos = seasonStatService.toTeamStatVos(Collections.singletonList(stat));
        return vos.isEmpty() ? null : vos.get(0);
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

    private List<BasketMatch> filterPublicSeasonMatches(List<BasketMatch> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return Collections.emptyList();
        }
        Set<Long> seasonIds = matches.stream().map(BasketMatch::getSeasonId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> publicSeasonIds = publicSupport.loadSeasonMap(seasonIds).keySet();
        return matches.stream().filter(m -> publicSeasonIds.contains(m.getSeasonId())).collect(Collectors.toList());
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
