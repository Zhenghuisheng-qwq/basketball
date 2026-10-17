package com.zhs.basket.support.v4;

import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.mapper.v4.BasketEventPlayerMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 资料或名单生效后，失效官网当前展示缓存。不清理历史比赛缓存。
 */
@Component
public class CurrentDisplayCacheEvictor {

    @Autowired
    private V4PublicReadCachePublisher cachePublisher;
    @Autowired
    private BasketPlayerProfileMapper playerProfileMapper;
    @Autowired
    private BasketEventPlayerMapper eventPlayerMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;

    public void afterMemberProfile(Long memberId) {
        if (memberId == null) {
            return;
        }
        BasketPlayerProfile profile = playerProfileMapper.selectByMemberId(memberId);
        if (profile == null || profile.getId() == null) {
            return;
        }
        cachePublisher.playerMutation(profile.getId());
        for (Long eventTeamId : eventPlayerMapper.selectEventTeamIdsBySourcePlayer(profile.getId())) {
            evictEventTeam(eventTeamId);
        }
    }

    public void afterTeamProfile(Long sourceTeamId) {
        if (sourceTeamId == null) {
            return;
        }
        List<BasketEventTeam> teams = eventTeamMapper.selectBySourceTeamId(sourceTeamId);
        if (teams == null) {
            return;
        }
        for (BasketEventTeam team : teams) {
            if (team != null) {
                evictEventTeam(team.getId(), team.getDivisionId(), team.getSeasonId());
            }
        }
    }

    public void afterSourceTeam(Long divisionId, Long sourceTeamId, Long seasonId) {
        if (divisionId == null || sourceTeamId == null) {
            return;
        }
        BasketEventTeam team = eventTeamMapper.selectByDivisionSourceTeam(divisionId, sourceTeamId);
        if (team != null) {
            evictEventTeam(team.getId(), divisionId, seasonId);
        }
    }

    public void afterEventTeam(Long eventTeamId, Long divisionId, Long seasonId) {
        evictEventTeam(eventTeamId, divisionId, seasonId);
    }

    private void evictEventTeam(Long eventTeamId) {
        if (eventTeamId == null) {
            return;
        }
        BasketEventTeam team = eventTeamMapper.selectById(eventTeamId);
        if (team == null) {
            cachePublisher.teamMutation(eventTeamId, null, null);
            return;
        }
        evictEventTeam(team.getId(), team.getDivisionId(), team.getSeasonId());
    }

    private void evictEventTeam(Long eventTeamId, Long divisionId, Long seasonId) {
        if (eventTeamId == null) {
            return;
        }
        cachePublisher.teamMutation(eventTeamId, divisionId, seasonId);
    }
}
