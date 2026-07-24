package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketStageStandingDao;
import com.zhs.model.BasketStageStanding;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTournamentGroup;
import com.zhs.model.BasketTournamentStage;
import com.zhs.response.publicapi.PublicStandingItemVo;
import com.zhs.service.IBasketPublicStandingService;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketPublicStandingServiceImpl implements IBasketPublicStandingService {

    @Resource
    private BasketStageStandingDao standingDao;
    @Resource
    private BasketPublicSupport publicSupport;

    @Override
    public List<PublicStandingItemVo> listBySeason(Long seasonId, Long stageId) {
        publicSupport.requirePublicSeason(seasonId);
        LambdaQueryWrapper<BasketStageStanding> wrapper = new LambdaQueryWrapper<BasketStageStanding>()
                .eq(BasketStageStanding::getSeasonId, seasonId)
                .eq(BasketStageStanding::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(stageId != null, BasketStageStanding::getStageId, stageId)
                .orderByAsc(BasketStageStanding::getStageId)
                .orderByAsc(BasketStageStanding::getGroupId)
                .orderByAsc(BasketStageStanding::getRankNo)
                .orderByAsc(BasketStageStanding::getId);
        return enrich(standingDao.selectList(wrapper));
    }

    @Override
    public List<PublicStandingItemVo> listByStage(Long stageId) {
        publicSupport.requirePublicStage(stageId);
        List<BasketStageStanding> list = standingDao.selectList(new LambdaQueryWrapper<BasketStageStanding>()
                .eq(BasketStageStanding::getStageId, stageId)
                .eq(BasketStageStanding::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketStageStanding::getGroupId)
                .orderByAsc(BasketStageStanding::getRankNo)
                .orderByAsc(BasketStageStanding::getId));
        return enrich(list);
    }

    @Override
    public List<PublicStandingItemVo> listByGroup(Long groupId) {
        publicSupport.requirePublicGroup(groupId);
        List<BasketStageStanding> list = standingDao.selectList(new LambdaQueryWrapper<BasketStageStanding>()
                .eq(BasketStageStanding::getGroupId, groupId)
                .eq(BasketStageStanding::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketStageStanding::getRankNo)
                .orderByAsc(BasketStageStanding::getId));
        return enrich(list);
    }

    private List<PublicStandingItemVo> enrich(List<BasketStageStanding> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        Set<Long> teamIds = new HashSet<>();
        Set<Long> stageIds = new HashSet<>();
        Set<Long> groupIds = new HashSet<>();
        for (BasketStageStanding item : list) {
            if (item.getTeamId() != null) {
                teamIds.add(item.getTeamId());
            }
            if (item.getStageId() != null) {
                stageIds.add(item.getStageId());
            }
            if (item.getGroupId() != null) {
                groupIds.add(item.getGroupId());
            }
        }
        Map<Long, BasketTeam> teamMap = publicSupport.loadPublicTeamMap(teamIds);
        Map<Long, String> logoMap = publicSupport.loadAccessUrlMap(teamMap.values().stream()
                .map(BasketTeam::getLogoFileId).collect(Collectors.toSet()));
        Map<Long, BasketTournamentStage> stageMap = publicSupport.loadStageMap(stageIds);
        Map<Long, BasketTournamentGroup> groupMap = publicSupport.loadGroupMap(groupIds);

        List<PublicStandingItemVo> vos = new ArrayList<>(list.size());
        for (BasketStageStanding item : list) {
            PublicStandingItemVo vo = new PublicStandingItemVo();
            vo.setStandingId(item.getId());
            vo.setSeasonId(item.getSeasonId());
            vo.setStageId(item.getStageId());
            vo.setGroupId(item.getGroupId());
            vo.setTeamId(item.getTeamId());
            vo.setRankNo(item.getRankNo());
            vo.setMatchesPlayed(item.getMatchesPlayed());
            vo.setWins(item.getWins());
            vo.setLosses(item.getLosses());
            vo.setPointsFor(item.getPointsFor());
            vo.setPointsAgainst(item.getPointsAgainst());
            vo.setPointDiff(item.getPointDiff());
            vo.setStandingPoints(item.getStandingPoints());
            vo.setWinRate(item.getWinRate());
            vo.setStreakType(item.getStreakType());
            vo.setStreakCount(item.getStreakCount());
            vo.setQualifiedFlag(item.getQualifiedFlag());
            BasketTeam team = teamMap.get(item.getTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamName());
                vo.setTeamLogoUrl(logoMap.get(team.getLogoFileId()));
            }
            BasketTournamentStage stage = stageMap.get(item.getStageId());
            if (stage != null) {
                vo.setStageName(stage.getStageName());
            }
            BasketTournamentGroup group = groupMap.get(item.getGroupId());
            if (group != null) {
                vo.setGroupName(group.getGroupName());
            }
            vos.add(vo);
        }
        return vos;
    }
}
