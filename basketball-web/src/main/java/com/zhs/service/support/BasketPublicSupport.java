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
import com.zhs.dao.BasketTournamentDao;
import com.zhs.dao.BasketTournamentGroupDao;
import com.zhs.dao.BasketTournamentSeasonDao;
import com.zhs.dao.BasketTournamentStageDao;
import com.zhs.dao.BasketVenueDao;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketPrivacySetting;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTournament;
import com.zhs.model.BasketTournamentGroup;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.model.BasketTournamentStage;
import com.zhs.model.BasketVenue;
import com.zhs.util.MiniRedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 官网公开查询公共能力：可见性校验、文件 URL、短缓存、分页规范化。
 */
@Slf4j
@Component
public class BasketPublicSupport {

    @Resource
    private BasketTournamentDao tournamentDao;
    @Resource
    private BasketTournamentSeasonDao seasonDao;
    @Resource
    private BasketTournamentStageDao stageDao;
    @Resource
    private BasketTournamentGroupDao groupDao;
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

    public <T> T cacheGetOrLoad(String cacheSuffix, Supplier<T> loader) {
        BasketPublicCacheKey key = new BasketPublicCacheKey(cacheSuffix);
        try {
            T cached = miniRedisUtil.get(key);
            if (cached != null) {
                return cached;
            }
        } catch (Exception ex) {
            log.debug("public cache get ignored: {}", ex.getMessage());
        }
        T value = loader.get();
        if (value != null) {
            try {
                miniRedisUtil.set(key, value);
            } catch (Exception ex) {
                log.debug("public cache set ignored: {}", ex.getMessage());
            }
        }
        return value;
    }

    public BasketTournament requirePublicTournament(Long tournamentId) {
        if (tournamentId == null) {
            throw new ServiceException("赛事主键不能为空");
        }
        BasketTournament tournament = tournamentDao.selectById(tournamentId);
        if (tournament == null
                || !Objects.equals(tournament.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                || !Objects.equals(tournament.getPublicStatus(), BasketPublicConstants.PUBLIC)) {
            throw new ServiceException("赛事不存在或未公开");
        }
        return tournament;
    }

    public BasketTournamentSeason requirePublicSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("赛季主键不能为空");
        }
        BasketTournamentSeason season = seasonDao.selectById(seasonId);
        if (season == null
                || !Objects.equals(season.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                || !Objects.equals(season.getPublicStatus(), BasketPublicConstants.PUBLIC)) {
            throw new ServiceException("赛季不存在或未公开");
        }
        BasketTournament tournament = tournamentDao.selectById(season.getTournamentId());
        if (tournament == null
                || !Objects.equals(tournament.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                || !Objects.equals(tournament.getPublicStatus(), BasketPublicConstants.PUBLIC)) {
            throw new ServiceException("赛季所属赛事未公开");
        }
        return season;
    }

    public BasketTournamentStage requirePublicStage(Long stageId) {
        if (stageId == null) {
            throw new ServiceException("阶段主键不能为空");
        }
        BasketTournamentStage stage = stageDao.selectById(stageId);
        if (stage == null
                || !Objects.equals(stage.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                || !Objects.equals(stage.getPublicStatus(), BasketPublicConstants.PUBLIC)) {
            throw new ServiceException("阶段不存在或未公开");
        }
        requirePublicSeason(stage.getSeasonId());
        return stage;
    }

    public BasketTournamentGroup requirePublicGroup(Long groupId) {
        if (groupId == null) {
            throw new ServiceException("分组主键不能为空");
        }
        BasketTournamentGroup group = groupDao.selectById(groupId);
        if (group == null || !Objects.equals(group.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
            throw new ServiceException("分组不存在");
        }
        requirePublicSeason(group.getSeasonId());
        return group;
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
        List<BasketFileAsset> files = fileAssetDao.selectBatchIds(ids);
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
        List<BasketTeam> teams = teamDao.selectBatchIds(ids);
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
        List<BasketPlayerProfile> players = playerProfileDao.selectBatchIds(ids);
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
            Long fileId = memberAvatarFileMap.get(player.getMemberId());
            if (fileId != null) {
                playerAvatarMap.put(player.getId(), urlMap.get(fileId));
            }
        }
        return playerAvatarMap;
    }

    public Map<Long, BasketTournamentSeason> loadSeasonMap(Collection<Long> seasonIds) {
        if (CollectionUtils.isEmpty(seasonIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = seasonIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketTournamentSeason> seasons = seasonDao.selectBatchIds(ids);
        Map<Long, BasketTournamentSeason> map = new HashMap<>();
        if (CollectionUtils.isEmpty(seasons)) {
            return map;
        }
        for (BasketTournamentSeason season : seasons) {
            if (season != null
                    && Objects.equals(season.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                    && Objects.equals(season.getPublicStatus(), BasketPublicConstants.PUBLIC)) {
                map.put(season.getId(), season);
            }
        }
        return map;
    }

    public Map<Long, BasketTournament> loadTournamentMap(Collection<Long> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = tournamentIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketTournament> list = tournamentDao.selectBatchIds(ids);
        Map<Long, BasketTournament> map = new HashMap<>();
        if (CollectionUtils.isEmpty(list)) {
            return map;
        }
        for (BasketTournament item : list) {
            if (item != null
                    && Objects.equals(item.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                    && Objects.equals(item.getPublicStatus(), BasketPublicConstants.PUBLIC)) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    public Map<Long, BasketTournamentStage> loadStageMap(Collection<Long> stageIds) {
        if (CollectionUtils.isEmpty(stageIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = stageIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketTournamentStage> list = stageDao.selectBatchIds(ids);
        Map<Long, BasketTournamentStage> map = new HashMap<>();
        if (CollectionUtils.isEmpty(list)) {
            return map;
        }
        for (BasketTournamentStage item : list) {
            if (item != null && Objects.equals(item.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    public Map<Long, BasketTournamentGroup> loadGroupMap(Collection<Long> groupIds) {
        if (CollectionUtils.isEmpty(groupIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = groupIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketTournamentGroup> list = groupDao.selectBatchIds(ids);
        Map<Long, BasketTournamentGroup> map = new HashMap<>();
        if (CollectionUtils.isEmpty(list)) {
            return map;
        }
        for (BasketTournamentGroup item : list) {
            if (item != null && Objects.equals(item.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
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
        List<BasketVenue> list = venueDao.selectBatchIds(ids);
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

    public List<BasketTournamentSeason> listPublicSeasons() {
        return seasonDao.selectList(new LambdaQueryWrapper<BasketTournamentSeason>()
                .eq(BasketTournamentSeason::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketTournamentSeason::getPublicStatus, BasketPublicConstants.PUBLIC)
                .orderByDesc(BasketTournamentSeason::getSeasonStartTime)
                .orderByDesc(BasketTournamentSeason::getId));
    }

    public BasketTournamentSeason findCurrentPublicSeason() {
        List<BasketTournamentSeason> seasons = seasonDao.selectList(new LambdaQueryWrapper<BasketTournamentSeason>()
                .eq(BasketTournamentSeason::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketTournamentSeason::getPublicStatus, BasketPublicConstants.PUBLIC)
                .orderByDesc(BasketTournamentSeason::getSeasonStatus)
                .orderByDesc(BasketTournamentSeason::getSeasonStartTime)
                .orderByDesc(BasketTournamentSeason::getId)
                .last("limit 20"));
        if (CollectionUtils.isEmpty(seasons)) {
            return null;
        }
        for (BasketTournamentSeason season : seasons) {
            if (Objects.equals(season.getSeasonStatus(), 2)) {
                return season;
            }
        }
        return seasons.get(0);
    }
}
