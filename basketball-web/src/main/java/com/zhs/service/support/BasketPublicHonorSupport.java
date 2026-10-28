package com.zhs.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.dao.BasketHonorCategoryDao;
import com.zhs.dao.BasketHonorLineupDao;
import com.zhs.dao.BasketHonorRecordDao;
import com.zhs.dao.v4.BasketEventPlayerDao;
import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.enums.PublicHonorSortEnum;
import com.zhs.model.BasketHonorCategory;
import com.zhs.model.BasketHonorLineup;
import com.zhs.model.BasketHonorRecord;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.v4.BasketEventPlayer;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketSeason;
import com.zhs.model.v4.BasketSeries;
import com.zhs.response.publicapi.PublicHonorDetailResp;
import com.zhs.response.publicapi.PublicHonorItemResp;
import com.zhs.response.publicapi.PublicHonorLineupItemResp;
import com.zhs.support.MemberAvatarSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 公开荣誉查询与组装。
 */
@Component
public class BasketPublicHonorSupport {

    @Resource
    private BasketHonorRecordDao honorRecordDao;
    @Resource
    private BasketHonorCategoryDao honorCategoryDao;
    @Resource
    private BasketHonorLineupDao honorLineupDao;
    @Resource
    private BasketPublicSupport publicSupport;
    @Resource
    private BasketEventTeamDao eventTeamDao;
    @Resource
    private BasketEventPlayerDao eventPlayerDao;

    public LambdaQueryWrapper<BasketHonorRecord> publishedWrapper() {
        return new LambdaQueryWrapper<BasketHonorRecord>()
                .eq(BasketHonorRecord::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketHonorRecord::getDisplayStatus, BasketPublicConstants.PUBLISHED);
    }

    public Map<Long, BasketHonorCategory> loadCategoryMap(Set<Long> categoryIds) {
        if (CollectionUtils.isEmpty(categoryIds)) {
            return Collections.emptyMap();
        }
        List<BasketHonorCategory> list = honorCategoryDao.selectByIds(categoryIds);
        Map<Long, BasketHonorCategory> map = new HashMap<>();
        if (CollectionUtils.isEmpty(list)) {
            return map;
        }
        for (BasketHonorCategory item : list) {
            if (item != null && Objects.equals(item.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    public Set<Long> categoryIdsByScope(String honorScope) {
        if (!StringUtils.hasText(honorScope)) {
            return Collections.emptySet();
        }
        List<BasketHonorCategory> list = honorCategoryDao.selectList(new LambdaQueryWrapper<BasketHonorCategory>()
                .eq(BasketHonorCategory::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketHonorCategory::getHonorScope, honorScope.trim().toUpperCase())
                .select(BasketHonorCategory::getId));
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptySet();
        }
        return list.stream().map(BasketHonorCategory::getId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public BasketHonorCategory findCategoryByCode(String honorCode) {
        if (!StringUtils.hasText(honorCode)) {
            return null;
        }
        return honorCategoryDao.selectOne(new LambdaQueryWrapper<BasketHonorCategory>()
                .eq(BasketHonorCategory::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketHonorCategory::getHonorCode, honorCode.trim().toUpperCase())
                .last("limit 1"));
    }

    public void applyHonorSort(LambdaQueryWrapper<BasketHonorRecord> wrapper, String sortField, String sortOrder) {
        PublicHonorSortEnum sortEnum = PublicHonorSortEnum.fromCode(sortField);
        boolean asc = publicSupport.isAsc(sortOrder);
        if (sortEnum == PublicHonorSortEnum.AWARD_DATE) {
            if (asc) {
                wrapper.orderByAsc(BasketHonorRecord::getAwardDate);
            } else {
                wrapper.orderByDesc(BasketHonorRecord::getAwardDate);
            }
            wrapper.orderByAsc(BasketHonorRecord::getDisplayOrder).orderByDesc(BasketHonorRecord::getId);
            return;
        }
        // default DISPLAY_ORDER
        if (asc) {
            wrapper.orderByAsc(BasketHonorRecord::getDisplayOrder);
        } else {
            wrapper.orderByDesc(BasketHonorRecord::getDisplayOrder);
        }
        wrapper.orderByDesc(BasketHonorRecord::getAwardDate).orderByDesc(BasketHonorRecord::getId);
    }

    public long countPublishedByScope(String honorScope) {
        Set<Long> categoryIds = categoryIdsByScope(honorScope);
        if (categoryIds.isEmpty()) {
            return 0L;
        }
        Long count = honorRecordDao.selectCount(publishedWrapper().in(BasketHonorRecord::getHonorCategoryId, categoryIds));
        return count == null ? 0L : count;
    }

    public List<BasketHonorRecord> listLatestByScope(String honorScope, int limit) {
        Set<Long> categoryIds = categoryIdsByScope(honorScope);
        if (categoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        int size = Math.min(Math.max(limit, 1), BasketPublicConstants.MAX_SIZE);
        return honorRecordDao.selectList(publishedWrapper()
                .in(BasketHonorRecord::getHonorCategoryId, categoryIds)
                .orderByAsc(BasketHonorRecord::getDisplayOrder)
                .orderByDesc(BasketHonorRecord::getAwardDate)
                .orderByDesc(BasketHonorRecord::getId)
                .last("limit " + size));
    }

    public IPage<PublicHonorItemResp> pageByScope(String honorScope, Integer currentPage, Integer pageSize,
                                                  Long tournamentId, Long seasonId, String keyword,
                                                  String sortField, String sortOrder) {
        Set<Long> categoryIds = categoryIdsByScope(honorScope);
        if (categoryIds.isEmpty()) {
            return publicSupport.emptyPage(currentPage, pageSize);
        }
        Page<BasketHonorRecord> page = publicSupport.newPage(currentPage, pageSize);
        LambdaQueryWrapper<BasketHonorRecord> wrapper = publishedWrapper()
                .in(BasketHonorRecord::getHonorCategoryId, categoryIds)
                .eq(tournamentId != null, BasketHonorRecord::getSeriesId, tournamentId)
                .eq(seasonId != null, BasketHonorRecord::getSeasonId, seasonId);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(BasketHonorRecord::getHonorTitle, kw)
                    .or().like(BasketHonorRecord::getHonorSubtitle, kw));
        }
        applyHonorSort(wrapper, sortField, sortOrder);
        IPage<BasketHonorRecord> entityPage = honorRecordDao.selectPage(page, wrapper);
        return toItemPage(entityPage, currentPage, pageSize);
    }

    public IPage<PublicHonorItemResp> pageByTeam(Long teamId, Integer currentPage, Integer pageSize) {
        Page<BasketHonorRecord> page = publicSupport.newPage(currentPage, pageSize);
        LambdaQueryWrapper<BasketHonorRecord> wrapper = publishedWrapper()
                .eq(BasketHonorRecord::getTeamId, teamId)
                .orderByAsc(BasketHonorRecord::getDisplayOrder)
                .orderByDesc(BasketHonorRecord::getAwardDate)
                .orderByDesc(BasketHonorRecord::getId);
        return toItemPage(honorRecordDao.selectPage(page, wrapper), currentPage, pageSize);
    }

    /**
     * 球员生涯荣誉：本人荣誉，加上其所在赛事球队已发布的球队荣誉（如冠军）。
     */
    public IPage<PublicHonorItemResp> pageByPlayerCareer(Long playerId, Integer currentPage, Integer pageSize) {
        Set<Long> teamKeys = teamKeysForPlayer(playerId);
        LambdaQueryWrapper<BasketHonorRecord> wrapper = publishedWrapper()
                .and(w -> {
                    w.eq(BasketHonorRecord::getPlayerId, playerId);
                    if (!teamKeys.isEmpty()) {
                        w.or(o -> o.in(BasketHonorRecord::getTeamId, teamKeys)
                                .and(inner -> inner.isNull(BasketHonorRecord::getPlayerId)
                                        .or()
                                        .eq(BasketHonorRecord::getPlayerId, playerId)));
                    }
                })
                .orderByDesc(BasketHonorRecord::getAwardDate)
                .orderByAsc(BasketHonorRecord::getDisplayOrder)
                .orderByDesc(BasketHonorRecord::getId);
        List<BasketHonorRecord> all = honorRecordDao.selectList(wrapper);
        int pageNo = currentPage == null || currentPage < 1 ? 1 : currentPage;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int from = Math.min((pageNo - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        Page<BasketHonorRecord> entityPage = new Page<>(pageNo, size, all.size());
        entityPage.setRecords(all.subList(from, to));
        return toItemPage(entityPage, pageNo, size);
    }

    public IPage<PublicHonorItemResp> pageByPlayer(Long playerId, Integer currentPage, Integer pageSize) {
        Page<BasketHonorRecord> page = publicSupport.newPage(currentPage, pageSize);
        LambdaQueryWrapper<BasketHonorRecord> wrapper = publishedWrapper()
                .eq(BasketHonorRecord::getPlayerId, playerId)
                .orderByAsc(BasketHonorRecord::getDisplayOrder)
                .orderByDesc(BasketHonorRecord::getAwardDate)
                .orderByDesc(BasketHonorRecord::getId);
        return toItemPage(honorRecordDao.selectPage(page, wrapper), currentPage, pageSize);
    }

    public List<PublicHonorItemResp> listBySeason(Long seasonId) {
        List<BasketHonorRecord> records = honorRecordDao.selectList(publishedWrapper()
                .eq(BasketHonorRecord::getSeasonId, seasonId)
                .orderByAsc(BasketHonorRecord::getDisplayOrder)
                .orderByDesc(BasketHonorRecord::getAwardDate)
                .orderByDesc(BasketHonorRecord::getId));
        return toItemList(records);
    }

    public BasketHonorRecord findPublishedChampionHonor(Long seasonId) {
        BasketHonorCategory champion = findCategoryByCode(BasketPublicConstants.HONOR_CODE_CHAMPION);
        if (champion == null || seasonId == null) {
            return null;
        }
        return honorRecordDao.selectOne(publishedWrapper()
                .eq(BasketHonorRecord::getSeasonId, seasonId)
                .eq(BasketHonorRecord::getHonorCategoryId, champion.getId())
                .orderByDesc(BasketHonorRecord::getId)
                .last("limit 1"));
    }

    public PublicHonorDetailResp getPublishedDetail(Long honorId) {
        if (honorId == null) {
            return null;
        }
        BasketHonorRecord record = honorRecordDao.selectById(honorId);
        if (record == null
                || !Objects.equals(record.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                || !Objects.equals(record.getDisplayStatus(), BasketPublicConstants.PUBLISHED)) {
            return null;
        }
        List<PublicHonorItemResp> items = toItemList(Collections.singletonList(record));
        if (items.isEmpty()) {
            return null;
        }
        PublicHonorDetailResp detail = new PublicHonorDetailResp();
        copyItem(items.get(0), detail);
        detail.setLineup(loadLineup(honorId));
        return detail;
    }

    public List<PublicHonorLineupItemResp> loadLineup(Long honorRecordId) {
        List<BasketHonorLineup> lineups = honorLineupDao.selectList(new LambdaQueryWrapper<BasketHonorLineup>()
                .eq(BasketHonorLineup::getHonorRecordId, honorRecordId)
                .eq(BasketHonorLineup::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketHonorLineup::getSortNo)
                .orderByAsc(BasketHonorLineup::getId));
        if (CollectionUtils.isEmpty(lineups)) {
            return Collections.emptyList();
        }
        Set<Long> playerIds = lineups.stream().map(BasketHonorLineup::getPlayerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketPlayerProfile> playerMap = publicSupport.loadPlayerMap(playerIds);
        List<BasketPlayerProfile> visiblePlayers = playerMap.values().stream()
                .filter(publicSupport::isPlayerPubliclyVisible)
                .collect(Collectors.toList());
        Map<Long, String> avatarMap = publicSupport.loadPlayerAvatarUrlMap(visiblePlayers);
        List<PublicHonorLineupItemResp> result = new ArrayList<>(lineups.size());
        for (BasketHonorLineup lineup : lineups) {
            PublicHonorLineupItemResp vo = new PublicHonorLineupItemResp();
            vo.setLineupId(lineup.getId());
            vo.setTeamId(lineup.getTeamId());
            vo.setPlayerId(lineup.getPlayerId());
            vo.setRoleType(lineup.getRoleType());
            vo.setJerseyNo(lineup.getJerseyNo());
            vo.setPositionCode(lineup.getPositionCode());
            vo.setSortNo(lineup.getSortNo());
            BasketPlayerProfile player = playerMap.get(lineup.getPlayerId());
            if (player != null && publicSupport.isPlayerPubliclyVisible(player)) {
                vo.setPlayerName(publicSupport.resolveDisplayName(player));
                vo.setAvatarUrl(MemberAvatarSupport.resolve(avatarMap.get(player.getId())));
            } else if (player != null) {
                // 隐私球员：保留球员主键但不暴露姓名头像
                vo.setPlayerName(null);
                vo.setAvatarUrl(null);
            }
            result.add(vo);
        }
        return result;
    }

    public IPage<PublicHonorItemResp> toItemPage(IPage<BasketHonorRecord> entityPage, Integer currentPage, Integer pageSize) {
        if (entityPage == null || CollectionUtils.isEmpty(entityPage.getRecords())) {
            return publicSupport.emptyPage(currentPage, pageSize);
        }
        Page<PublicHonorItemResp> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(toItemList(entityPage.getRecords()));
        return result;
    }

    public List<PublicHonorItemResp> toItemList(List<BasketHonorRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }
        Set<Long> categoryIds = new HashSet<>();
        Set<Long> seriesIds = new HashSet<>();
        Set<Long> seasonIds = new HashSet<>();
        Set<Long> teamIds = new HashSet<>();
        Set<Long> playerIds = new HashSet<>();
        Set<Long> fileIds = new HashSet<>();
        for (BasketHonorRecord record : records) {
            if (record.getHonorCategoryId() != null) {
                categoryIds.add(record.getHonorCategoryId());
            }
            if (record.getSeriesId() != null) {
                seriesIds.add(record.getSeriesId());
            }
            if (record.getSeasonId() != null) {
                seasonIds.add(record.getSeasonId());
            }
            if (record.getTeamId() != null) {
                teamIds.add(record.getTeamId());
            }
            if (record.getPlayerId() != null) {
                playerIds.add(record.getPlayerId());
            }
            if (record.getCoverFileId() != null) {
                fileIds.add(record.getCoverFileId());
            }
        }
        Map<Long, BasketHonorCategory> categoryMap = loadCategoryMap(categoryIds);
        Map<Long, BasketSeries> seriesMap = publicSupport.loadSeriesMap(seriesIds);
        Map<Long, BasketSeason> seasonMap = publicSupport.loadSeasonMap(seasonIds);
        Map<Long, BasketTeam> teamMap = publicSupport.loadTeamMap(teamIds);
        List<BasketEventTeam> eventTeams = loadEventTeams(teamIds);
        Map<Long, BasketEventTeam> eventById = new HashMap<>();
        Map<String, BasketEventTeam> eventBySourceSeason = new HashMap<>();
        Map<Long, BasketEventTeam> latestBySource = new HashMap<>();
        for (BasketEventTeam eventTeam : eventTeams) {
            eventById.put(eventTeam.getId(), eventTeam);
            if (eventTeam.getTeamLogoFileId() != null) {
                fileIds.add(eventTeam.getTeamLogoFileId());
            }
            if (eventTeam.getSourceTeamId() == null) {
                continue;
            }
            if (eventTeam.getSeasonId() != null) {
                String key = eventTeam.getSourceTeamId() + ":" + eventTeam.getSeasonId();
                eventBySourceSeason.merge(key, eventTeam, this::preferNewerEventTeam);
            }
            latestBySource.merge(eventTeam.getSourceTeamId(), eventTeam, this::preferNewerEventTeam);
        }
        for (BasketTeam team : teamMap.values()) {
            if (team.getLogoFileId() != null) {
                fileIds.add(team.getLogoFileId());
            }
        }
        Map<Long, BasketPlayerProfile> playerMap = publicSupport.loadPlayerMap(playerIds);
        List<BasketPlayerProfile> visiblePlayers = playerMap.values().stream()
                .filter(publicSupport::isPlayerPubliclyVisible)
                .collect(Collectors.toList());
        Map<Long, String> avatarMap = publicSupport.loadPlayerAvatarUrlMap(visiblePlayers);
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(fileIds);

        List<PublicHonorItemResp> result = new ArrayList<>(records.size());
        for (BasketHonorRecord record : records) {
            PublicHonorItemResp vo = new PublicHonorItemResp();
            vo.setHonorId(record.getId());
            vo.setTournamentId(record.getSeriesId());
            vo.setSeasonId(record.getSeasonId());
            vo.setTeamId(record.getTeamId());
            vo.setPlayerId(record.getPlayerId());
            vo.setHonorTitle(record.getHonorTitle());
            vo.setHonorTitleEn(record.getHonorTitleEn());
            vo.setHonorSubtitle(record.getHonorSubtitle());
            vo.setHonorSubtitleEn(record.getHonorSubtitleEn());
            vo.setHonorValue(record.getHonorValue());
            vo.setAwardDate(record.getAwardDate());
            vo.setDisplayOrder(record.getDisplayOrder());
            vo.setCoverUrl(urlMap.get(record.getCoverFileId()));

            BasketHonorCategory category = categoryMap.get(record.getHonorCategoryId());
            if (category != null) {
                vo.setHonorCode(category.getHonorCode());
                vo.setHonorName(category.getHonorName());
                vo.setHonorScope(category.getHonorScope());
                vo.setRecipientType(category.getRecipientType());
            }
            BasketSeries series = seriesMap.get(record.getSeriesId());
            if (series != null) {
                vo.setTournamentName(series.getSeriesName());
            }
            BasketSeason season = seasonMap.get(record.getSeasonId());
            if (season != null) {
                vo.setSeasonName(season.getDisplayTitle() != null ? season.getDisplayTitle() : season.getSeasonName());
                vo.setSeasonNameEn(season.getDisplayTitleEn());
            }
            BasketTeam team = teamMap.get(record.getTeamId());
            BasketEventTeam eventTeam = pickEventTeam(record, eventById, eventBySourceSeason, latestBySource);
            if (team != null) {
                vo.setTeamName(team.getTeamName());
                vo.setTeamLogoUrl(urlMap.get(team.getLogoFileId()));
            }
            if (eventTeam != null) {
                vo.setEventTeamId(eventTeam.getId());
                vo.setPlatformCode(eventTeam.getPlatformCode());
                if (!StringUtils.hasText(vo.getTeamName())) {
                    vo.setTeamName(eventTeam.getTeamNameSnapshot());
                }
                if (!StringUtils.hasText(vo.getTeamLogoUrl()) && eventTeam.getTeamLogoFileId() != null) {
                    vo.setTeamLogoUrl(urlMap.get(eventTeam.getTeamLogoFileId()));
                }
            }
            BasketPlayerProfile player = playerMap.get(record.getPlayerId());
            if (player != null && publicSupport.isPlayerPubliclyVisible(player)) {
                vo.setPlayerName(publicSupport.resolveDisplayName(player));
                vo.setPlayerAvatarUrl(MemberAvatarSupport.resolve(avatarMap.get(player.getId())));
            }
            result.add(vo);
        }
        return result;
    }

    private Set<Long> teamKeysForPlayer(Long playerId) {
        if (playerId == null) {
            return Collections.emptySet();
        }
        List<BasketEventPlayer> players = eventPlayerDao.selectList(new LambdaQueryWrapper<BasketEventPlayer>()
                .eq(BasketEventPlayer::getSourcePlayerId, playerId)
                .eq(BasketEventPlayer::getIsDelete, BasketPublicConstants.NOT_DELETED));
        if (CollectionUtils.isEmpty(players)) {
            return Collections.emptySet();
        }
        Set<Long> eventTeamIds = players.stream()
                .map(BasketEventPlayer::getEventTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (eventTeamIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<BasketEventTeam> teams = eventTeamDao.selectList(new LambdaQueryWrapper<BasketEventTeam>()
                .in(BasketEventTeam::getId, eventTeamIds)
                .eq(BasketEventTeam::getIsDelete, BasketPublicConstants.NOT_DELETED));
        Set<Long> keys = new HashSet<>(eventTeamIds);
        if (!CollectionUtils.isEmpty(teams)) {
            for (BasketEventTeam team : teams) {
                if (team.getSourceTeamId() != null) {
                    keys.add(team.getSourceTeamId());
                }
            }
        }
        return keys;
    }

    private List<BasketEventTeam> loadEventTeams(Set<Long> teamIds) {
        if (CollectionUtils.isEmpty(teamIds)) {
            return Collections.emptyList();
        }
        return eventTeamDao.selectList(new LambdaQueryWrapper<BasketEventTeam>()
                .eq(BasketEventTeam::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .and(w -> w.in(BasketEventTeam::getId, teamIds)
                        .or()
                        .in(BasketEventTeam::getSourceTeamId, teamIds)));
    }

    private BasketEventTeam pickEventTeam(BasketHonorRecord record,
                                          Map<Long, BasketEventTeam> eventById,
                                          Map<String, BasketEventTeam> eventBySourceSeason,
                                          Map<Long, BasketEventTeam> latestBySource) {
        if (record.getTeamId() == null) {
            return null;
        }
        if (record.getSeasonId() != null) {
            BasketEventTeam bySeason = eventBySourceSeason.get(record.getTeamId() + ":" + record.getSeasonId());
            if (bySeason != null) {
                return bySeason;
            }
        }
        BasketEventTeam byId = eventById.get(record.getTeamId());
        if (byId != null) {
            return byId;
        }
        return latestBySource.get(record.getTeamId());
    }

    private BasketEventTeam preferNewerEventTeam(BasketEventTeam left, BasketEventTeam right) {
        long leftId = left.getId() == null ? 0L : left.getId();
        long rightId = right.getId() == null ? 0L : right.getId();
        return rightId >= leftId ? right : left;
    }

    private void copyItem(PublicHonorItemResp src, PublicHonorDetailResp dest) {
        dest.setHonorId(src.getHonorId());
        dest.setTournamentId(src.getTournamentId());
        dest.setTournamentName(src.getTournamentName());
        dest.setSeasonId(src.getSeasonId());
        dest.setSeasonName(src.getSeasonName());
        dest.setSeasonNameEn(src.getSeasonNameEn());
        dest.setHonorCode(src.getHonorCode());
        dest.setHonorName(src.getHonorName());
        dest.setHonorScope(src.getHonorScope());
        dest.setRecipientType(src.getRecipientType());
        dest.setTeamId(src.getTeamId());
        dest.setTeamName(src.getTeamName());
        dest.setTeamLogoUrl(src.getTeamLogoUrl());
        dest.setEventTeamId(src.getEventTeamId());
        dest.setPlatformCode(src.getPlatformCode());
        dest.setPlayerId(src.getPlayerId());
        dest.setPlayerName(src.getPlayerName());
        dest.setPlayerAvatarUrl(src.getPlayerAvatarUrl());
        dest.setHonorTitle(src.getHonorTitle());
        dest.setHonorTitleEn(src.getHonorTitleEn());
        dest.setHonorSubtitle(src.getHonorSubtitle());
        dest.setHonorSubtitleEn(src.getHonorSubtitleEn());
        dest.setHonorValue(src.getHonorValue());
        dest.setAwardDate(src.getAwardDate());
        dest.setCoverUrl(src.getCoverUrl());
        dest.setDisplayOrder(src.getDisplayOrder());
    }
}
