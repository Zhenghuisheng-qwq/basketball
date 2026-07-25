package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketPlayerRankDao;
import com.zhs.dao.BasketTeamRankDao;
import com.zhs.enums.PublicPlayerRankTypeEnum;
import com.zhs.enums.PublicTeamRankTypeEnum;
import com.zhs.model.BasketCompetitionSeason;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketPlayerRank;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamRank;
import com.zhs.model.BasketTournament;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.response.publicapi.PublicPlayerRankItemVo;
import com.zhs.response.publicapi.PublicSeasonItemVo;
import com.zhs.response.publicapi.PublicSeasonSummaryVo;
import com.zhs.response.publicapi.PublicTeamRankItemVo;
import com.zhs.service.IBasketPublicDataCenterService;
import com.zhs.service.IBasketPublicSeasonStatService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketPublicDataCenterServiceImpl implements IBasketPublicDataCenterService {

    @Resource
    private BasketTeamRankDao teamRankDao;
    @Resource
    private BasketPlayerRankDao playerRankDao;
    @Resource
    private BasketPublicSupport publicSupport;
    @Resource
    private IBasketPublicSeasonStatService seasonStatService;

    @Override
    public List<PublicSeasonItemVo> listSeasons() {
        List<BasketTournamentSeason> seasons = publicSupport.listPublicSeasons();
        if (CollectionUtils.isEmpty(seasons)) {
            return Collections.emptyList();
        }
        Set<Long> tournamentIds = seasons.stream()
                .map(BasketTournamentSeason::getTournamentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BasketTournament> tournamentMap = publicSupport.loadTournamentMap(tournamentIds);
        Set<Long> competitionSeasonIds = tournamentMap.values().stream()
                .map(BasketTournament::getCompetitionSeasonId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BasketCompetitionSeason> competitionSeasonMap = publicSupport.loadCompetitionSeasonMap(competitionSeasonIds);
        List<PublicSeasonItemVo> result = new ArrayList<>(seasons.size());
        for (BasketTournamentSeason season : seasons) {
            if (!tournamentMap.containsKey(season.getTournamentId())) {
                continue;
            }
            PublicSeasonItemVo vo = new PublicSeasonItemVo();
            vo.setSeasonId(season.getId());
            vo.setSeasonNo(season.getSeasonNo());
            vo.setTournamentId(season.getTournamentId());
            BasketTournament tournament = tournamentMap.get(season.getTournamentId());
            vo.setTournamentName(tournament == null ? null : tournament.getTournamentName());
            vo.setSeasonName(season.getSeasonName());
            vo.setSeasonYear(season.getSeasonYear() == null ? null : String.valueOf(season.getSeasonYear()));
            vo.setSeasonStatus(season.getSeasonStatus());
            vo.setRegistrationStartTime(season.getRegistrationStartTime());
            vo.setRegistrationEndTime(season.getRegistrationEndTime());
            vo.setSeasonStartTime(season.getSeasonStartTime());
            vo.setSeasonEndTime(season.getSeasonEndTime());
            vo.setMaxTeamCount(season.getMaxTeamCount());
            if (tournament != null && tournament.getCompetitionSeasonId() != null) {
                vo.setCompetitionSeasonId(tournament.getCompetitionSeasonId());
                BasketCompetitionSeason competitionSeason = competitionSeasonMap.get(tournament.getCompetitionSeasonId());
                if (competitionSeason != null) {
                    vo.setCompetitionSeasonName(competitionSeason.getSeasonName());
                    vo.setCompetitionSeasonYear(competitionSeason.getSeasonYear());
                }
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<PublicTeamRankItemVo> listTeamRanking(Long seasonId, String rankType, Integer limit) {
        Long resolvedSeasonId = resolveSeasonId(seasonId);
        if (resolvedSeasonId == null) {
            return Collections.emptyList();
        }
        PublicTeamRankTypeEnum typeEnum = PublicTeamRankTypeEnum.fromCode(
                rankType == null || rankType.isBlank() ? PublicTeamRankTypeEnum.WIN_RATE.getCode() : rankType);
        int size = normalizeLimit(limit);
        Date snapshotDate = latestTeamSnapshotDate(resolvedSeasonId, typeEnum.getCode());
        if (snapshotDate == null) {
            return Collections.emptyList();
        }
        List<BasketTeamRank> ranks = teamRankDao.selectList(new LambdaQueryWrapper<BasketTeamRank>()
                .eq(BasketTeamRank::getSeasonId, resolvedSeasonId)
                .eq(BasketTeamRank::getRankType, typeEnum.getCode())
                .eq(BasketTeamRank::getSnapshotDate, snapshotDate)
                .eq(BasketTeamRank::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketTeamRank::getRankNo)
                .orderByAsc(BasketTeamRank::getId)
                .last("limit " + size));
        return toTeamRankVos(ranks, typeEnum);
    }

    @Override
    public List<PublicPlayerRankItemVo> listPlayerRanking(Long seasonId, String rankType, Integer limit) {
        Long resolvedSeasonId = resolveSeasonId(seasonId);
        if (resolvedSeasonId == null) {
            return Collections.emptyList();
        }
        PublicPlayerRankTypeEnum typeEnum = PublicPlayerRankTypeEnum.fromCode(
                rankType == null || rankType.isBlank() ? PublicPlayerRankTypeEnum.AVG_POINTS.getCode() : rankType);
        int size = normalizeLimit(limit);
        Date snapshotDate = latestPlayerSnapshotDate(resolvedSeasonId, typeEnum.getCode());
        if (snapshotDate == null) {
            return Collections.emptyList();
        }
        List<BasketPlayerRank> ranks = playerRankDao.selectList(new LambdaQueryWrapper<BasketPlayerRank>()
                .eq(BasketPlayerRank::getSeasonId, resolvedSeasonId)
                .eq(BasketPlayerRank::getRankType, typeEnum.getCode())
                .eq(BasketPlayerRank::getSnapshotDate, snapshotDate)
                .eq(BasketPlayerRank::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketPlayerRank::getRankNo)
                .orderByAsc(BasketPlayerRank::getId)
                .last("limit " + size));
        return toPlayerRankVos(ranks, typeEnum);
    }

    @Override
    public PublicSeasonSummaryVo getSeasonSummary(Long seasonId) {
        Long resolvedSeasonId = resolveSeasonId(seasonId);
        if (resolvedSeasonId == null) {
            return null;
        }
        return seasonStatService.getSummary(resolvedSeasonId);
    }

    public List<PublicTeamRankItemVo> toTeamRankVos(List<BasketTeamRank> ranks, PublicTeamRankTypeEnum typeEnum) {
        if (CollectionUtils.isEmpty(ranks)) {
            return Collections.emptyList();
        }
        Map<Long, BasketTeam> teamMap = publicSupport.loadPublicTeamMap(ranks.stream()
                .map(BasketTeamRank::getTeamId).collect(Collectors.toSet()));
        Map<Long, String> logoMap = publicSupport.loadAccessUrlMap(teamMap.values().stream()
                .map(BasketTeam::getLogoFileId).collect(Collectors.toSet()));
        List<PublicTeamRankItemVo> vos = new ArrayList<>(ranks.size());
        for (BasketTeamRank rank : ranks) {
            PublicTeamRankItemVo vo = new PublicTeamRankItemVo();
            vo.setSeasonId(rank.getSeasonId());
            vo.setRankType(rank.getRankType());
            vo.setRankTypeName(typeEnum.getDescription());
            vo.setTeamId(rank.getTeamId());
            BasketTeam team = teamMap.get(rank.getTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamName());
                vo.setTeamLogoUrl(logoMap.get(team.getLogoFileId()));
            }
            vo.setRankNo(rank.getRankNo());
            vo.setPreviousRank(rank.getPreviousRank());
            vo.setStatValue(rank.getStatValue());
            vo.setSnapshotDate(rank.getSnapshotDate());
            vos.add(vo);
        }
        return vos;
    }

    public List<PublicPlayerRankItemVo> toPlayerRankVos(List<BasketPlayerRank> ranks, PublicPlayerRankTypeEnum typeEnum) {
        if (CollectionUtils.isEmpty(ranks)) {
            return Collections.emptyList();
        }
        Map<Long, BasketPlayerProfile> playerMap = publicSupport.loadPlayerMap(ranks.stream()
                .map(BasketPlayerRank::getPlayerId).collect(Collectors.toSet()));
        Map<Long, String> avatarMap = publicSupport.loadPlayerAvatarUrlMap(playerMap.values());
        Map<Long, BasketTeam> teamMap = publicSupport.loadPublicTeamMap(ranks.stream()
                .map(BasketPlayerRank::getTeamId).collect(Collectors.toSet()));
        List<PublicPlayerRankItemVo> vos = new ArrayList<>();
        for (BasketPlayerRank rank : ranks) {
            BasketPlayerProfile player = playerMap.get(rank.getPlayerId());
            if (player == null || !publicSupport.isPlayerPubliclyVisible(player)) {
                continue;
            }
            PublicPlayerRankItemVo vo = new PublicPlayerRankItemVo();
            vo.setSeasonId(rank.getSeasonId());
            vo.setRankType(rank.getRankType());
            vo.setRankTypeName(typeEnum.getDescription());
            vo.setPlayerId(rank.getPlayerId());
            vo.setPlayerName(publicSupport.resolveDisplayName(player));
            vo.setAvatarUrl(avatarMap.get(player.getId()));
            vo.setTeamId(rank.getTeamId());
            BasketTeam team = teamMap.get(rank.getTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamName());
            }
            vo.setRankNo(rank.getRankNo());
            vo.setPreviousRank(rank.getPreviousRank());
            vo.setStatValue(rank.getStatValue());
            vo.setSnapshotDate(rank.getSnapshotDate());
            vos.add(vo);
        }
        return vos;
    }

    private Long resolveSeasonId(Long seasonId) {
        if (seasonId != null) {
            publicSupport.requirePublicSeason(seasonId);
            return seasonId;
        }
        BasketTournamentSeason current = publicSupport.findCurrentPublicSeason();
        return current == null ? null : current.getId();
    }

    private Date latestTeamSnapshotDate(Long seasonId, String rankType) {
        BasketTeamRank latest = teamRankDao.selectOne(new LambdaQueryWrapper<BasketTeamRank>()
                .eq(BasketTeamRank::getSeasonId, seasonId)
                .eq(BasketTeamRank::getRankType, rankType)
                .eq(BasketTeamRank::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByDesc(BasketTeamRank::getSnapshotDate)
                .last("limit 1"));
        return latest == null ? null : latest.getSnapshotDate();
    }

    private Date latestPlayerSnapshotDate(Long seasonId, String rankType) {
        BasketPlayerRank latest = playerRankDao.selectOne(new LambdaQueryWrapper<BasketPlayerRank>()
                .eq(BasketPlayerRank::getSeasonId, seasonId)
                .eq(BasketPlayerRank::getRankType, rankType)
                .eq(BasketPlayerRank::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByDesc(BasketPlayerRank::getSnapshotDate)
                .last("limit 1"));
        return latest == null ? null : latest.getSnapshotDate();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return BasketPublicConstants.HOME_LIMIT;
        }
        return Math.min(limit, BasketPublicConstants.MAX_SIZE);
    }
}
