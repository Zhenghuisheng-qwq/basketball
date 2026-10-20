package com.zhs.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.v4.BasketDivisionFormatSnapshotDao;
import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.dao.v4.BasketScheduleBatchDao;
import com.zhs.dao.v4.BasketV4MatchDao;
import com.zhs.dao.v4.BasketV4MatchTeamStatDao;
import com.zhs.model.v4.BasketV4MatchTeamStat;
import com.zhs.enums.v4.MatchPublishStatusEnum;
import com.zhs.model.v4.BasketDivisionFormatSnapshot;
import com.zhs.enums.v4.MatchStatusEnum;
import com.zhs.enums.v4.PublishStatusEnum;
import com.zhs.mapper.v4.BasketV4MatchSeriesMapper;
import com.zhs.mapper.v4.BasketV4PublicStageMapper;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketScheduleBatch;
import com.zhs.model.v4.BasketMatchSeries;
import com.zhs.model.v4.BasketV4Match;
import com.zhs.response.v4.publicapi.V4PublicMatchSeriesDetailResp;
import com.zhs.support.v4.PublicCurrentRosterSupport;
import com.zhs.support.v4.PublicCurrentRosterSupport.CurrentTeamFace;
import com.zhs.response.v4.publicapi.V4PublicMatchSeriesGameResp;
import com.zhs.response.v4.publicapi.V4PublicScheduleItemResp;
import com.zhs.response.v4.publicapi.V4PublicStageItemResp;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component
public class V4MatchSeriesDisplaySupport {

    @Resource
    private BasketV4MatchSeriesMapper matchSeriesMapper;
    @Resource
    private BasketV4MatchDao matchDao;
    @Resource
    private BasketEventTeamDao eventTeamDao;
    @Resource
    private PublicCurrentRosterSupport currentRosterSupport;
    @Resource
    private BasketV4PublicStageMapper publicStageMapper;
    @Resource
    private BasketScheduleBatchDao scheduleBatchDao;
    @Resource
    private BasketDivisionFormatSnapshotDao formatSnapshotDao;
    @Resource
    private BasketPublicSupport publicSupport;
    @Resource
    private BasketV4MatchTeamStatDao matchTeamStatDao;

    public V4PublicMatchSeriesDetailResp buildDetail(Long seriesId, Long currentMatchId) {
        if (seriesId == null) {
            return null;
        }
        BasketMatchSeries series = matchSeriesMapper.selectById(seriesId);
        if (series == null) {
            return null;
        }
        if (!isPubliclyVisibleSeries(series)) {
            return null;
        }
        return buildDetail(series, currentMatchId, null);
    }

    public V4PublicMatchSeriesDetailResp buildDetail(Long seriesId, Long currentMatchId,
                                                    Map<Long, BasketEventTeam> teamMap,
                                                    Map<Long, String> urlMap,
                                                    List<V4PublicStageItemResp> stages) {
        if (seriesId == null) {
            return null;
        }
        BasketMatchSeries series = matchSeriesMapper.selectById(seriesId);
        if (series == null || !isPubliclyVisibleSeries(series)) {
            return null;
        }
        SeriesBuildContext ctx = prepareSeriesBuildContext(List.of(series), teamMap, urlMap, stages);
        return buildDetail(series, currentMatchId, ctx);
    }

    public List<V4PublicMatchSeriesDetailResp> listByStageId(Long stageId) {
        List<BasketMatchSeries> list = matchSeriesMapper.selectByStageId(stageId);
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }
        ScheduleVisibilityCache visibility = new ScheduleVisibilityCache();
        List<BasketMatchSeries> visible = new ArrayList<>();
        for (BasketMatchSeries series : list) {
            if (isPubliclyVisibleSeries(series, visibility)) {
                visible.add(series);
            }
        }
        if (visible.isEmpty()) {
            return List.of();
        }
        SeriesBuildContext ctx = prepareSeriesBuildContext(visible, null, null, null);
        List<V4PublicMatchSeriesDetailResp> result = new ArrayList<>();
        for (BasketMatchSeries series : visible) {
            result.add(buildDetail(series, null, ctx));
        }
        return result;
    }

    public List<V4PublicScheduleItemResp> listScheduleByDivisionId(Long divisionId) {
        if (divisionId == null) {
            return List.of();
        }
        List<V4PublicStageItemResp> stages = publicStageMapper.selectStagesByDivisionId(divisionId);
        List<BasketMatchSeries> seriesList = matchSeriesMapper.selectByDivisionId(divisionId);
        if (seriesList == null) {
            seriesList = new ArrayList<>();
        }
        Map<Long, V4PublicStageItemResp> stageMap = new HashMap<>();
        Map<Long, Integer> maxRoundByStage = new HashMap<>();
        if (!CollectionUtils.isEmpty(stages)) {
            for (V4PublicStageItemResp stage : stages) {
                if (stage.getStageId() != null) {
                    stageMap.put(stage.getStageId(), stage);
                }
            }
        }
        if (!CollectionUtils.isEmpty(seriesList)) {
            for (BasketMatchSeries series : seriesList) {
                if (series.getStageId() == null || series.getRoundNo() == null) {
                    continue;
                }
                maxRoundByStage.merge(series.getStageId(), series.getRoundNo(), Math::max);
            }
        }

        List<BasketV4Match> matches = matchDao.selectList(new LambdaQueryWrapper<BasketV4Match>()
                .eq(BasketV4Match::getDivisionId, divisionId)
                .eq(BasketV4Match::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketV4Match::getRoundNo)
                .orderByAsc(BasketV4Match::getMatchNo));
        Map<Long, BasketEventTeam> teamMap = loadScheduleTeamMap(seriesList, matches);
        Map<Long, List<BasketV4Match>> matchesBySeries = groupMatchesBySeriesId(matches);

        ScheduleVisibilityCache visibility = new ScheduleVisibilityCache();
        List<V4PublicScheduleItemResp> items = new ArrayList<>();
        Set<Long> exposedSeriesIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(seriesList)) {
            for (BasketMatchSeries series : seriesList) {
                if (!isPubliclyVisibleSeries(series, visibility)) {
                    continue;
                }
                if (!PublicScheduleUnitResolver.exposeAsSeries(series.getBestOf())) {
                    continue;
                }
                items.add(toSeriesScheduleItem(series, stageMap.get(series.getStageId()),
                        maxRoundByStage.get(series.getStageId()), teamMap,
                        matchesBySeries.getOrDefault(series.getId(), List.of())));
                if (series.getId() != null) {
                    exposedSeriesIds.add(series.getId());
                }
            }
        }
        if (!CollectionUtils.isEmpty(matches)) {
            for (BasketV4Match match : matches) {
                if (match.getSeriesId() != null && exposedSeriesIds.contains(match.getSeriesId())) {
                    continue;
                }
                if (!isPubliclyVisibleMatch(match, visibility)) {
                    continue;
                }
                items.add(toMatchScheduleItem(match, stageMap.get(match.getStageId()), teamMap));
            }
        }

        items.sort(Comparator
                .comparing(V4PublicScheduleItemResp::getStageId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(V4PublicScheduleItemResp::getRoundNo, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(V4PublicScheduleItemResp::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        fillScheduleTeamLogos(items, teamMap);
        fillScheduleActionFlags(items, matches);
        return items;
    }

    private Map<Long, BasketEventTeam> loadScheduleTeamMap(List<BasketMatchSeries> seriesList,
                                                           List<BasketV4Match> matches) {
        Set<Long> teamIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(seriesList)) {
            for (BasketMatchSeries series : seriesList) {
                addPositiveId(teamIds, series.getHomeEventTeamId());
                addPositiveId(teamIds, series.getAwayEventTeamId());
            }
        }
        if (!CollectionUtils.isEmpty(matches)) {
            for (BasketV4Match match : matches) {
                addPositiveId(teamIds, match.getHomeEventTeamId());
                addPositiveId(teamIds, match.getAwayEventTeamId());
            }
        }
        if (teamIds.isEmpty()) {
            return new HashMap<>();
        }
        List<BasketEventTeam> teams = eventTeamDao.selectByIds(teamIds);
        if (CollectionUtils.isEmpty(teams)) {
            return new HashMap<>();
        }
        Map<Long, BasketEventTeam> map = new HashMap<>();
        for (BasketEventTeam team : teams) {
            if (team != null && team.getId() != null) {
                map.put(team.getId(), team);
            }
        }
        return map;
    }

    private Map<Long, CurrentTeamFace> loadCurrentFaces(Collection<BasketEventTeam> teams) {
        if (currentRosterSupport == null || CollectionUtils.isEmpty(teams)) {
            return Map.of();
        }
        return currentRosterSupport.loadCurrentFaces(teams);
    }

    private void addPositiveId(Set<Long> ids, Long id) {
        if (id != null && id > 0) {
            ids.add(id);
        }
    }

    private void fillScheduleTeamLogos(List<V4PublicScheduleItemResp> items, Map<Long, BasketEventTeam> teamMap) {
        if (CollectionUtils.isEmpty(items) || teamMap == null || teamMap.isEmpty()) {
            return;
        }
        Set<Long> logoIds = new HashSet<>();
        for (BasketEventTeam team : teamMap.values()) {
            if (team != null && team.getTeamLogoFileId() != null) {
                logoIds.add(team.getTeamLogoFileId());
            }
        }
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(logoIds);
        Map<Long, CurrentTeamFace> currentFaces = currentRosterSupport == null
                ? Map.of()
                : currentRosterSupport.loadCurrentFaces(teamMap.values());
        for (V4PublicScheduleItemResp item : items) {
            BasketEventTeam home = teamMap.get(item.getHomeEventTeamId());
            if (home != null) {
                item.setHomeTeamLogoFileId(home.getTeamLogoFileId());
                item.setHomeTeamLogoUrl(urlMap.get(home.getTeamLogoFileId()));
                fillScheduleTeamIdentity(item, home, true);
            }
            BasketEventTeam away = teamMap.get(item.getAwayEventTeamId());
            if (away != null) {
                item.setAwayTeamLogoFileId(away.getTeamLogoFileId());
                item.setAwayTeamLogoUrl(urlMap.get(away.getTeamLogoFileId()));
                fillScheduleTeamIdentity(item, away, false);
            }
            if (PublicCurrentRosterSupport.useCurrentIdentity(item.getStatus())) {
                PublicCurrentRosterSupport.applyScheduleHome(item, currentFaces.get(item.getHomeEventTeamId()));
                PublicCurrentRosterSupport.applyScheduleAway(item, currentFaces.get(item.getAwayEventTeamId()));
            }
        }
    }

    private void fillScheduleActionFlags(List<V4PublicScheduleItemResp> items, List<BasketV4Match> matches) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        Map<Long, BasketV4Match> matchById = new HashMap<>();
        Map<Long, List<Long>> seriesMatchIds = new HashMap<>();
        Set<Long> allMatchIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(matches)) {
            for (BasketV4Match match : matches) {
                if (match == null || match.getId() == null) {
                    continue;
                }
                matchById.put(match.getId(), match);
                allMatchIds.add(match.getId());
                if (match.getSeriesId() != null && match.getSeriesId() > 0) {
                    seriesMatchIds.computeIfAbsent(match.getSeriesId(), key -> new ArrayList<>()).add(match.getId());
                }
            }
        }
        Set<Long> matchIdsWithStats = loadMatchIdsWithOfficialStats(allMatchIds);
        for (V4PublicScheduleItemResp item : items) {
            if (item == null || item.getId() == null) {
                continue;
            }
            if (PublicScheduleUnitResolver.MATCH_SERIES.equals(item.getUnitType())) {
                List<Long> gameIds = seriesMatchIds.getOrDefault(item.getId(), List.of());
                boolean hasStats = false;
                String replayUrl = null;
                for (Long gameId : gameIds) {
                    if (matchIdsWithStats.contains(gameId)) {
                        hasStats = true;
                    }
                    if (replayUrl == null) {
                        BasketV4Match game = matchById.get(gameId);
                        replayUrl = game == null ? null : normalizeReplayUrl(game.getReplayUrl());
                    }
                }
                item.setHasOfficialStats(hasStats);
                item.setReplayUrl(replayUrl);
                continue;
            }
            BasketV4Match match = matchById.get(item.getId());
            item.setReplayUrl(match == null ? null : normalizeReplayUrl(match.getReplayUrl()));
            item.setHasOfficialStats(matchIdsWithStats.contains(item.getId()));
        }
    }

    private Set<Long> loadMatchIdsWithOfficialStats(Set<Long> matchIds) {
        if (CollectionUtils.isEmpty(matchIds)) {
            return Set.of();
        }
        List<BasketV4MatchTeamStat> rows = matchTeamStatDao.selectList(new LambdaQueryWrapper<BasketV4MatchTeamStat>()
                .in(BasketV4MatchTeamStat::getMatchId, matchIds)
                .eq(BasketV4MatchTeamStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .select(BasketV4MatchTeamStat::getMatchId));
        if (CollectionUtils.isEmpty(rows)) {
            return Set.of();
        }
        Set<Long> result = new HashSet<>();
        for (BasketV4MatchTeamStat row : rows) {
            if (row != null && row.getMatchId() != null) {
                result.add(row.getMatchId());
            }
        }
        return result;
    }

    private void fillScheduleTeamIdentity(V4PublicScheduleItemResp item, BasketEventTeam team, boolean home) {
        if (item == null || team == null) {
            return;
        }
        String shortName = blankToNull(team.getTeamShortName());
        String code = firstNonBlank(team.getTeamShortName(), team.getEventTeamNo());
        if (home) {
            if (item.getHomeTeamShortName() == null || item.getHomeTeamShortName().isBlank()) {
                item.setHomeTeamShortName(shortName);
            }
            if (item.getHomeTeamCode() == null || item.getHomeTeamCode().isBlank()) {
                item.setHomeTeamCode(code);
            }
            return;
        }
        if (item.getAwayTeamShortName() == null || item.getAwayTeamShortName().isBlank()) {
            item.setAwayTeamShortName(shortName);
        }
        if (item.getAwayTeamCode() == null || item.getAwayTeamCode().isBlank()) {
            item.setAwayTeamCode(code);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeReplayUrl(String replayUrl) {
        if (replayUrl == null || replayUrl.isBlank()) {
            return null;
        }
        String trimmed = replayUrl.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("javascript:")) {
            return null;
        }
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return null;
        }
        return trimmed;
    }

    public V4PublicMatchSeriesDetailResp buildDetail(BasketMatchSeries series, Long currentMatchId) {
        return buildDetail(series, currentMatchId, prepareSeriesBuildContext(List.of(series), null, null, null));
    }

    private V4PublicMatchSeriesDetailResp buildDetail(BasketMatchSeries series, Long currentMatchId,
                                                     SeriesBuildContext ctx) {
        if (ctx == null) {
            ctx = prepareSeriesBuildContext(List.of(series), null, null, null);
        }
        V4PublicMatchSeriesDetailResp vo = new V4PublicMatchSeriesDetailResp();
        vo.setId(series.getId());
        vo.setSeriesCode(series.getSeriesCode());
        vo.setDivisionId(series.getDivisionId());
        vo.setStageId(series.getStageId());
        vo.setRoundNo(series.getRoundNo());
        vo.setRoundName(PublicKnockoutRoundNames.name(series.getRoundNo(), ctx.maxRoundByStage.get(series.getStageId())));
        vo.setBestOf(series.getBestOf());
        vo.setRequiredWins(requiredWins(series.getBestOf()));
        int[] displayWins = resolveDisplayWinCounts(series);
        vo.setHomeWinCount(displayWins[0]);
        vo.setAwayWinCount(displayWins[1]);
        vo.setSeriesStatus(series.getSeriesStatus());
        vo.setSeriesStatusName(seriesStatusLabel(series.getSeriesStatus()));
        vo.setResolutionType(series.getResolutionType());
        vo.setResolutionTypeName(resolutionTypeLabel(series.getResolutionType()));
        vo.setWinnerEventTeamId(series.getWinnerEventTeamId());
        vo.setHomeEventTeamId(series.getHomeEventTeamId());
        vo.setAwayEventTeamId(series.getAwayEventTeamId());
        vo.setHomeSourceType(series.getHomeSourceType());
        vo.setAwaySourceType(series.getAwaySourceType());
        vo.setHomeSourceDisplay(series.getHomeSourceDisplay());
        vo.setAwaySourceDisplay(series.getAwaySourceDisplay());
        vo.setScheduledDate(series.getScheduledDate());
        vo.setScheduledStartTime(series.getScheduledStartTime());
        if (series.getHomeEventTeamId() != null && series.getHomeEventTeamId() <= 0) {
            vo.setHomeEventTeamId(null);
        }
        if (series.getAwayEventTeamId() != null && series.getAwayEventTeamId() <= 0) {
            vo.setAwayEventTeamId(null);
        }

        fillStageInfo(vo, series, ctx.stages);
        fillTeamNames(vo, series, ctx.teamMap, ctx.urlMap, ctx.currentFaces);
        fillForfeitSummary(vo, series);
        fillDisqualificationDisplay(vo, series, ctx.teamMap);
        vo.setGames(buildGames(series, currentMatchId, ctx.matchesBySeries.getOrDefault(series.getId(), List.of())));
        return vo;
    }

    private void fillStageInfo(V4PublicMatchSeriesDetailResp vo, BasketMatchSeries series,
                               List<V4PublicStageItemResp> stages) {
        if (series.getDivisionId() == null || series.getStageId() == null) {
            return;
        }
        if (CollectionUtils.isEmpty(stages)) {
            return;
        }
        for (V4PublicStageItemResp stage : stages) {
            if (series.getStageId().equals(stage.getStageId())) {
                vo.setStageName(stage.getStageName());
                boolean finalStage = (stage.getNextStageCode() == null || stage.getNextStageCode().isEmpty())
                        && "NONE".equalsIgnoreCase(String.valueOf(stage.getAdvancementType()));
                vo.setFinalStage(finalStage);
                if ("COMPLETED".equals(series.getSeriesStatus()) && series.getWinnerEventTeamId() != null) {
                    vo.setResultLabel(Boolean.TRUE.equals(vo.getFinalStage()) ? "冠军" : "本轮胜者");
                }
                break;
            }
        }
    }

    private void fillTeamNames(V4PublicMatchSeriesDetailResp vo, BasketMatchSeries series,
                               Map<Long, BasketEventTeam> teamMap, Map<Long, String> urlMap,
                               Map<Long, CurrentTeamFace> currentFaces) {
        Map<Long, BasketEventTeam> map = teamMap == null ? Collections.emptyMap() : teamMap;
        if (urlMap == null) {
            urlMap = Collections.emptyMap();
        }
        if (map.isEmpty()
                && (series.getHomeEventTeamId() == null || series.getHomeEventTeamId() <= 0)
                && (series.getAwayEventTeamId() == null || series.getAwayEventTeamId() <= 0)) {
            if (series.getHomeSourceDisplay() != null && !series.getHomeSourceDisplay().isBlank()) {
                vo.setHomeTeamName(series.getHomeSourceDisplay());
            } else {
                vo.setHomeTeamName(null);
            }
            if (series.getAwaySourceDisplay() != null && !series.getAwaySourceDisplay().isBlank()) {
                vo.setAwayTeamName(series.getAwaySourceDisplay());
            } else {
                vo.setAwayTeamName(null);
            }
            return;
        }
        BasketEventTeam home = map.get(series.getHomeEventTeamId());
        if (home != null) {
            vo.setHomeTeamName(home.getTeamNameSnapshot());
            vo.setHomeTeamShortName(blankToNull(home.getTeamShortName()));
            vo.setHomeTeamLogoFileId(home.getTeamLogoFileId());
            vo.setHomeTeamLogoUrl(urlMap.get(home.getTeamLogoFileId()));
        } else if (series.getHomeSourceDisplay() != null && !series.getHomeSourceDisplay().isBlank()) {
            vo.setHomeTeamName(series.getHomeSourceDisplay());
        } else {
            vo.setHomeTeamName(blankToNull(series.getHomeSourceDisplay()));
        }
        BasketEventTeam away = map.get(series.getAwayEventTeamId());
        if (away != null) {
            vo.setAwayTeamName(away.getTeamNameSnapshot());
            vo.setAwayTeamShortName(blankToNull(away.getTeamShortName()));
            vo.setAwayTeamLogoFileId(away.getTeamLogoFileId());
            vo.setAwayTeamLogoUrl(urlMap.get(away.getTeamLogoFileId()));
        } else if (series.getAwaySourceDisplay() != null && !series.getAwaySourceDisplay().isBlank()) {
            vo.setAwayTeamName(series.getAwaySourceDisplay());
        } else {
            vo.setAwayTeamName(blankToNull(series.getAwaySourceDisplay()));
        }
        BasketEventTeam winner = map.get(series.getWinnerEventTeamId());
        if (winner != null) {
            vo.setWinnerTeamName(winner.getTeamNameSnapshot());
        }
        if (PublicCurrentRosterSupport.useCurrentIdentity(series.getSeriesStatus()) && currentFaces != null) {
            PublicCurrentRosterSupport.applySeriesHome(vo, currentFaces.get(series.getHomeEventTeamId()));
            PublicCurrentRosterSupport.applySeriesAway(vo, currentFaces.get(series.getAwayEventTeamId()));
        }
    }

    private List<V4PublicMatchSeriesGameResp> buildGames(BasketMatchSeries series, Long currentMatchId,
                                                        List<BasketV4Match> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return List.of();
        }
        boolean seriesCompleted = "COMPLETED".equals(series.getSeriesStatus());
        boolean seriesForfeit = "FORFEIT".equalsIgnoreCase(String.valueOf(series.getResolutionType()));
        List<V4PublicMatchSeriesGameResp> games = new ArrayList<>();
        for (BasketV4Match match : matches) {
            V4PublicMatchSeriesGameResp game = new V4PublicMatchSeriesGameResp();
            game.setMatchId(match.getId());
            game.setMatchCode(match.getMatchNo());
            game.setSeriesGameNo(match.getSeriesGameNo());
            game.setMatchStatus(match.getMatchStatus());
            game.setMatchStatusName(matchStatusLabel(match.getMatchStatus()));
            game.setHomeScore(match.getHomeScore());
            game.setAwayScore(match.getAwayScore());
            game.setWinnerEventTeamId(match.getWinnerEventTeamId());
            game.setScheduledTime(match.getMatchTime() != null ? match.getMatchTime() : seriesStart(series));
            game.setCurrentGame(currentMatchId != null && currentMatchId.equals(match.getId()));
            game.setReplayUrl(normalizeReplayUrl(match.getReplayUrl()));
            if ((seriesCompleted || seriesForfeit) && !isOfficialResult(match)) {
                game.setSkipped(true);
                game.setSkippedReason(seriesForfeit ? "系列赛已判定弃赛，本局无需进行" : "系列赛已提前结束，本局无需进行");
            }
            games.add(game);
        }
        return games;
    }

    public Date resolveDisplayTime(BasketV4Match match, BasketMatchSeries series) {
        if (match != null && match.getMatchTime() != null) {
            return match.getMatchTime();
        }
        return seriesStart(series);
    }

    private Date seriesStart(BasketMatchSeries series) {
        if (series == null || series.getScheduledDate() == null || series.getScheduledStartTime() == null) {
            return null;
        }
        return Date.from(LocalDateTime.of(series.getScheduledDate(), series.getScheduledStartTime())
                .atZone(ZoneId.of("Asia/Shanghai")).toInstant());
    }

    public Map<Long, BasketMatchSeries> loadSeriesMap(Collection<Long> seriesIds) {
        if (CollectionUtils.isEmpty(seriesIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = new HashSet<>();
        for (Long id : seriesIds) {
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketMatchSeries> list = matchSeriesMapper.selectByIds(ids);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        Map<Long, BasketMatchSeries> map = new HashMap<>();
        for (BasketMatchSeries series : list) {
            if (series != null && series.getId() != null) {
                map.put(series.getId(), series);
            }
        }
        return map;
    }

    public List<BasketV4Match> filterPubliclyVisibleMatches(List<BasketV4Match> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return matches;
        }
        ScheduleVisibilityCache cache = new ScheduleVisibilityCache();
        List<BasketV4Match> kept = new ArrayList<>();
        for (BasketV4Match match : matches) {
            if (isPubliclyVisibleMatch(match, cache)) {
                kept.add(match);
            }
        }
        return excludeRemainingUnplayed(kept);
    }

    public boolean isPublishedBatch(Long batchId) {
        return isPublishedBatch(batchId, new ScheduleVisibilityCache());
    }

    private boolean isPublishedBatch(Long batchId, ScheduleVisibilityCache cache) {
        if (batchId == null) {
            return false;
        }
        return cache.batchPublished.computeIfAbsent(batchId, id -> {
            BasketScheduleBatch batch = scheduleBatchDao.selectById(id);
            return batch != null && PublishStatusEnum.PUBLISHED.getCode().equals(batch.getPublishStatus());
        });
    }

    public boolean isVersionedDivision(Long divisionId) {
        if (divisionId == null) {
            return false;
        }
        BasketDivisionFormatSnapshot snapshot = formatSnapshotDao.selectOne(new LambdaQueryWrapper<BasketDivisionFormatSnapshot>()
                .eq(BasketDivisionFormatSnapshot::getDivisionId, divisionId)
                .eq(BasketDivisionFormatSnapshot::getIsCurrent, 1)
                .eq(BasketDivisionFormatSnapshot::getIsDelete, 0)
                .last("limit 1"));
        return snapshot != null && PublicVisibilityPolicy.isVersionedWorkflow(snapshot.getRosterWorkflowMode());
    }

    public List<BasketV4Match> excludeRemainingUnplayed(List<BasketV4Match> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return matches;
        }
        Set<Long> seriesIds = new HashSet<>();
        for (BasketV4Match match : matches) {
            if (match != null && match.getSeriesId() != null) {
                seriesIds.add(match.getSeriesId());
            }
        }
        Map<Long, BasketMatchSeries> seriesMap = loadSeriesMap(seriesIds);
        List<BasketV4Match> kept = new ArrayList<>();
        for (BasketV4Match match : matches) {
            if (match == null) {
                continue;
            }
            BasketMatchSeries series = match.getSeriesId() == null ? null : seriesMap.get(match.getSeriesId());
            if (!isRemainingUnplayedChild(match, series)) {
                kept.add(match);
            }
        }
        return kept;
    }

    public boolean isRemainingUnplayedChild(BasketV4Match match, BasketMatchSeries series) {
        if (match == null || series == null) {
            return false;
        }
        boolean seriesOver = "COMPLETED".equals(series.getSeriesStatus())
                || "FORFEIT".equalsIgnoreCase(String.valueOf(series.getResolutionType()));
        return !PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(seriesOver, isOfficialResult(match));
    }

    public boolean isPubliclyVisibleMatch(BasketV4Match match) {
        return isPubliclyVisibleMatch(match, new ScheduleVisibilityCache());
    }

    private boolean isPubliclyVisibleMatch(BasketV4Match match, ScheduleVisibilityCache cache) {
        if (match == null) {
            return false;
        }
        boolean matchPublished = MatchPublishStatusEnum.PUBLISHED.getCode().equals(match.getPublishStatus());
        boolean batchPublished = isPublishedBatch(match.getScheduleBatchId(), cache);
        return PublicVisibilityPolicy.keepPublishedMatch(
                versionedDivision(match.getDivisionId(), cache), matchPublished, batchPublished);
    }

    public boolean isPubliclyVisibleSeries(BasketMatchSeries series) {
        return isPubliclyVisibleSeries(series, new ScheduleVisibilityCache());
    }

    private boolean isPubliclyVisibleSeries(BasketMatchSeries series, ScheduleVisibilityCache cache) {
        if (series == null) {
            return false;
        }
        boolean batchPublished = isPublishedBatch(series.getScheduleBatchId(), cache);
        boolean versioned = versionedDivision(series.getDivisionId(), cache);
        boolean hasPublishedChild = false;
        if (!versioned && !batchPublished && series.getId() != null) {
            Long count = matchDao.selectCount(new LambdaQueryWrapper<BasketV4Match>()
                    .eq(BasketV4Match::getSeriesId, series.getId())
                    .eq(BasketV4Match::getIsDelete, 0)
                    .eq(BasketV4Match::getPublishStatus, MatchPublishStatusEnum.PUBLISHED.getCode()));
            hasPublishedChild = count != null && count > 0;
        }
        return PublicVisibilityPolicy.keepPublishedSeries(versioned, batchPublished, hasPublishedChild);
    }

    private boolean versionedDivision(Long divisionId, ScheduleVisibilityCache cache) {
        if (divisionId == null) {
            return false;
        }
        return cache.versionedDivision.computeIfAbsent(divisionId, this::isVersionedDivision);
    }

    private static final class ScheduleVisibilityCache {
        private final Map<Long, Boolean> batchPublished = new HashMap<>();
        private final Map<Long, Boolean> versionedDivision = new HashMap<>();
    }

    private static final class SeriesBuildContext {
        private List<V4PublicStageItemResp> stages = List.of();
        private Map<Long, BasketEventTeam> teamMap = Collections.emptyMap();
        private Map<Long, String> urlMap = Collections.emptyMap();
        private Map<Long, CurrentTeamFace> currentFaces = Map.of();
        private Map<Long, List<BasketV4Match>> matchesBySeries = new HashMap<>();
        private final Map<Long, Integer> maxRoundByStage = new HashMap<>();
    }

    private SeriesBuildContext prepareSeriesBuildContext(List<BasketMatchSeries> seriesList,
                                                        Map<Long, BasketEventTeam> existingTeams,
                                                        Map<Long, String> existingUrls,
                                                        List<V4PublicStageItemResp> existingStages) {
        SeriesBuildContext ctx = new SeriesBuildContext();
        if (CollectionUtils.isEmpty(seriesList)) {
            return ctx;
        }
        Map<Long, Integer> seriesCountByStage = new HashMap<>();
        for (BasketMatchSeries series : seriesList) {
            if (series.getStageId() != null && series.getRoundNo() != null) {
                ctx.maxRoundByStage.merge(series.getStageId(), series.getRoundNo(), Math::max);
            }
            if (series.getStageId() != null) {
                seriesCountByStage.merge(series.getStageId(), 1, Integer::sum);
            }
        }
        for (Map.Entry<Long, Integer> entry : seriesCountByStage.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 1) {
                continue;
            }
            List<BasketMatchSeries> siblings = matchSeriesMapper.selectByStageId(entry.getKey());
            if (CollectionUtils.isEmpty(siblings)) {
                continue;
            }
            for (BasketMatchSeries sibling : siblings) {
                if (sibling != null && sibling.getRoundNo() != null) {
                    ctx.maxRoundByStage.merge(entry.getKey(), sibling.getRoundNo(), Math::max);
                }
            }
        }
        Long divisionId = seriesList.stream()
                .map(BasketMatchSeries::getDivisionId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (!CollectionUtils.isEmpty(existingStages)) {
            ctx.stages = existingStages;
        } else if (divisionId != null) {
            List<V4PublicStageItemResp> stages = publicStageMapper.selectStagesByDivisionId(divisionId);
            ctx.stages = stages == null ? List.of() : stages;
        }
        Set<Long> teamIds = new HashSet<>();
        Set<Long> seriesIds = new HashSet<>();
        for (BasketMatchSeries series : seriesList) {
            if (series.getId() != null) {
                seriesIds.add(series.getId());
            }
            addPositiveId(teamIds, series.getHomeEventTeamId());
            addPositiveId(teamIds, series.getAwayEventTeamId());
            addPositiveId(teamIds, series.getWinnerEventTeamId());
        }
        if (existingTeams != null && !existingTeams.isEmpty() && teamIds.stream().allMatch(existingTeams::containsKey)) {
            ctx.teamMap = existingTeams;
            ctx.urlMap = existingUrls != null ? existingUrls : Collections.emptyMap();
            ctx.currentFaces = loadCurrentFaces(existingTeams.values());
        } else if (!teamIds.isEmpty()) {
            List<BasketEventTeam> teams = eventTeamDao.selectByIds(teamIds);
            Map<Long, BasketEventTeam> map = new HashMap<>();
            Set<Long> logoIds = new HashSet<>();
            if (!CollectionUtils.isEmpty(teams)) {
                for (BasketEventTeam team : teams) {
                    if (team != null && team.getId() != null) {
                        map.put(team.getId(), team);
                        if (team.getTeamLogoFileId() != null) {
                            logoIds.add(team.getTeamLogoFileId());
                        }
                    }
                }
            }
            ctx.teamMap = map;
            ctx.urlMap = publicSupport.loadAccessUrlMap(logoIds);
            ctx.currentFaces = loadCurrentFaces(map.values());
        }
        if (!seriesIds.isEmpty()) {
            List<BasketV4Match> matches = matchDao.selectList(new LambdaQueryWrapper<BasketV4Match>()
                    .in(BasketV4Match::getSeriesId, seriesIds)
                    .eq(BasketV4Match::getIsDelete, BasketPublicConstants.NOT_DELETED)
                    .orderByAsc(BasketV4Match::getSeriesGameNo)
                    .orderByAsc(BasketV4Match::getId));
            ctx.matchesBySeries = groupMatchesBySeriesId(matches);
        }
        return ctx;
    }

    private Map<Long, List<BasketV4Match>> groupMatchesBySeriesId(List<BasketV4Match> matches) {
        Map<Long, List<BasketV4Match>> map = new HashMap<>();
        if (CollectionUtils.isEmpty(matches)) {
            return map;
        }
        for (BasketV4Match match : matches) {
            if (match == null || match.getSeriesId() == null) {
                continue;
            }
            map.computeIfAbsent(match.getSeriesId(), key -> new ArrayList<>()).add(match);
        }
        return map;
    }

    private V4PublicScheduleItemResp toSeriesScheduleItem(BasketMatchSeries series,
                                                          V4PublicStageItemResp stage,
                                                          Integer maxRound,
                                                          Map<Long, BasketEventTeam> teamMap,
                                                          List<BasketV4Match> seriesMatches) {
        V4PublicScheduleItemResp item = new V4PublicScheduleItemResp();
        item.setId(series.getId());
        item.setUnitType(PublicScheduleUnitResolver.MATCH_SERIES);
        item.setStageId(series.getStageId());
        item.setStageName(stage != null ? stage.getStageName() : null);
        item.setRoundNo(series.getRoundNo());
        item.setRoundName(PublicKnockoutRoundNames.name(series.getRoundNo(), maxRound));
        item.setSeriesCode(series.getSeriesCode());
        item.setMatchNo(series.getSeriesCode());
        Long homeId = positiveOrNull(series.getHomeEventTeamId());
        Long awayId = positiveOrNull(series.getAwayEventTeamId());
        item.setHomeEventTeamId(homeId);
        item.setAwayEventTeamId(awayId);
        BasketEventTeam homeTeam = homeId != null ? teamMap.get(homeId) : null;
        BasketEventTeam awayTeam = awayId != null ? teamMap.get(awayId) : null;
        item.setHomeTeam(homeTeam != null ? homeTeam.getTeamNameSnapshot()
                : blankToNull(series.getHomeSourceDisplay()));
        item.setAwayTeam(awayTeam != null ? awayTeam.getTeamNameSnapshot()
                : blankToNull(series.getAwaySourceDisplay()));
        item.setBestOf(series.getBestOf());
        int[] displayWins = resolveDisplayWinCounts(series);
        int homeWins = displayWins[0];
        int awayWins = displayWins[1];
        item.setHomeWinCount(homeWins);
        item.setAwayWinCount(awayWins);
        item.setWinnerEventTeamId(series.getWinnerEventTeamId());
        item.setRequiredWins(requiredWins(series.getBestOf()));
        item.setSeriesScore(formatSeriesScore(series, homeWins, awayWins));
        int gamesPlayed = 0;
        if (!CollectionUtils.isEmpty(seriesMatches)) {
            for (BasketV4Match match : seriesMatches) {
                if (match != null && !isRemainingUnplayedChild(match, series) && isOfficialResult(match)) {
                    gamesPlayed++;
                }
            }
        }
        item.setGamesPlayed(gamesPlayed);
        item.setGameProgress(gamesPlayed + "/" + (series.getBestOf() == null ? 0 : series.getBestOf()));
        item.setScheduleDate(formatScheduleDate(series.getScheduledDate()));
        item.setStartTime(formatScheduleTime(series.getScheduledStartTime()));
        item.setStatus(series.getSeriesStatus());
        item.setStatusName(seriesStatusLabel(series.getSeriesStatus()));
        item.setPlaceholderSource(joinPlaceholderSource(series.getHomeSourceType(), series.getAwaySourceType()));
        item.setPublished(true);
        com.zhs.support.v4.EventTeamPublicDisplaySupport.fillSchedule(item, series, homeTeam, awayTeam);
        return item;
    }

    private V4PublicScheduleItemResp toMatchScheduleItem(BasketV4Match match, V4PublicStageItemResp stage,
                                                         Map<Long, BasketEventTeam> teamMap) {
        V4PublicScheduleItemResp item = new V4PublicScheduleItemResp();
        item.setId(match.getId());
        item.setUnitType(PublicScheduleUnitResolver.MATCH);
        item.setStageId(match.getStageId());
        item.setStageName(stage != null ? stage.getStageName() : null);
        item.setRoundNo(match.getRoundNo());
        item.setRoundName(match.getRoundNo() == null ? "--" : ("R" + match.getRoundNo()));
        item.setSeriesCode(match.getMatchNo());
        item.setMatchNo(match.getMatchNo());
        item.setHomeEventTeamId(positiveOrNull(match.getHomeEventTeamId()));
        item.setAwayEventTeamId(positiveOrNull(match.getAwayEventTeamId()));
        item.setHomeTeam(null);
        item.setAwayTeam(null);
        BasketEventTeam home = item.getHomeEventTeamId() == null ? null : teamMap.get(item.getHomeEventTeamId());
        BasketEventTeam away = item.getAwayEventTeamId() == null ? null : teamMap.get(item.getAwayEventTeamId());
        if (home != null) {
            item.setHomeTeam(home.getTeamNameSnapshot());
            fillScheduleTeamIdentity(item, home, true);
        }
        if (away != null) {
            item.setAwayTeam(away.getTeamNameSnapshot());
            fillScheduleTeamIdentity(item, away, false);
        }
        if (item.getHomeTeam() != null && item.getHomeTeam().isBlank()) {
            item.setHomeTeam(null);
        }
        if (item.getAwayTeam() != null && item.getAwayTeam().isBlank()) {
            item.setAwayTeam(null);
        }
        item.setBestOf(1);
        item.setSeriesScore(null);
        item.setGameProgress(null);
        if (match.getMatchTime() != null) {
            java.time.LocalDateTime ldt = toShanghaiDateTime(match.getMatchTime());
            if (ldt != null) {
                item.setScheduleDate(formatScheduleDate(ldt.toLocalDate()));
                item.setStartTime(formatScheduleTime(ldt.toLocalTime()));
            }
        }
        item.setStatus(match.getMatchStatus());
        item.setStatusName(matchStatusLabel(match.getMatchStatus()));
        item.setPlaceholderSource(null);
        item.setPublished(true);
        item.setWinnerEventTeamId(positiveOrNull(match.getWinnerEventTeamId()));
        applyMatchOfficialDisplay(item, match, home, away);
        return item;
    }

    private void applyMatchOfficialDisplay(V4PublicScheduleItemResp item, BasketV4Match match,
            BasketEventTeam home, BasketEventTeam away) {
        if (item == null || match == null) {
            return;
        }
        boolean homeDq = com.zhs.support.v4.EventTeamPublicDisplaySupport.isDisqualified(home);
        boolean awayDq = com.zhs.support.v4.EventTeamPublicDisplaySupport.isDisqualified(away);
        item.setHomeDisqualified(homeDq);
        item.setAwayDisqualified(awayDq);
        boolean forfeited = MatchStatusEnum.FORFEITED.getCode().equalsIgnoreCase(match.getMatchStatus());
        if (!forfeited) {
            item.setHomeScore(match.getHomeScore());
            item.setAwayScore(match.getAwayScore());
            return;
        }
        boolean dqForfeit = homeDq || awayDq
                || (match.getRemark() != null && match.getRemark().contains("赛事资格取消"));
        item.setResultType(dqForfeit
                ? com.zhs.support.v4.EventTeamPublicDisplaySupport.RESULT_EVENT_DISQUALIFICATION
                : com.zhs.support.v4.EventTeamPublicDisplaySupport.RESULT_FORFEIT);
        Long winner = match.getWinnerEventTeamId();
        if (winner != null && winner.equals(match.getHomeEventTeamId())) {
            item.setHomeResultLabel(com.zhs.support.v4.EventTeamPublicDisplaySupport.LABEL_FORFEIT_WIN);
            item.setAwayResultLabel(com.zhs.support.v4.EventTeamPublicDisplaySupport.LABEL_FORFEIT_LOSS);
        } else if (winner != null && winner.equals(match.getAwayEventTeamId())) {
            item.setHomeResultLabel(com.zhs.support.v4.EventTeamPublicDisplaySupport.LABEL_FORFEIT_LOSS);
            item.setAwayResultLabel(com.zhs.support.v4.EventTeamPublicDisplaySupport.LABEL_FORFEIT_WIN);
        }
        if (!dqForfeit) {
            item.setHomeScore(match.getHomeScore());
            item.setAwayScore(match.getAwayScore());
        }
    }

    private boolean isOfficialGameStatus(String status) {
        return MatchStatusEnum.FINISHED.getCode().equals(status)
                || MatchStatusEnum.FORFEITED.getCode().equals(status);
    }

    private Long positiveOrNull(Long id) {
        return id == null || id <= 0 ? null : id;
    }

    private String joinPlaceholderSource(String home, String away) {
        boolean homePh = home != null && !home.isBlank() && !"DIRECT_EVENT_TEAM".equalsIgnoreCase(home);
        boolean awayPh = away != null && !away.isBlank() && !"DIRECT_EVENT_TEAM".equalsIgnoreCase(away);
        if (homePh && awayPh) {
            return home;
        }
        if (homePh) {
            return home;
        }
        if (awayPh) {
            return away;
        }
        return null;
    }

    private boolean isOfficialResult(BasketV4Match match) {
        return MatchStatusEnum.FINISHED.getCode().equals(match.getMatchStatus())
                || MatchStatusEnum.FORFEITED.getCode().equals(match.getMatchStatus());
    }

    /**
     * 公开端系列赛局分。弃赛若未写入胜场，按获胜所需场次展示，避免 0:0。
     */
    public int[] resolveDisplayWinCounts(BasketMatchSeries series) {
        int home = series.getHomeWinCount() == null ? 0 : series.getHomeWinCount();
        int away = series.getAwayWinCount() == null ? 0 : series.getAwayWinCount();
        boolean forfeit = "FORFEIT".equalsIgnoreCase(String.valueOf(series.getResolutionType()));
        boolean completed = "COMPLETED".equalsIgnoreCase(String.valueOf(series.getSeriesStatus()));
        if ((forfeit || completed) && home == 0 && away == 0 && series.getWinnerEventTeamId() != null) {
            int need = requiredWins(series.getBestOf());
            if (series.getWinnerEventTeamId().equals(series.getHomeEventTeamId())) {
                return new int[]{need, 0};
            }
            if (series.getWinnerEventTeamId().equals(series.getAwayEventTeamId())) {
                return new int[]{0, need};
            }
        }
        return new int[]{home, away};
    }

    public String formatSeriesScore(BasketMatchSeries series, int homeWins, int awayWins) {
        boolean show = "COMPLETED".equalsIgnoreCase(String.valueOf(series.getSeriesStatus()))
                || "FORFEIT".equalsIgnoreCase(String.valueOf(series.getResolutionType()))
                || homeWins > 0
                || awayWins > 0;
        return show ? homeWins + ":" + awayWins : null;
    }

    private int requiredWins(Integer bestOf) {
        if (bestOf == null || bestOf <= 1) {
            return 1;
        }
        return bestOf / 2 + 1;
    }

    private void fillForfeitSummary(V4PublicMatchSeriesDetailResp vo, BasketMatchSeries series) {
        if (!"FORFEIT".equalsIgnoreCase(String.valueOf(series.getResolutionType()))) {
            return;
        }
        if (vo.getWinnerTeamName() == null) {
            return;
        }
        String loserName = null;
        if (series.getWinnerEventTeamId() != null) {
            if (series.getWinnerEventTeamId().equals(series.getHomeEventTeamId())) {
                loserName = vo.getAwayTeamName();
            } else if (series.getWinnerEventTeamId().equals(series.getAwayEventTeamId())) {
                loserName = vo.getHomeTeamName();
            }
        }
        if (loserName != null) {
            vo.setResultSummary(loserName + " 弃赛，" + vo.getWinnerTeamName() + " 晋级");
        } else {
            vo.setResultSummary("系列赛弃赛，" + vo.getWinnerTeamName() + " 晋级");
        }
    }

    private void fillDisqualificationDisplay(V4PublicMatchSeriesDetailResp vo, BasketMatchSeries series,
                                            Map<Long, BasketEventTeam> teamMap) {
        Map<Long, BasketEventTeam> map = teamMap == null ? Collections.emptyMap() : teamMap;
        BasketEventTeam home = series.getHomeEventTeamId() != null && series.getHomeEventTeamId() > 0
                ? map.get(series.getHomeEventTeamId()) : null;
        BasketEventTeam away = series.getAwayEventTeamId() != null && series.getAwayEventTeamId() > 0
                ? map.get(series.getAwayEventTeamId()) : null;
        com.zhs.support.v4.EventTeamPublicDisplaySupport.fillSeries(vo, series, home, away);
    }

    private String resolutionTypeLabel(String type) {
        if (type == null || type.isEmpty()) {
            return "正常结束";
        }
        return switch (type.toUpperCase()) {
            case "FORFEIT" -> "系列赛弃赛";
            case "MANUAL" -> "人工裁定";
            case "NORMAL" -> "正常结束";
            default -> type;
        };
    }

    private String seriesStatusLabel(String status) {
        if (status == null) {
            return "--";
        }
        return switch (status) {
            case "PENDING" -> "待准备";
            case "READY" -> "待比赛";
            case "RUNNING" -> "进行中";
            case "COMPLETED" -> "已结束";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private String matchStatusLabel(String status) {
        if (status == null) {
            return "--";
        }
        try {
            return MatchStatusEnum.fromCode(status).getDescription();
        } catch (Exception ex) {
            return status;
        }
    }

    private String formatScheduleDate(java.time.LocalDate date) {
        return date == null ? null : date.toString();
    }

    private String formatScheduleTime(java.time.LocalTime time) {
        if (time == null) {
            return null;
        }
        String raw = time.toString();
        return raw.length() >= 5 ? raw.substring(0, 5) : raw;
    }

    private java.time.LocalDateTime toShanghaiDateTime(java.util.Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().atStartOfDay();
        }
        return java.time.LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.of("Asia/Shanghai"));
    }
}
