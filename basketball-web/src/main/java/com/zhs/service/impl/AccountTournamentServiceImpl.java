package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTournamentDao;
import com.zhs.dao.BasketTournamentRegistrationDao;
import com.zhs.dao.BasketTournamentRegistrationPlayerDao;
import com.zhs.dao.BasketTournamentSeasonDao;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTournament;
import com.zhs.model.BasketTournamentRegistration;
import com.zhs.model.BasketTournamentRegistrationPlayer;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.response.account.AccountTournamentItemResp;
import com.zhs.service.IAccountTournamentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccountTournamentServiceImpl implements IAccountTournamentService {

    private static final Integer NOT_DELETED = 0;

    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketTournamentRegistrationPlayerDao registrationPlayerDao;
    @Resource
    private BasketTournamentRegistrationDao registrationDao;
    @Resource
    private BasketTournamentSeasonDao seasonDao;
    @Resource
    private BasketTournamentDao tournamentDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketFileAssetDao fileAssetDao;

    @Override
    public IPage<AccountTournamentItemResp> pageTournaments(Integer currentPage, Integer pageSize,
                                                            Integer status, String keyword) {
        Long memberId = UserContext.getRequiredUserId();
        BasketPlayerProfile playerProfile = loadPlayerProfile(memberId);
        if (playerProfile == null) {
            return emptyPage(currentPage, pageSize);
        }

        List<AccountTournamentItemResp> allItems = buildAllTournamentItems(memberId, playerProfile);
        List<AccountTournamentItemResp> filtered = filterItems(allItems, status, keyword);
        return toPage(filtered, currentPage, pageSize);
    }

    @Override
    public AccountTournamentItemResp getTournamentDetail(Long registrationId) {
        if (registrationId == null) {
            throw new ServiceException("报名主键不能为空");
        }
        Long memberId = UserContext.getRequiredUserId();
        BasketPlayerProfile playerProfile = loadPlayerProfile(memberId);
        if (playerProfile == null) {
            throw new ServiceException("无权查看该报名信息");
        }

        BasketTournamentRegistrationPlayer roster = findOwnedRoster(registrationId, memberId, playerProfile.getId());
        if (roster == null) {
            throw new ServiceException("无权查看该报名信息");
        }

        TournamentContext context = loadTournamentContext(Collections.singletonList(roster));
        return buildTournamentItem(roster, context);
    }

    @Override
    public List<AccountTournamentItemResp> listRecentTournaments(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        Long memberId = UserContext.getRequiredUserId();
        BasketPlayerProfile playerProfile = loadPlayerProfile(memberId);
        if (playerProfile == null) {
            return Collections.emptyList();
        }

        List<AccountTournamentItemResp> allItems = buildAllTournamentItems(memberId, playerProfile);
        return allItems.stream().limit(limit).collect(Collectors.toList());
    }

    private List<AccountTournamentItemResp> buildAllTournamentItems(Long memberId, BasketPlayerProfile playerProfile) {
        List<BasketTournamentRegistrationPlayer> rosters = registrationPlayerDao.selectList(
                new LambdaQueryWrapper<BasketTournamentRegistrationPlayer>()
                        .eq(BasketTournamentRegistrationPlayer::getIsDelete, NOT_DELETED)
                        .and(wrapper -> wrapper
                                .eq(BasketTournamentRegistrationPlayer::getMemberId, memberId)
                                .or()
                                .eq(BasketTournamentRegistrationPlayer::getPlayerId, playerProfile.getId()))
                        .orderByDesc(BasketTournamentRegistrationPlayer::getCreateTime)
                        .orderByDesc(BasketTournamentRegistrationPlayer::getId));
        if (CollectionUtils.isEmpty(rosters)) {
            return Collections.emptyList();
        }

        TournamentContext context = loadTournamentContext(rosters);
        List<AccountTournamentItemResp> items = new ArrayList<>(rosters.size());
        for (BasketTournamentRegistrationPlayer roster : rosters) {
            items.add(buildTournamentItem(roster, context));
        }
        items.sort(Comparator
                .comparing(AccountTournamentItemResp::getSubmittedTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AccountTournamentItemResp::getRegistrationId, Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    private TournamentContext loadTournamentContext(List<BasketTournamentRegistrationPlayer> rosters) {
        Set<Long> registrationIds = rosters.stream()
                .map(BasketTournamentRegistrationPlayer::getRegistrationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> seasonIds = rosters.stream()
                .map(BasketTournamentRegistrationPlayer::getSeasonId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> teamIds = rosters.stream()
                .map(BasketTournamentRegistrationPlayer::getTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, BasketTournamentRegistration> registrationMap = loadRegistrationMap(registrationIds);
        Map<Long, BasketTournamentSeason> seasonMap = loadSeasonMap(seasonIds);
        Map<Long, BasketTournament> tournamentMap = loadTournamentMap(seasonMap.values());
        Map<Long, BasketTeam> teamMap = loadTeamMap(teamIds);
        Map<Long, String> logoUrlMap = loadLogoUrlMap(teamMap.values());

        return new TournamentContext(registrationMap, seasonMap, tournamentMap, teamMap, logoUrlMap);
    }

    private Map<Long, BasketTournamentRegistration> loadRegistrationMap(Set<Long> registrationIds) {
        if (registrationIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketTournamentRegistration> registrations = registrationDao.selectBatchIds(registrationIds);
        Map<Long, BasketTournamentRegistration> map = new HashMap<>();
        if (CollectionUtils.isEmpty(registrations)) {
            return map;
        }
        for (BasketTournamentRegistration registration : registrations) {
            if (registration != null && NOT_DELETED.equals(registration.getIsDelete())) {
                map.put(registration.getId(), registration);
            }
        }
        return map;
    }

    private Map<Long, BasketTournamentSeason> loadSeasonMap(Set<Long> seasonIds) {
        if (seasonIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketTournamentSeason> seasons = seasonDao.selectBatchIds(seasonIds);
        Map<Long, BasketTournamentSeason> map = new HashMap<>();
        if (CollectionUtils.isEmpty(seasons)) {
            return map;
        }
        for (BasketTournamentSeason season : seasons) {
            if (season != null && NOT_DELETED.equals(season.getIsDelete())) {
                map.put(season.getId(), season);
            }
        }
        return map;
    }

    private Map<Long, BasketTournament> loadTournamentMap(java.util.Collection<BasketTournamentSeason> seasons) {
        Set<Long> tournamentIds = seasons.stream()
                .map(BasketTournamentSeason::getTournamentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (tournamentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketTournament> tournaments = tournamentDao.selectBatchIds(tournamentIds);
        Map<Long, BasketTournament> map = new HashMap<>();
        if (CollectionUtils.isEmpty(tournaments)) {
            return map;
        }
        for (BasketTournament tournament : tournaments) {
            if (tournament != null && NOT_DELETED.equals(tournament.getIsDelete())) {
                map.put(tournament.getId(), tournament);
            }
        }
        return map;
    }

    private Map<Long, BasketTeam> loadTeamMap(Set<Long> teamIds) {
        if (teamIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketTeam> teams = teamDao.selectBatchIds(teamIds);
        Map<Long, BasketTeam> map = new HashMap<>();
        if (CollectionUtils.isEmpty(teams)) {
            return map;
        }
        for (BasketTeam team : teams) {
            if (team != null && NOT_DELETED.equals(team.getIsDelete())) {
                map.put(team.getId(), team);
            }
        }
        return map;
    }

    private Map<Long, String> loadLogoUrlMap(java.util.Collection<BasketTeam> teams) {
        Set<Long> logoFileIds = teams.stream()
                .map(BasketTeam::getLogoFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (logoFileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketFileAsset> assets = fileAssetDao.selectBatchIds(logoFileIds);
        Map<Long, String> map = new HashMap<>();
        if (CollectionUtils.isEmpty(assets)) {
            return map;
        }
        for (BasketFileAsset asset : assets) {
            if (asset != null && NOT_DELETED.equals(asset.getIsDelete())) {
                map.put(asset.getId(), asset.getAccessUrl());
            }
        }
        return map;
    }

    private AccountTournamentItemResp buildTournamentItem(BasketTournamentRegistrationPlayer roster,
                                                          TournamentContext context) {
        AccountTournamentItemResp resp = new AccountTournamentItemResp();
        resp.setRegistrationId(toStringId(roster.getRegistrationId()));
        resp.setSeasonId(toStringId(roster.getSeasonId()));
        resp.setTeamId(toStringId(roster.getTeamId()));
        resp.setRosterRole(roster.getRosterRole());
        resp.setIsStarting(roster.getIsStarting());
        resp.setValidationStatus(roster.getValidationStatus());
        resp.setNextMatch(null);

        BasketTournamentRegistration registration = context.registrationMap().get(roster.getRegistrationId());
        if (registration != null) {
            resp.setRegistrationNo(registration.getRegistrationNo());
            resp.setRegistrationStatus(registration.getRegistrationStatus());
            resp.setSubmittedTime(registration.getSubmittedTime());
        }

        BasketTournamentSeason season = context.seasonMap().get(roster.getSeasonId());
        if (season != null) {
            resp.setSeasonName(season.getSeasonName());
            resp.setTournamentStartTime(season.getSeasonStartTime());
            resp.setTournamentEndTime(season.getSeasonEndTime());
            BasketTournament tournament = context.tournamentMap().get(season.getTournamentId());
            if (tournament != null) {
                resp.setTournamentId(String.valueOf(tournament.getId()));
                resp.setTournamentName(tournament.getTournamentName());
            }
        }

        BasketTeam team = context.teamMap().get(roster.getTeamId());
        if (team != null) {
            resp.setTeamName(team.getTeamName());
            if (team.getLogoFileId() != null) {
                resp.setTeamLogoUrl(context.logoUrlMap().get(team.getLogoFileId()));
            }
        }
        return resp;
    }

    private List<AccountTournamentItemResp> filterItems(List<AccountTournamentItemResp> items,
                                                          Integer status, String keyword) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return items.stream()
                .filter(item -> status == null || Objects.equals(item.getRegistrationStatus(), status))
                .filter(item -> {
                    if (normalizedKeyword == null) {
                        return true;
                    }
                    return containsIgnoreCase(item.getRegistrationNo(), normalizedKeyword)
                            || containsIgnoreCase(item.getTournamentName(), normalizedKeyword)
                            || containsIgnoreCase(item.getSeasonName(), normalizedKeyword)
                            || containsIgnoreCase(item.getTeamName(), normalizedKeyword);
                })
                .collect(Collectors.toList());
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return StringUtils.hasText(value) && value.toLowerCase().contains(keyword);
    }

    private BasketPlayerProfile loadPlayerProfile(Long memberId) {
        return playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private BasketTournamentRegistrationPlayer findOwnedRoster(Long registrationId, Long memberId, Long playerId) {
        return registrationPlayerDao.selectOne(new LambdaQueryWrapper<BasketTournamentRegistrationPlayer>()
                .eq(BasketTournamentRegistrationPlayer::getRegistrationId, registrationId)
                .eq(BasketTournamentRegistrationPlayer::getIsDelete, NOT_DELETED)
                .and(wrapper -> wrapper
                        .eq(BasketTournamentRegistrationPlayer::getMemberId, memberId)
                        .or()
                        .eq(BasketTournamentRegistrationPlayer::getPlayerId, playerId))
                .last("limit 1"));
    }

    private IPage<AccountTournamentItemResp> emptyPage(Integer currentPage, Integer pageSize) {
        return toPage(Collections.emptyList(), currentPage, pageSize);
    }

    private IPage<AccountTournamentItemResp> toPage(List<AccountTournamentItemResp> items,
                                                    Integer currentPage, Integer pageSize) {
        long page = normalizePage(currentPage);
        long size = normalizeSize(pageSize);
        long total = items.size();
        int fromIndex = (int) Math.min((page - 1) * size, total);
        int toIndex = (int) Math.min(fromIndex + size, total);
        List<AccountTournamentItemResp> pageRecords = fromIndex >= toIndex
                ? Collections.emptyList()
                : items.subList(fromIndex, toIndex);

        Page<AccountTournamentItemResp> respPage = new Page<>(page, size, total);
        respPage.setRecords(pageRecords);
        return respPage;
    }

    private String toStringId(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private long normalizePage(Integer currentPage) {
        return currentPage == null || currentPage < 1 ? 1L : currentPage;
    }

    private long normalizeSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10L : pageSize;
    }

    private record TournamentContext(
            Map<Long, BasketTournamentRegistration> registrationMap,
            Map<Long, BasketTournamentSeason> seasonMap,
            Map<Long, BasketTournament> tournamentMap,
            Map<Long, BasketTeam> teamMap,
            Map<Long, String> logoUrlMap) {
    }
}
