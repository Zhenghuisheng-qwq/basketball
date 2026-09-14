package com.zhs.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.cachekey.publicapi.BasketPublicCacheKey;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketPrivacySettingDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketVenueDao;
import com.zhs.dao.v4.BasketSeasonDao;
import com.zhs.dao.v4.BasketSeriesDao;
import com.zhs.enums.v4.PublishStatusEnum;
import com.zhs.enums.v4.SeasonStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketPrivacySetting;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketVenue;
import com.zhs.model.v4.BasketSeason;
import com.zhs.model.v4.BasketSeries;
import com.zhs.support.MemberAvatarSupport;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.zhs.util.MiniRedisUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToLongFunction;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 官网公开查询公共能力：可见性校验、文件 URL、短缓存、分页规范化。
 * 赛季/系列仅使用 V4 主线 basket_season / basket_series。
 */
@Slf4j
@Component
public class BasketPublicSupport {

    @Resource
    private BasketSeasonDao seasonDao;
    @Resource
    private BasketSeriesDao seriesDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private BasketPrivacySettingDao privacySettingDao;
    @Resource
    private BasketFileAssetDao fileAssetDao;
    @Resource
    private BasketVenueDao venueDao;
    @Resource
    private MiniRedisUtil miniRedisUtil;
    @Resource
    private ObjectMapper objectMapper;

    private ObjectMapper cacheMapper;

    public static final long LIVE_TTL_SECONDS = 15L;
    public static final long SLOW_TTL_SECONDS = 300L;
    public static final long FINALIZED_TTL_SECONDS = 604_800L;
    public static final long ARCHIVED_TTL_SECONDS = 2_592_000L;

    @PostConstruct
    void initCacheMapper() {
        cacheMapper = objectMapper.copy();
        cacheMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.WRAPPER_ARRAY);
    }

    public int normalizePage(Integer currentPage) {
        return currentPage == null || currentPage < 1 ? BasketPublicConstants.DEFAULT_PAGE : currentPage;
    }

    public int normalizeSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return BasketPublicConstants.DEFAULT_SIZE;
        }
        return Math.min(pageSize, BasketPublicConstants.MAX_SIZE);
    }

    public <T> Page<T> newPage(Integer currentPage, Integer pageSize) {
        return new Page<>(normalizePage(currentPage), normalizeSize(pageSize));
    }

    public <T> IPage<T> emptyPage(Integer currentPage, Integer pageSize) {
        return new Page<>(normalizePage(currentPage), normalizeSize(pageSize), 0);
    }

    public boolean isAsc(String sortOrder) {
        return sortOrder != null && "asc".equalsIgnoreCase(sortOrder.trim());
    }

    private static final long CACHE_BREAKER_MS = 30_000L;
    private volatile long cacheDisabledUntilMs;

    public long liveTtlSeconds() {
        return LIVE_TTL_SECONDS;
    }

    public long slowTtlSeconds() {
        return SLOW_TTL_SECONDS + ThreadLocalRandom.current().nextLong(60);
    }

    public long finalizedTtlSeconds() {
        return FINALIZED_TTL_SECONDS + ThreadLocalRandom.current().nextLong(3_600);
    }

    public long archivedTtlSeconds() {
        return ARCHIVED_TTL_SECONDS + ThreadLocalRandom.current().nextLong(3_600);
    }

    public <T> T cacheGetOrLoad(String cacheSuffix, Supplier<T> loader) {
        return cacheGetOrLoad(cacheSuffix, BasketPublicCacheKey.DEFAULT_SECONDS, loader);
    }

    public <T> T cacheGetOrLoad(String cacheSuffix, long expireSeconds, Supplier<T> loader) {
        return cacheGetOrLoad(cacheSuffix, loader, value -> expireSeconds);
    }

    public <T> T cacheGetOrLoad(String cacheSuffix, Supplier<T> loader, ToLongFunction<T> ttlResolver) {
        long now = System.currentTimeMillis();
        if (now < cacheDisabledUntilMs) {
            return loader.get();
        }
        BasketPublicCacheKey peekKey = new BasketPublicCacheKey(cacheSuffix);
        try {
            Object raw = miniRedisUtil.get(peekKey);
            if (raw instanceof String json && !json.isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    T cached = (T) cacheMapper.readValue(json, Object.class);
                    if (cached != null) {
                        return cached;
                    }
                } catch (Exception deserializeEx) {
                    log.warn("public cache value ignored, rebuild: {}", deserializeEx.getMessage());
                    try {
                        miniRedisUtil.delete(peekKey);
                    } catch (Exception ignored) {
                        // ignore broken-key delete
                    }
                }
            }
        } catch (Exception ex) {
            if (isRedisInfraFailure(ex)) {
                cacheDisabledUntilMs = System.currentTimeMillis() + CACHE_BREAKER_MS;
            }
            log.debug("public cache get ignored: {}", ex.getMessage());
        }
        T value = loader.get();
        if (value != null && System.currentTimeMillis() >= cacheDisabledUntilMs) {
            long ttl = ttlResolver == null ? LIVE_TTL_SECONDS : Math.max(1L, ttlResolver.applyAsLong(value));
            BasketPublicCacheKey key = new BasketPublicCacheKey(cacheSuffix, ttl);
            try {
                miniRedisUtil.set(key, cacheMapper.writeValueAsString(value));
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                log.debug("public cache serialize ignored: {}", ex.getMessage());
            } catch (Exception ex) {
                if (isRedisInfraFailure(ex)) {
                    cacheDisabledUntilMs = System.currentTimeMillis() + CACHE_BREAKER_MS;
                }
                log.debug("public cache set ignored: {}", ex.getMessage());
            }
        }
        return value;
    }

    static boolean isRedisInfraFailure(Throwable ex) {
        Throwable cur = ex;
        while (cur != null) {
            String name = cur.getClass().getName();
            if (name.contains("RedisConnectionFailure")
                    || name.contains("RedisSystemException")
                    || name.contains("QueryTimeoutException")
                    || name.contains("RedisCommandTimeout")
                    || cur instanceof java.net.ConnectException
                    || cur instanceof java.net.SocketTimeoutException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    public void evictPublicCache(String cacheSuffix) {
        try {
            miniRedisUtil.delete(new BasketPublicCacheKey(cacheSuffix));
        } catch (Exception ex) {
            log.debug("public cache evict ignored: {}", ex.getMessage());
        }
    }

    public void evictPublicCachePrefix(String suffixPrefix) {
        try {
            miniRedisUtil.deleteByPattern(BasketPublicCacheKey.PREFIX + suffixPrefix + "*");
        } catch (Exception ex) {
            log.debug("public cache prefix evict ignored: {}", ex.getMessage());
        }
    }

    public BasketSeason requirePublicSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("赛季主键不能为空");
        }
        BasketSeason season = seasonDao.selectById(seasonId);
        if (season == null
                || !Objects.equals(season.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                || !PublishStatusEnum.PUBLISHED.getCode().equals(season.getPublishStatus())) {
            throw new ServiceException("赛季不存在或未公开");
        }
        BasketSeries series = seriesDao.selectById(season.getSeriesId());
        if (series == null
                || !Objects.equals(series.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                || !Objects.equals(series.getEnabled(), 1)) {
            throw new ServiceException("赛季所属系列未公开");
        }
        return season;
    }

    public BasketTeam requirePublicTeam(Long teamId) {
        if (teamId == null) {
            throw new ServiceException("球队主键不能为空");
        }
        BasketTeam team = teamDao.selectById(teamId);
        if (team == null
                || !Objects.equals(team.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                || !Objects.equals(team.getPublicStatus(), BasketPublicConstants.PUBLIC)
                || !Objects.equals(team.getAuditStatus(), BasketPublicConstants.TEAM_AUDIT_APPROVED)) {
            throw new ServiceException("球队不存在或未公开");
        }
        return team;
    }

    public BasketPlayerProfile requirePlayer(Long playerId) {
        if (playerId == null) {
            throw new ServiceException("球员主键不能为空");
        }
        BasketPlayerProfile player = playerProfileDao.selectById(playerId);
        if (player == null || !Objects.equals(player.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
            throw new ServiceException("球员不存在");
        }
        return player;
    }

    public boolean isPlayerPubliclyVisible(BasketPlayerProfile player) {
        if (player == null) {
            return false;
        }
        if (!Objects.equals(player.getProfileVisible(), BasketPublicConstants.PROFILE_VISIBLE)) {
            return false;
        }
        BasketPrivacySetting privacy = findPrivacy(player.getMemberId());
        if (privacy != null && !Objects.equals(privacy.getProfileVisible(), BasketPublicConstants.PROFILE_VISIBLE)) {
            return false;
        }
        return true;
    }

    public boolean canShowMatchData(BasketPlayerProfile player) {
        if (!isPlayerPubliclyVisible(player)) {
            return false;
        }
        BasketPrivacySetting privacy = findPrivacy(player.getMemberId());
        if (privacy == null) {
            return true;
        }
        return privacy.getShowMatchData() == null || Objects.equals(privacy.getShowMatchData(), BasketPublicConstants.PROFILE_VISIBLE);
    }

    public boolean canShowTeamHistory(BasketPlayerProfile player) {
        if (!isPlayerPubliclyVisible(player)) {
            return false;
        }
        BasketPrivacySetting privacy = findPrivacy(player.getMemberId());
        if (privacy == null) {
            return true;
        }
        return privacy.getShowTeamHistory() == null || Objects.equals(privacy.getShowTeamHistory(), BasketPublicConstants.PROFILE_VISIBLE);
    }

    public BasketPrivacySetting findPrivacy(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return privacySettingDao.selectOne(new LambdaQueryWrapper<BasketPrivacySetting>()
                .eq(BasketPrivacySetting::getMemberId, memberId)
                .eq(BasketPrivacySetting::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .last("limit 1"));
    }

    public String resolveDisplayName(BasketPlayerProfile player) {
        if (player == null) {
            return null;
        }
        if (StringUtils.hasText(player.getGameNickname())) {
            return player.getGameNickname();
        }
        return null;
    }

    public Map<Long, String> loadAccessUrlMap(Collection<Long> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = fileIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketFileAsset> files = fileAssetDao.selectByIds(ids);
        Map<Long, String> map = new HashMap<>();
        if (CollectionUtils.isEmpty(files)) {
            return map;
        }
        for (BasketFileAsset file : files) {
            if (file != null
                    && Objects.equals(file.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                    && StringUtils.hasText(file.getAccessUrl())) {
                map.put(file.getId(), file.getAccessUrl());
            }
        }
        return map;
    }

    public Map<Long, BasketTeam> loadPublicTeamMap(Collection<Long> teamIds) {
        if (CollectionUtils.isEmpty(teamIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = teamIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketTeam> teams = teamDao.selectByIds(ids);
        Map<Long, BasketTeam> map = new HashMap<>();
        if (CollectionUtils.isEmpty(teams)) {
            return map;
        }
        for (BasketTeam team : teams) {
            if (team != null
                    && Objects.equals(team.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                    && Objects.equals(team.getPublicStatus(), BasketPublicConstants.PUBLIC)
                    && Objects.equals(team.getAuditStatus(), BasketPublicConstants.TEAM_AUDIT_APPROVED)) {
                map.put(team.getId(), team);
            }
        }
        return map;
    }

    public Map<Long, BasketPlayerProfile> loadPlayerMap(Collection<Long> playerIds) {
        if (CollectionUtils.isEmpty(playerIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = playerIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketPlayerProfile> players = playerProfileDao.selectByIds(ids);
        Map<Long, BasketPlayerProfile> map = new HashMap<>();
        if (CollectionUtils.isEmpty(players)) {
            return map;
        }
        for (BasketPlayerProfile player : players) {
            if (player != null && Objects.equals(player.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                map.put(player.getId(), player);
            }
        }
        return map;
    }

    public Map<Long, String> loadPlayerAvatarUrlMap(Collection<BasketPlayerProfile> players) {
        if (CollectionUtils.isEmpty(players)) {
            return Collections.emptyMap();
        }
        Set<Long> memberIds = players.stream()
                .map(BasketPlayerProfile::getMemberId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketMemberProfile> profiles = memberProfileDao.selectList(new LambdaQueryWrapper<BasketMemberProfile>()
                .in(BasketMemberProfile::getMemberId, memberIds)
                .eq(BasketMemberProfile::getIsDelete, BasketPublicConstants.NOT_DELETED));
        if (CollectionUtils.isEmpty(profiles)) {
            return Collections.emptyMap();
        }
        Map<Long, Long> memberAvatarFileMap = new HashMap<>();
        for (BasketMemberProfile profile : profiles) {
            if (profile.getAvatarFileId() != null) {
                memberAvatarFileMap.put(profile.getMemberId(), profile.getAvatarFileId());
            }
        }
        Map<Long, String> urlMap = loadAccessUrlMap(memberAvatarFileMap.values());
        Map<Long, String> playerAvatarMap = new HashMap<>();
        for (BasketPlayerProfile player : players) {
            if (player == null || player.getId() == null) {
                continue;
            }
            Long fileId = memberAvatarFileMap.get(player.getMemberId());
            String url = fileId != null ? urlMap.get(fileId) : null;
            playerAvatarMap.put(player.getId(), MemberAvatarSupport.resolve(url));
        }
        return playerAvatarMap;
    }

    public Map<Long, BasketSeason> loadSeasonMap(Collection<Long> seasonIds) {
        if (CollectionUtils.isEmpty(seasonIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = seasonIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketSeason> seasons = seasonDao.selectByIds(ids);
        Map<Long, BasketSeason> map = new HashMap<>();
        if (CollectionUtils.isEmpty(seasons)) {
            return map;
        }
        for (BasketSeason season : seasons) {
            if (season != null
                    && Objects.equals(season.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                    && PublishStatusEnum.PUBLISHED.getCode().equals(season.getPublishStatus())) {
                map.put(season.getId(), season);
            }
        }
        return map;
    }

    public Map<Long, BasketSeries> loadSeriesMap(Collection<Long> seriesIds) {
        if (CollectionUtils.isEmpty(seriesIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = seriesIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketSeries> list = seriesDao.selectByIds(ids);
        Map<Long, BasketSeries> map = new HashMap<>();
        if (CollectionUtils.isEmpty(list)) {
            return map;
        }
        for (BasketSeries item : list) {
            if (item != null
                    && Objects.equals(item.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                    && Objects.equals(item.getEnabled(), 1)) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    public Map<Long, BasketVenue> loadVenueMap(Collection<Long> venueIds) {
        if (CollectionUtils.isEmpty(venueIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = venueIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketVenue> list = venueDao.selectByIds(ids);
        Map<Long, BasketVenue> map = new HashMap<>();
        if (CollectionUtils.isEmpty(list)) {
            return map;
        }
        for (BasketVenue item : list) {
            if (item != null && Objects.equals(item.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    public List<BasketSeason> listPublicSeasons() {
        return seasonDao.selectList(new LambdaQueryWrapper<BasketSeason>()
                .eq(BasketSeason::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketSeason::getPublishStatus, PublishStatusEnum.PUBLISHED.getCode())
                .orderByDesc(BasketSeason::getMatchStartTime)
                .orderByDesc(BasketSeason::getId));
    }

    public BasketSeason findCurrentPublicSeason() {
        List<BasketSeason> seasons = seasonDao.selectList(new LambdaQueryWrapper<BasketSeason>()
                .eq(BasketSeason::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketSeason::getPublishStatus, PublishStatusEnum.PUBLISHED.getCode())
                .orderByDesc(BasketSeason::getSeasonStatus)
                .orderByDesc(BasketSeason::getMatchStartTime)
                .orderByDesc(BasketSeason::getId)
                .last("limit 20"));
        if (CollectionUtils.isEmpty(seasons)) {
            return null;
        }
        for (BasketSeason season : seasons) {
            if (SeasonStatusEnum.PLAYING.getCode().equals(season.getSeasonStatus())) {
                return season;
            }
        }
        return seasons.get(0);
    }

    public Set<Long> findPublicTeamIdsByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptySet();
        }
        String kw = keyword.trim();
        List<BasketTeam> teams = teamDao.selectList(new LambdaQueryWrapper<BasketTeam>()
                .eq(BasketTeam::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketTeam::getPublicStatus, BasketPublicConstants.PUBLIC)
                .eq(BasketTeam::getAuditStatus, BasketPublicConstants.TEAM_AUDIT_APPROVED)
                .and(w -> w.like(BasketTeam::getTeamName, kw)
                        .or().like(BasketTeam::getShortName, kw)
                        .or().like(BasketTeam::getTeamNo, kw)));
        if (CollectionUtils.isEmpty(teams)) {
            return Collections.emptySet();
        }
        return teams.stream()
                .map(BasketTeam::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Set<Long> findPublicPlayerIdsByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptySet();
        }
        String kw = keyword.trim();
        Set<Long> playerIds = new HashSet<>();
        List<BasketPlayerProfile> players = playerProfileDao.selectList(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .and(w -> w.like(BasketPlayerProfile::getGameNickname, kw)
                        .or().like(BasketPlayerProfile::getPlayerNo, kw)
                        .or().like(BasketPlayerProfile::getGameAccount, kw)));
        if (!CollectionUtils.isEmpty(players)) {
            for (BasketPlayerProfile player : players) {
                if (player != null && player.getId() != null && isPlayerPubliclyVisible(player)) {
                    playerIds.add(player.getId());
                }
            }
        }
        List<BasketMemberProfile> members = memberProfileDao.selectList(new LambdaQueryWrapper<BasketMemberProfile>()
                .eq(BasketMemberProfile::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .like(BasketMemberProfile::getNickname, kw));
        if (!CollectionUtils.isEmpty(members)) {
            Set<Long> memberIds = members.stream()
                    .map(BasketMemberProfile::getMemberId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!memberIds.isEmpty()) {
                List<BasketPlayerProfile> linkedPlayers = playerProfileDao.selectList(new LambdaQueryWrapper<BasketPlayerProfile>()
                        .in(BasketPlayerProfile::getMemberId, memberIds)
                        .eq(BasketPlayerProfile::getIsDelete, BasketPublicConstants.NOT_DELETED));
                if (!CollectionUtils.isEmpty(linkedPlayers)) {
                    for (BasketPlayerProfile player : linkedPlayers) {
                        if (player != null && player.getId() != null && isPlayerPubliclyVisible(player)) {
                            playerIds.add(player.getId());
                        }
                    }
                }
            }
        }
        return playerIds;
    }
}
