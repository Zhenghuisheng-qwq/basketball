package com.zhs.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketTournamentDao;
import com.zhs.dao.BasketTournamentSeasonDao;
import com.zhs.dao.BasketTournamentSeasonPlatformDao;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketTournament;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.model.BasketTournamentSeasonPlatform;
import com.zhs.response.team.SeasonPlatformItemResp;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 赛事赛季开放平台校验。
 */
@Component
public class EventPlatformSupport {

    private static final Integer NOT_DELETED = 0;
    private static final int PLATFORM_OPEN = 1;

    @Resource
    private BasketTournamentSeasonPlatformDao seasonPlatformDao;
    @Resource
    private BasketTournamentSeasonDao seasonDao;
    @Resource
    private BasketTournamentDao tournamentDao;
    @Resource
    private PlatformTeamMembershipSupport platformTeamMembershipSupport;

    public String requireOpenPlatform(Long seasonId, String platformCode) {
        String normalized = resolveRequestedPlatformCode(seasonId, platformCode);
        if (normalized == null) {
            throw new ServiceException("该赛事未配置游戏端/参赛端");
        }
        if (isPlatformConfiguredOpen(seasonId, normalized)) {
            return normalized;
        }
        throw new ServiceException("该赛事未开放 " + GameTerminalSupport.formatDisplayName(normalized) + " 参赛端报名");
    }

    public boolean isPlatformOpen(Long seasonId, String platformCode) {
        if (seasonId == null) {
            return false;
        }
        String normalized = resolveRequestedPlatformCode(seasonId, platformCode);
        return normalized != null && isPlatformConfiguredOpen(seasonId, normalized);
    }

    public String resolveEventPlatformCode(Long seasonId) {
        if (seasonId == null) {
            return null;
        }
        List<BasketTournamentSeasonPlatform> openPlatforms = listOpenPlatformRows(seasonId);
        if (openPlatforms.size() == 1) {
            return normalizePlatformCode(openPlatforms.get(0).getPlatformCode());
        }
        if (openPlatforms.size() > 1) {
            return normalizePlatformCode(openPlatforms.get(0).getPlatformCode());
        }
        BasketTournament tournament = loadTournamentBySeasonId(seasonId);
        return tournament == null ? null : GameTerminalSupport.normalize(tournament.getGameTerminal());
    }

    public List<SeasonPlatformItemResp> listSeasonPlatforms(Long seasonId) {
        if (seasonId == null) {
            return Collections.emptyList();
        }
        String eventPlatformCode = resolveEventPlatformCode(seasonId);
        if (!StringUtils.hasText(eventPlatformCode)) {
            return Collections.emptyList();
        }
        SeasonPlatformItemResp item = new SeasonPlatformItemResp();
        item.setPlatformCode(eventPlatformCode);
        item.setPlatformName(resolvePlatformName(eventPlatformCode));
        item.setOpen(true);
        item.setDisabledReason(null);
        return List.of(item);
    }

    public String resolvePlatformName(String platformCode) {
        return GameTerminalSupport.formatDisplayName(platformCode);
    }

    private String resolveRequestedPlatformCode(Long seasonId, String platformCode) {
        if (StringUtils.hasText(platformCode)) {
            return platformTeamMembershipSupport.requirePlatformCode(platformCode);
        }
        return resolveEventPlatformCode(seasonId);
    }

    private boolean isPlatformConfiguredOpen(Long seasonId, String platformCode) {
        if (seasonId == null || !StringUtils.hasText(platformCode)) {
            return false;
        }
        String normalized = normalizePlatformCode(platformCode);
        for (BasketTournamentSeasonPlatform platform : listOpenPlatformRows(seasonId)) {
            if (normalized.equals(normalizePlatformCode(platform.getPlatformCode()))) {
                return true;
            }
        }
        BasketTournament tournament = loadTournamentBySeasonId(seasonId);
        if (tournament == null) {
            return false;
        }
        return GameTerminalSupport.matches(tournament.getGameTerminal(), normalized);
    }

    private List<BasketTournamentSeasonPlatform> listOpenPlatformRows(Long seasonId) {
        return seasonPlatformDao.selectList(new LambdaQueryWrapper<BasketTournamentSeasonPlatform>()
                .eq(BasketTournamentSeasonPlatform::getSeasonId, seasonId)
                .eq(BasketTournamentSeasonPlatform::getPlatformStatus, PLATFORM_OPEN)
                .eq(BasketTournamentSeasonPlatform::getIsDelete, NOT_DELETED)
                .orderByAsc(BasketTournamentSeasonPlatform::getId));
    }

    private BasketTournament loadTournamentBySeasonId(Long seasonId) {
        BasketTournamentSeason season = seasonDao.selectById(seasonId);
        if (season == null || season.getTournamentId() == null) {
            return null;
        }
        return tournamentDao.selectById(season.getTournamentId());
    }

    private String normalizePlatformCode(String platformCode) {
        return GameTerminalSupport.normalize(platformCode);
    }
}
