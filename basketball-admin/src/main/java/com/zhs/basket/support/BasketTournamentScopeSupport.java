package com.zhs.basket.support;

import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.mapper.BasketTournamentAdminScopeMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class BasketTournamentScopeSupport {

    @Autowired private BasketTournamentAdminScopeMapper scopeMapper;
    @Autowired private BasketTournamentSeasonMapper seasonMapper;

    public void assertTournamentAccess(Long tournamentId) {
        if (tournamentId == null || SecurityUtils.isAdmin()) {
            return;
        }
        if (scopeMapper.countAuthorized(SecurityUtils.getUserId(), tournamentId) <= 0) {
            throw new ServiceException("无权访问该赛事");
        }
    }

    public List<Long> filterSeasonIdsForAdmin(List<Long> seasonIds) {
        if (SecurityUtils.isAdmin()) {
            return seasonIds == null ? Collections.emptyList() : seasonIds;
        }
        List<Long> tournamentIds = scopeMapper.selectTournamentIdsByAdmin(SecurityUtils.getUserId());
        if (tournamentIds == null || tournamentIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> accessibleSeasonIds = new ArrayList<>();
        for (Long tournamentId : tournamentIds) {
            List<BasketTournamentSeason> seasons = seasonMapper.selectByTournamentId(tournamentId);
            if (seasons != null) {
                accessibleSeasonIds.addAll(seasons.stream().map(BasketTournamentSeason::getId).collect(Collectors.toList()));
            }
        }
        if (seasonIds == null || seasonIds.isEmpty()) {
            return accessibleSeasonIds;
        }
        return seasonIds.stream().filter(accessibleSeasonIds::contains).collect(Collectors.toList());
    }

    public List<Long> accessibleTournamentIds() {
        if (SecurityUtils.isAdmin()) {
            return null;
        }
        List<Long> ids = scopeMapper.selectTournamentIdsByAdmin(SecurityUtils.getUserId());
        return ids == null ? Collections.emptyList() : ids.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }
}
