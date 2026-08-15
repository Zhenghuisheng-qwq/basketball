package com.zhs.service.impl.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.v4.*;
import com.zhs.enums.v4.DivisionStatusEnum;
import com.zhs.enums.v4.MatchPublishStatusEnum;
import com.zhs.enums.v4.MatchStatusEnum;
import com.zhs.enums.v4.PublishStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.v4.*;
import com.zhs.response.v4.publicapi.*;
import com.zhs.mapper.v4.BasketV4PublicStageMapper;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import com.zhs.service.support.PublicTeamDisplaySupport;
import com.zhs.service.support.V4MatchSeriesDisplaySupport;
import com.zhs.service.v4.IBasketV4PublicService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BasketV4PublicServiceImpl implements IBasketV4PublicService {

    @Resource
    private BasketSeriesDao seriesDao;
    @Resource
    private BasketSeasonDao seasonDao;
    @Resource
    private BasketDivisionDao divisionDao;
    @Resource
    private BasketEventTeamDao eventTeamDao;
    @Resource
    private BasketEventPlayerDao eventPlayerDao;
    @Resource
    private BasketV4MatchDao matchDao;
    @Resource
    private BasketStandingDao standingDao;
    @Resource
    private BasketTeamStatDao teamStatDao;
    @Resource
    private BasketPlayerStatDao playerStatDao;
    @Resource
    private BasketRankDao rankDao;
    @Resource
    private BasketV4MatchTeamStatDao matchTeamStatDao;
    @Resource
    private BasketV4MatchPlayerStatDao matchPlayerStatDao;
    @Resource
    private BasketPublicSupport publicSupport;
    @Resource
    private BasketV4PublicStageMapper publicStageMapper;
    @Resource
    private BasketDivisionFormatSnapshotDao formatSnapshotDao;
    @Resource
    private V4MatchSeriesDisplaySupport matchSeriesDisplaySupport;
    @Resource
    private BasketScheduleBatchDao scheduleBatchDao;

    @Override
    public List<V4PublicDivisionListItemResp> listDivisions(Long seasonId) {
        if (seasonId == null || seasonId <= 0) {
            return Collections.emptyList();
        }
        BasketSeason season = seasonDao.selectById(seasonId);
        if (season == null || !Objects.equals(season.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
            return Collections.emptyList();
        }
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(season.getPublishStatus())) {
            return Collections.emptyList();
        }
        List<BasketDivision> divisions = listDivisionsBySeasonId(seasonId);
        if (CollectionUtils.isEmpty(divisions)) {
            return Collections.emptyList();
        }
        Set<Long> snapshotIds = divisions.stream()
                .map(BasketDivision::getFormatSnapshotId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> snapshotStatusMap = Collections.emptyMap();
        if (!snapshotIds.isEmpty()) {
            snapshotStatusMap = formatSnapshotDao.selectList(new LambdaQueryWrapper<BasketDivisionFormatSnapshot>()
                            .in(BasketDivisionFormatSnapshot::getId, snapshotIds)
                            .eq(BasketDivisionFormatSnapshot::getIsDelete, BasketPublicConstants.NOT_DELETED))
                    .stream()
                    .collect(Collectors.toMap(BasketDivisionFormatSnapshot::getId,
                            BasketDivisionFormatSnapshot::getSnapshotStatus, (a, b) -> a));
        }
        List<V4PublicDivisionListItemResp> result = new ArrayList<>();
        for (BasketDivision division : divisions) {
            V4PublicDivisionListItemResp item = new V4PublicDivisionListItemResp();
            item.setDivisionId(division.getId());
            item.setDivisionName(division.getDivisionName());
            item.setPlatformCode(division.getPlatformCode());
            item.setGameMode(division.getGameMode());
            item.setRegistrationMode(division.getRegistrationMode());
            item.setDivisionStatus(division.getDivisionStatus());
            item.setRegistrationStartTime(division.getRegistrationStartTime());
            item.setRegistrationEndTime(division.getRegistrationEndTime());
            if (division.getFormatSnapshotId() != null) {
                item.setSnapshotStatus(snapshotStatusMap.get(division.getFormatSnapshotId()));
            }
            result.add(item);
        }
        return result;
    }

    @Override
    public List<V4PublicSeriesItemResp> listSeries() {
        List<BasketSeries> seriesList = seriesDao.selectList(new LambdaQueryWrapper<BasketSeries>()
                .eq(BasketSeries::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketSeries::getEnabled, 1)
                .orderByAsc(BasketSeries::getSortOrder)
                .orderByAsc(BasketSeries::getId));
        if (CollectionUtils.isEmpty(seriesList)) {
            return Collections.emptyList();
        }
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(collectSeriesFileIds(seriesList));
        List<V4PublicSeasonItemResp> publishedSeasons = listSeasons(null, null, null);
        Map<String, Long> seasonCountBySeriesCode = publishedSeasons.stream()
                .filter(s -> StringUtils.hasText(s.getSeriesCode()))
                .collect(Collectors.groupingBy(V4PublicSeasonItemResp::getSeriesCode, Collectors.counting()));

        List<V4PublicSeriesItemResp> result = new ArrayList<>();
        for (BasketSeries series : seriesList) {
            V4PublicSeriesItemResp vo = new V4PublicSeriesItemResp();
            vo.setSeriesId(series.getId());
            vo.setSeriesCode(series.getSeriesCode());
            vo.setSeriesName(series.getSeriesName());
            vo.setSeriesDesc(series.getSeriesDesc());
            vo.setIconFileId(series.getIconFileId());
            vo.setIconUrl(urlMap.get(series.getIconFileId()));
            vo.setCoverFileId(series.getCoverFileId());
            vo.setCoverUrl(urlMap.get(series.getCoverFileId()));
            vo.setBannerFileId(series.getBannerFileId());
            vo.setBannerUrl(urlMap.get(series.getBannerFileId()));
            vo.setEnabled(series.getEnabled());
            vo.setSeasonCount(seasonCountBySeriesCode.getOrDefault(series.getSeriesCode(), 0L).intValue());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<V4PublicSeasonItemResp> listSeasons(String seriesCode, Integer seasonYear, String seasonStatus) {
        LambdaQueryWrapper<BasketSeason> wrapper = new LambdaQueryWrapper<BasketSeason>()
                .eq(BasketSeason::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketSeason::getPublishStatus, PublishStatusEnum.PUBLISHED.getCode())
                .ne(BasketSeason::getPublishStatus, PublishStatusEnum.OFFLINE.getCode())
                .orderByDesc(BasketSeason::getSeasonYear)
                .orderByDesc(BasketSeason::getCreateTime);
        if (StringUtils.hasText(seriesCode)) {
            BasketSeries series = findSeriesByCode(seriesCode);
            if (series == null) {
                return Collections.emptyList();
            }
            wrapper.eq(BasketSeason::getSeriesId, series.getId());
        }
        if (seasonYear != null) {
            wrapper.eq(BasketSeason::getSeasonYear, seasonYear);
        }
        if (StringUtils.hasText(seasonStatus)) {
            wrapper.eq(BasketSeason::getSeasonStatus, seasonStatus);
        }
        List<BasketSeason> seasons = seasonDao.selectList(wrapper);
        if (CollectionUtils.isEmpty(seasons)) {
            return Collections.emptyList();
        }
        Map<Long, BasketSeries> seriesMap = loadSeriesMap(seasons.stream().map(BasketSeason::getSeriesId).collect(Collectors.toSet()));
        Map<Long, List<BasketDivision>> divisionMap = loadDivisionMapBySeasonIds(
                seasons.stream().map(BasketSeason::getId).collect(Collectors.toSet()));
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(collectSeasonFileIds(seasons));

        List<V4PublicSeasonItemResp> result = new ArrayList<>();
        for (BasketSeason season : seasons) {
            BasketSeries series = seriesMap.get(season.getSeriesId());
            V4PublicSeasonItemResp vo = toSeasonItem(season, series, divisionMap.get(season.getId()), urlMap);
            result.add(vo);
        }
        return result;
    }

    @Override
    public V4PublicSeasonDetailResp getSeasonDetail(Long seasonId) {
        BasketSeason season = requirePublishedSeason(seasonId);
        BasketSeries series = seriesDao.selectById(season.getSeriesId());
        List<BasketDivision> divisions = listDivisionsBySeasonId(season.getId());
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(collectSeasonFileIds(Collections.singletonList(season)));

        V4PublicSeasonDetailResp resp = new V4PublicSeasonDetailResp();
        copySeasonBase(season, series, resp, urlMap);
        resp.setDivisions(toDivisionBriefList(divisions));
        resp.setDefaultDivisionId(divisions.isEmpty() ? null : divisions.get(0).getId());

        int teamCount = 0;
        int matchCount = 0;
        int finishedCount = 0;
        for (BasketDivision division : divisions) {
            teamCount += countEventTeams(division.getId());
            matchCount += countPublishedMatches(division.getId(), null);
            finishedCount += countPublishedMatches(division.getId(), MatchStatusEnum.FINISHED.getCode());
        }
        resp.setEventTeamCount(teamCount);
        resp.setMatchCount(matchCount);
        resp.setFinishedMatchCount(finishedCount);
        return resp;
    }

    @Override
    public V4PublicDivisionDetailResp getDivisionDetail(Long divisionId) {
        BasketDivision division = requireDivision(divisionId);
        requirePublishedSeason(division.getSeasonId());
        BasketSeason season = seasonDao.selectById(division.getSeasonId());
        BasketSeries series = season != null ? seriesDao.selectById(season.getSeriesId()) : null;

        V4PublicDivisionDetailResp resp = new V4PublicDivisionDetailResp();
        resp.setDivisionId(division.getId());
        resp.setSeasonId(division.getSeasonId());
        resp.setSeriesCode(series != null ? series.getSeriesCode() : null);
        resp.setSeasonTitle(season != null ? resolveSeasonTitle(season) : null);
        resp.setPlatformCode(division.getPlatformCode());
        resp.setDivisionName(division.getDivisionName());
        resp.setRegistrationMode(division.getRegistrationMode());
        resp.setFormatType(division.getFormatType());
        resp.setMaxTeamCount(division.getMaxTeamCount());
        resp.setLineupMinCount(division.getLineupMinCount());
        resp.setLineupMaxCount(division.getLineupMaxCount());
        resp.setRegistrationStartTime(division.getRegistrationStartTime());
        resp.setRegistrationEndTime(division.getRegistrationEndTime());
        resp.setMatchStartTime(division.getMatchStartTime());
        resp.setMatchEndTime(division.getMatchEndTime());
        resp.setDivisionStatus(division.getDivisionStatus());
        resp.setEventTeamCount(countEventTeams(divisionId));
        resp.setMatchCount(countPublishedMatches(divisionId, null));
        resp.setFinishedMatchCount(countPublishedMatches(divisionId, MatchStatusEnum.FINISHED.getCode()));
        return resp;
    }

    @Override
    public List<V4PublicEventTeamItemResp> listTeams(Long divisionId) {
        requireDivision(divisionId);
        List<BasketEventTeam> teams = eventTeamDao.selectList(new LambdaQueryWrapper<BasketEventTeam>()
                .eq(BasketEventTeam::getDivisionId, divisionId)
                .eq(BasketEventTeam::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketEventTeam::getTeamNameSnapshot));
        if (CollectionUtils.isEmpty(teams)) {
            return Collections.emptyList();
        }
        Map<Long, String> captainMap = loadCaptainNameMap(teams);
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(
                teams.stream().map(BasketEventTeam::getTeamLogoFileId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<V4PublicEventTeamItemResp> result = new ArrayList<>();
        for (BasketEventTeam team : teams) {
            result.add(toEventTeamItem(team, captainMap, urlMap));
        }
        return result;
    }

    @Override
    public V4PublicEventTeamDetailResp getTeamDetail(Long eventTeamId) {
        BasketEventTeam team = requireEventTeam(eventTeamId);
        requirePublishedSeason(team.getSeasonId());
        Map<Long, String> captainMap = loadCaptainNameMap(Collections.singletonList(team));
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(
                Collections.singleton(team.getTeamLogoFileId()));

        V4PublicEventTeamDetailResp resp = new V4PublicEventTeamDetailResp();
        copyEventTeamBase(team, resp, captainMap, urlMap);
        resp.setPlayers(listPlayersByEventTeamId(eventTeamId));
        return resp;
    }

    @Override
    public List<V4PublicMatchItemResp> listMatches(Long divisionId, Integer roundNo, String matchStatus,
                                                   Boolean upcomingOnly, Boolean finishedOnly, Long eventTeamId) {
        requireDivision(divisionId);
        LambdaQueryWrapper<BasketV4Match> wrapper = new LambdaQueryWrapper<BasketV4Match>()
                .eq(BasketV4Match::getDivisionId, divisionId)
                .eq(BasketV4Match::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketV4Match::getPublishStatus, MatchPublishStatusEnum.PUBLISHED.getCode())
                .orderByAsc(BasketV4Match::getMatchTime)
                .orderByAsc(BasketV4Match::getRoundNo)
                .orderByAsc(BasketV4Match::getMatchNo);
        if (roundNo != null) {
            wrapper.eq(BasketV4Match::getRoundNo, roundNo);
        }
        if (StringUtils.hasText(matchStatus)) {
            wrapper.eq(BasketV4Match::getMatchStatus, matchStatus);
        }
        if (Boolean.TRUE.equals(upcomingOnly)) {
            wrapper.in(BasketV4Match::getMatchStatus,
                    MatchStatusEnum.SCHEDULED.getCode(), MatchStatusEnum.PLAYING.getCode());
        }
        if (Boolean.TRUE.equals(finishedOnly)) {
            wrapper.eq(BasketV4Match::getMatchStatus, MatchStatusEnum.FINISHED.getCode());
        }
        if (eventTeamId != null) {
            wrapper.and(w -> w.eq(BasketV4Match::getHomeEventTeamId, eventTeamId)
                    .or().eq(BasketV4Match::getAwayEventTeamId, eventTeamId));
        }
        List<BasketV4Match> matches = matchDao.selectList(wrapper);
        return enrichMatchItems(filterPublishedStageMatches(matches));
    }

    @Override
    public V4PublicMatchDetailResp getMatchDetail(Long matchId) {
        BasketV4Match match = requirePublishedMatch(matchId);
        requirePublishedSeason(match.getSeasonId());
        Map<Long, BasketEventTeam> teamMap = loadEventTeamMap(Arrays.asList(match.getHomeEventTeamId(), match.getAwayEventTeamId()));
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(teamMap.values().stream()
                .map(BasketEventTeam::getTeamLogoFileId).filter(Objects::nonNull).collect(Collectors.toSet()));

        V4PublicMatchDetailResp resp = new V4PublicMatchDetailResp();
        resp.setMatchId(match.getId());
        resp.setSeasonId(match.getSeasonId());
        resp.setDivisionId(match.getDivisionId());
        resp.setStageId(match.getStageId());
        resp.setRoundNo(match.getRoundNo());
        resp.setMatchNo(match.getMatchNo());
        resp.setMatchTime(match.getMatchTime());
        resp.setMatchStatus(match.getMatchStatus());
        resp.setMatchStatusName(resolveMatchStatusName(match.getMatchStatus()));
        resp.setPublishStatus(match.getPublishStatus());
        resp.setVenueText(match.getVenueText());
        resp.setHomeScore(match.getHomeScore());
        resp.setAwayScore(match.getAwayScore());
        resp.setWinnerEventTeamId(match.getWinnerEventTeamId());
        fillMatchContextNames(resp, match);

        Map<Long, String> captainMap = loadCaptainNameMap(new ArrayList<>(teamMap.values()));
        BasketEventTeam home = teamMap.get(match.getHomeEventTeamId());
        BasketEventTeam away = teamMap.get(match.getAwayEventTeamId());
        if (home != null) {
            V4PublicEventTeamDetailResp homeVo = new V4PublicEventTeamDetailResp();
            copyEventTeamBase(home, homeVo, captainMap, urlMap);
            homeVo.setPlayers(listPlayersByEventTeamId(home.getId()));
            resp.setHomeTeam(homeVo);
        }
        if (away != null) {
            V4PublicEventTeamDetailResp awayVo = new V4PublicEventTeamDetailResp();
            copyEventTeamBase(away, awayVo, captainMap, urlMap);
            awayVo.setPlayers(listPlayersByEventTeamId(away.getId()));
            resp.setAwayTeam(awayVo);
        }

        resp.setTeamStats(listMatchTeamStats(matchId, teamMap));
        resp.setPlayerStats(listMatchPlayerStats(matchId, teamMap));
        if (match.getSeriesId() != null) {
            resp.setMatchSeriesId(match.getSeriesId());
            resp.setSeriesGameNo(match.getSeriesGameNo());
            V4PublicMatchSeriesDetailResp seriesContext = matchSeriesDisplaySupport.buildDetail(match.getSeriesId(), matchId);
            if (seriesContext != null) {
                resp.setBestOf(seriesContext.getBestOf());
                resp.setSeriesContext(seriesContext);
            }
        }
        return resp;
    }

    @Override
    public List<V4PublicStandingItemResp> listStanding(Long divisionId) {
        requireDivision(divisionId);
        List<BasketStanding> rows = standingDao.selectList(new LambdaQueryWrapper<BasketStanding>()
                .eq(BasketStanding::getDivisionId, divisionId)
                .eq(BasketStanding::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketStanding::getRankNo));
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        Map<Long, BasketEventTeam> teamMap = loadEventTeamMap(
                rows.stream().map(BasketStanding::getEventTeamId).collect(Collectors.toList()));
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(teamMap.values().stream()
                .map(BasketEventTeam::getTeamLogoFileId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<V4PublicStandingItemResp> result = new ArrayList<>();
        for (BasketStanding row : rows) {
            V4PublicStandingItemResp vo = new V4PublicStandingItemResp();
            vo.setRankNo(row.getRankNo());
            vo.setEventTeamId(row.getEventTeamId());
            vo.setWinCount(row.getWinCount());
            vo.setLoseCount(row.getLoseCount());
            vo.setPointFor(row.getPointFor());
            vo.setPointAgainst(row.getPointAgainst());
            vo.setPointDiff(row.getPointDiff());
            vo.setStandingPoints(row.getStandingPoints());
            BasketEventTeam team = teamMap.get(row.getEventTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamNameSnapshot());
                vo.setTeamLogoFileId(team.getTeamLogoFileId());
                vo.setTeamLogoUrl(urlMap.get(team.getTeamLogoFileId()));
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<V4PublicStageItemResp> listStages(Long divisionId) {
        requireDivision(divisionId);
        List<V4PublicStageItemResp> stages = publicStageMapper.selectStagesByDivisionId(divisionId);
        if (CollectionUtils.isEmpty(stages)) {
            return Collections.emptyList();
        }
        for (V4PublicStageItemResp stage : stages) {
            stage.setStageTypeName(resolveStageTypeName(stage.getStageType()));
            stage.setOrganizationModeName(resolveOrganizationModeName(stage.getOrganizationMode()));
            stage.setCompetitionFormatName(resolveCompetitionFormatName(stage.getCompetitionFormat()));
            stage.setExecutionStatusName(resolveExecutionStatusName(stage.getExecutionStatus()));
        }
        return stages;
    }

    @Override
    public List<V4PublicStageStandingScopeResp> listStageStandings(Long stageId) {
        if (stageId == null) {
            throw new ServiceException("stageId 不能为空");
        }
        V4PublicStageItemResp stage = publicStageMapper.selectStageById(stageId);
        Integer qualifyCount = resolveStageQualifyCount(stage);
        List<Long> scopeKeys = publicStageMapper.selectGroupScopeKeysByStageId(stageId);
        if (CollectionUtils.isEmpty(scopeKeys)) {
            scopeKeys = Collections.singletonList(0L);
        }
        List<V4PublicStageStandingScopeResp> scopes = new ArrayList<>();
        for (Long scopeKey : scopeKeys) {
            List<V4PublicStandingItemResp> rows = publicStageMapper.selectStandingsByStageScope(stageId, scopeKey);
            if (CollectionUtils.isEmpty(rows)) {
                continue;
            }
            enrichStandingTeamInfo(rows);
            V4PublicStageStandingScopeResp scope = new V4PublicStageStandingScopeResp();
            scope.setGroupScopeKey(scopeKey);
            scope.setGroupId(scopeKey != null && scopeKey > 0 ? scopeKey : null);
            if (scopeKey != null && scopeKey > 0) {
                // group name via first row's groupId lookup omitted for brevity; frontend can show code
            }
            scope.setQualifyCount(qualifyCount);
            scope.setRows(rows);
            scopes.add(scope);
        }
        return scopes;
    }

    private Integer resolveStageQualifyCount(V4PublicStageItemResp stage) {
        if (stage == null || stage.getAdvancementCount() == null || stage.getAdvancementCount() <= 0) {
            return null;
        }
        String type = stage.getAdvancementType() == null ? "" : stage.getAdvancementType().trim().toUpperCase();
        if ("OVERALL_TOP_N".equals(type) || "GROUP_TOP_N".equals(type) || "TOP_N".equals(type)) {
            return stage.getAdvancementCount();
        }
        return null;
    }

    @Override
    public List<V4PublicMatchItemResp> listStageBracket(Long stageId) {
        if (stageId == null) {
            throw new ServiceException("stageId 不能为空");
        }
        List<BasketV4Match> matches = matchDao.selectList(new LambdaQueryWrapper<BasketV4Match>()
                .eq(BasketV4Match::getStageId, stageId)
                .eq(BasketV4Match::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketV4Match::getPublishStatus, MatchPublishStatusEnum.PUBLISHED.getCode())
                .orderByAsc(BasketV4Match::getRoundNo)
                .orderByAsc(BasketV4Match::getMatchNo));
        if (CollectionUtils.isEmpty(matches)) {
            return Collections.emptyList();
        }
        return enrichMatchItems(filterPublishedStageMatches(matches));
    }

    @Override
    public List<V4PublicMatchSeriesDetailResp> listStageSeries(Long stageId) {
        if (stageId == null) {
            throw new ServiceException("stageId 不能为空");
        }
        return matchSeriesDisplaySupport.listByStageId(stageId);
    }

    @Override
    public V4PublicMatchSeriesDetailResp getMatchSeriesDetail(Long seriesId) {
        if (seriesId == null) {
            throw new ServiceException("seriesId 不能为空");
        }
        V4PublicMatchSeriesDetailResp detail = matchSeriesDisplaySupport.buildDetail(seriesId, null);
        if (detail == null) {
            throw new ServiceException("系列赛不存在");
        }
        return detail;
    }

    @Override
    public List<V4PublicScheduleItemResp> listDivisionSchedule(Long divisionId) {
        requireDivision(divisionId);
        return matchSeriesDisplaySupport.listScheduleByDivisionId(divisionId);
    }

    private void enrichStandingTeamInfo(List<V4PublicStandingItemResp> rows) {
        Map<Long, BasketEventTeam> teamMap = loadEventTeamMap(
                rows.stream().map(V4PublicStandingItemResp::getEventTeamId).collect(Collectors.toList()));
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(teamMap.values().stream()
                .map(BasketEventTeam::getTeamLogoFileId).filter(Objects::nonNull).collect(Collectors.toSet()));
        for (V4PublicStandingItemResp row : rows) {
            BasketEventTeam team = teamMap.get(row.getEventTeamId());
            if (team != null) {
                row.setTeamName(team.getTeamNameSnapshot());
                row.setTeamLogoFileId(team.getTeamLogoFileId());
                row.setTeamLogoUrl(urlMap.get(team.getTeamLogoFileId()));
            }
        }
    }

    private String resolveStageTypeName(String stageType) {
        if (!StringUtils.hasText(stageType)) return "阶段";
        return switch (stageType.toUpperCase()) {
            case "REGULAR" -> "循环赛";
            case "GROUP" -> "小组赛";
            case "KNOCKOUT" -> "淘汰赛";
            default -> stageType;
        };
    }

    private String resolveOrganizationModeName(String organizationMode) {
        if (!StringUtils.hasText(organizationMode)) {
            return "不分组";
        }
        return switch (organizationMode.toUpperCase()) {
            case "GROUPED" -> "分组";
            case "UNGROUPED" -> "不分组";
            default -> organizationMode;
        };
    }

    private String resolveCompetitionFormatName(String format) {
        if (!StringUtils.hasText(format)) return "";
        return switch (format.toUpperCase()) {
            case "ROUND_ROBIN" -> "单循环";
            case "SINGLE_ELIMINATION" -> "单败淘汰";
            case "DOUBLE_ELIMINATION" -> "双败淘汰（预留 / Demo）";
            default -> format;
        };
    }

    private String resolveExecutionStatusName(String status) {
        if (!StringUtils.hasText(status)) return "";
        return switch (status.toUpperCase()) {
            case "RUNNING" -> "进行中";
            case "RESULT_PENDING_CONFIRM" -> "待确认晋级";
            case "COMPLETED" -> "已完成";
            case "SCHEDULE_GENERATED" -> "赛程已生成";
            default -> status;
        };
    }

    @Override
    public List<V4PublicTeamStatItemResp> listTeamStat(Long divisionId) {
        requireDivision(divisionId);
        List<BasketTeamStat> rows = teamStatDao.selectList(new LambdaQueryWrapper<BasketTeamStat>()
                .eq(BasketTeamStat::getDivisionId, divisionId)
                .eq(BasketTeamStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByDesc(BasketTeamStat::getPointsFor));
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        Map<Long, BasketEventTeam> teamMap = loadEventTeamMap(
                rows.stream().map(BasketTeamStat::getEventTeamId).collect(Collectors.toList()));
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(teamMap.values().stream()
                .map(BasketEventTeam::getTeamLogoFileId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<V4PublicTeamStatItemResp> result = new ArrayList<>();
        for (BasketTeamStat row : rows) {
            V4PublicTeamStatItemResp vo = new V4PublicTeamStatItemResp();
            vo.setEventTeamId(row.getEventTeamId());
            vo.setMatchCount(row.getMatchCount());
            vo.setWinCount(row.getWinCount());
            vo.setLoseCount(row.getLoseCount());
            vo.setPointsFor(row.getPointsFor());
            vo.setPointsAgainst(row.getPointsAgainst());
            vo.setAvgPointsFor(row.getAvgPointsFor());
            vo.setAvgPointsAgainst(row.getAvgPointsAgainst());
            vo.setRebound(row.getRebound());
            vo.setAssist(row.getAssist());
            vo.setSteal(row.getSteal());
            vo.setBlock(row.getBlock());
            vo.setTurnover(row.getTurnover());
            BasketEventTeam team = teamMap.get(row.getEventTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamNameSnapshot());
                vo.setTeamLogoFileId(team.getTeamLogoFileId());
                vo.setTeamLogoUrl(urlMap.get(team.getTeamLogoFileId()));
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<V4PublicPlayerStatItemResp> listPlayerStat(Long divisionId) {
        requireDivision(divisionId);
        List<BasketPlayerStat> rows = playerStatDao.selectList(new LambdaQueryWrapper<BasketPlayerStat>()
                .eq(BasketPlayerStat::getDivisionId, divisionId)
                .eq(BasketPlayerStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByDesc(BasketPlayerStat::getPoints));
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        Map<Long, BasketEventPlayer> playerMap = loadEventPlayerMap(
                rows.stream().map(BasketPlayerStat::getEventPlayerId).collect(Collectors.toList()));
        Map<Long, BasketEventTeam> teamMap = loadEventTeamMap(
                rows.stream().map(BasketPlayerStat::getEventTeamId).collect(Collectors.toList()));
        List<V4PublicPlayerStatItemResp> result = new ArrayList<>();
        for (BasketPlayerStat row : rows) {
            V4PublicPlayerStatItemResp vo = new V4PublicPlayerStatItemResp();
            vo.setEventPlayerId(row.getEventPlayerId());
            vo.setMatchCount(row.getMatchCount());
            vo.setPoints(row.getPoints());
            vo.setAvgPoints(row.getAvgPoints());
            vo.setRebound(row.getRebound());
            vo.setAvgRebound(row.getAvgRebound());
            vo.setAssist(row.getAssist());
            vo.setAvgAssist(row.getAvgAssist());
            vo.setSteal(row.getSteal());
            vo.setBlock(row.getBlock());
            vo.setTurnover(row.getTurnover());
            BasketEventPlayer player = playerMap.get(row.getEventPlayerId());
            if (player != null) {
                vo.setSourcePlayerId(player.getSourcePlayerId());
                vo.setPlayerName(player.getPlayerNameSnapshot());
            }
            BasketEventTeam team = teamMap.get(row.getEventTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamNameSnapshot());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<V4PublicRankItemResp> listRank(Long divisionId, String rankType) {
        requireDivision(divisionId);
        LambdaQueryWrapper<BasketRank> wrapper = new LambdaQueryWrapper<BasketRank>()
                .eq(BasketRank::getDivisionId, divisionId)
                .eq(BasketRank::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketRank::getRankNo);
        if (StringUtils.hasText(rankType)) {
            wrapper.eq(BasketRank::getRankType, rankType);
        }
        List<BasketRank> rows = rankDao.selectList(wrapper);
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        Map<Long, BasketEventTeam> teamMap = loadEventTeamMap(
                rows.stream().map(BasketRank::getEventTeamId).filter(Objects::nonNull).collect(Collectors.toList()));
        Map<Long, BasketEventPlayer> playerMap = loadEventPlayerMap(
                rows.stream().map(BasketRank::getEventPlayerId).filter(Objects::nonNull).collect(Collectors.toList()));
        List<V4PublicRankItemResp> result = new ArrayList<>();
        for (BasketRank row : rows) {
            V4PublicRankItemResp vo = new V4PublicRankItemResp();
            vo.setRankNo(row.getRankNo());
            vo.setRankType(row.getRankType());
            vo.setTargetType(row.getTargetType());
            vo.setEventTeamId(row.getEventTeamId());
            vo.setEventPlayerId(row.getEventPlayerId());
            vo.setRankValue(row.getRankValue());
            if (row.getEventTeamId() != null) {
                BasketEventTeam team = teamMap.get(row.getEventTeamId());
                if (team != null) {
                    vo.setTeamName(team.getTeamNameSnapshot());
                }
            }
            if (row.getEventPlayerId() != null) {
                BasketEventPlayer player = playerMap.get(row.getEventPlayerId());
                if (player != null) {
                    vo.setSourcePlayerId(player.getSourcePlayerId());
                    vo.setPlayerName(player.getPlayerNameSnapshot());
                }
            }
            result.add(vo);
        }
        return result;
    }

    // ---- helpers ----

    private BasketSeries findSeriesByCode(String seriesCode) {
        return seriesDao.selectOne(new LambdaQueryWrapper<BasketSeries>()
                .eq(BasketSeries::getSeriesCode, seriesCode)
                .eq(BasketSeries::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .last("limit 1"));
    }

    private Map<Long, BasketSeries> loadSeriesMap(Set<Long> seriesIds) {
        if (CollectionUtils.isEmpty(seriesIds)) {
            return Collections.emptyMap();
        }
        List<BasketSeries> list = seriesDao.selectByIds(seriesIds);
        Map<Long, BasketSeries> map = new HashMap<>();
        for (BasketSeries item : list) {
            if (item != null && Objects.equals(item.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    private Map<Long, List<BasketDivision>> loadDivisionMapBySeasonIds(Set<Long> seasonIds) {
        if (CollectionUtils.isEmpty(seasonIds)) {
            return Collections.emptyMap();
        }
        List<BasketDivision> divisions = divisionDao.selectList(new LambdaQueryWrapper<BasketDivision>()
                .in(BasketDivision::getSeasonId, seasonIds)
                .eq(BasketDivision::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketDivision::getSortOrder)
                .orderByAsc(BasketDivision::getId));
        return divisions.stream().collect(Collectors.groupingBy(BasketDivision::getSeasonId));
    }

    private List<BasketDivision> listDivisionsBySeasonId(Long seasonId) {
        return divisionDao.selectList(new LambdaQueryWrapper<BasketDivision>()
                .eq(BasketDivision::getSeasonId, seasonId)
                .eq(BasketDivision::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketDivision::getSortOrder)
                .orderByAsc(BasketDivision::getId));
    }

    private List<V4PublicDivisionBriefResp> toDivisionBriefList(List<BasketDivision> divisions) {
        if (CollectionUtils.isEmpty(divisions)) {
            return Collections.emptyList();
        }
        List<V4PublicDivisionBriefResp> list = new ArrayList<>();
        for (BasketDivision division : divisions) {
            V4PublicDivisionBriefResp vo = new V4PublicDivisionBriefResp();
            vo.setDivisionId(division.getId());
            vo.setPlatformCode(division.getPlatformCode());
            vo.setDivisionName(division.getDivisionName());
            vo.setDivisionStatus(division.getDivisionStatus());
            vo.setMaxTeamCount(division.getMaxTeamCount());
            vo.setLineupMinCount(division.getLineupMinCount());
            vo.setLineupMaxCount(division.getLineupMaxCount());
            list.add(vo);
        }
        return list;
    }

    private V4PublicSeasonItemResp toSeasonItem(BasketSeason season, BasketSeries series,
                                                  List<BasketDivision> divisions, Map<Long, String> urlMap) {
        V4PublicSeasonItemResp vo = new V4PublicSeasonItemResp();
        vo.setSeasonId(season.getId());
        vo.setSeriesId(season.getSeriesId());
        if (series != null) {
            vo.setSeriesCode(series.getSeriesCode());
            vo.setSeriesName(series.getSeriesName());
        }
        vo.setSeasonYear(season.getSeasonYear());
        vo.setSeasonName(season.getSeasonName());
        vo.setDisplayTitle(resolveSeasonTitle(season));
        vo.setDescription(season.getDescription());
        vo.setCoverFileId(season.getCoverFileId());
        vo.setCoverUrl(urlMap.get(season.getCoverFileId()));
        vo.setBannerFileId(season.getBannerFileId());
        vo.setBannerUrl(urlMap.get(season.getBannerFileId()));
        vo.setRegistrationMode(season.getRegistrationMode());
        vo.setFormatType(season.getFormatType());
        vo.setRegistrationStartTime(season.getRegistrationStartTime());
        vo.setRegistrationEndTime(season.getRegistrationEndTime());
        vo.setMatchStartTime(season.getMatchStartTime());
        vo.setMatchEndTime(season.getMatchEndTime());
        vo.setSeasonStatus(season.getSeasonStatus());
        vo.setDivisions(toDivisionBriefList(divisions));
        applyDivisionAggregates(vo, divisions);
        return vo;
    }

    private void applyDivisionAggregates(V4PublicSeasonItemResp vo, List<BasketDivision> divisions) {
        if (CollectionUtils.isEmpty(divisions)) {
            vo.setDivisionCount(0);
            vo.setPlatformCodes(Collections.emptyList());
            vo.setGameModes(Collections.emptyList());
            vo.setRegistrationModes(Collections.emptyList());
            vo.setRegistrationOpenDivisionCount(0);
            vo.setNearestRegistrationEndTime(null);
            return;
        }
        vo.setDivisionCount(divisions.size());
        LinkedHashSet<String> platformCodes = new LinkedHashSet<>();
        LinkedHashSet<String> gameModes = new LinkedHashSet<>();
        LinkedHashSet<String> registrationModes = new LinkedHashSet<>();
        int registrationOpenCount = 0;
        Date nearestRegistrationEndTime = null;
        String registrationOpenCode = DivisionStatusEnum.REGISTRATION_OPEN.getCode();
        for (BasketDivision division : divisions) {
            if (StringUtils.hasText(division.getPlatformCode())) {
                platformCodes.add(division.getPlatformCode());
            }
            if (StringUtils.hasText(division.getGameMode())) {
                gameModes.add(division.getGameMode());
            }
            if (StringUtils.hasText(division.getRegistrationMode())) {
                registrationModes.add(division.getRegistrationMode());
            }
            if (registrationOpenCode.equals(division.getDivisionStatus())) {
                registrationOpenCount++;
                Date endTime = division.getRegistrationEndTime();
                if (endTime != null && (nearestRegistrationEndTime == null || endTime.before(nearestRegistrationEndTime))) {
                    nearestRegistrationEndTime = endTime;
                }
            }
        }
        vo.setPlatformCodes(new ArrayList<>(platformCodes));
        vo.setGameModes(new ArrayList<>(gameModes));
        vo.setRegistrationModes(new ArrayList<>(registrationModes));
        vo.setRegistrationOpenDivisionCount(registrationOpenCount);
        vo.setNearestRegistrationEndTime(nearestRegistrationEndTime);
    }

    private void copySeasonBase(BasketSeason season, BasketSeries series, V4PublicSeasonDetailResp resp,
                                Map<Long, String> urlMap) {
        resp.setSeasonId(season.getId());
        resp.setSeriesId(season.getSeriesId());
        if (series != null) {
            resp.setSeriesCode(series.getSeriesCode());
            resp.setSeriesName(series.getSeriesName());
        }
        resp.setSeasonYear(season.getSeasonYear());
        resp.setSeasonName(season.getSeasonName());
        resp.setDisplayTitle(resolveSeasonTitle(season));
        resp.setDescription(season.getDescription());
        resp.setCoverFileId(season.getCoverFileId());
        resp.setCoverUrl(urlMap.get(season.getCoverFileId()));
        resp.setBannerFileId(season.getBannerFileId());
        resp.setBannerUrl(urlMap.get(season.getBannerFileId()));
        resp.setRegistrationMode(season.getRegistrationMode());
        resp.setFormatType(season.getFormatType());
        resp.setRegistrationStartTime(season.getRegistrationStartTime());
        resp.setRegistrationEndTime(season.getRegistrationEndTime());
        resp.setMatchStartTime(season.getMatchStartTime());
        resp.setMatchEndTime(season.getMatchEndTime());
        resp.setSeasonStatus(season.getSeasonStatus());
    }

    private String resolveSeasonTitle(BasketSeason season) {
        if (StringUtils.hasText(season.getDisplayTitle())) {
            return season.getDisplayTitle();
        }
        if (StringUtils.hasText(season.getSeasonName())) {
            return season.getSeasonName();
        }
        return season.getSeasonYear() != null ? season.getSeasonYear() + " 赛季" : "赛季";
    }

    private Collection<Long> collectSeriesFileIds(List<BasketSeries> list) {
        Set<Long> ids = new HashSet<>();
        for (BasketSeries item : list) {
            if (item.getIconFileId() != null) ids.add(item.getIconFileId());
            if (item.getCoverFileId() != null) ids.add(item.getCoverFileId());
            if (item.getBannerFileId() != null) ids.add(item.getBannerFileId());
        }
        return ids;
    }

    private Collection<Long> collectSeasonFileIds(List<BasketSeason> list) {
        Set<Long> ids = new HashSet<>();
        for (BasketSeason item : list) {
            if (item.getCoverFileId() != null) ids.add(item.getCoverFileId());
            if (item.getBannerFileId() != null) ids.add(item.getBannerFileId());
        }
        return ids;
    }

    private BasketSeason requirePublishedSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("赛季主键不能为空");
        }
        BasketSeason season = seasonDao.selectById(seasonId);
        if (season == null || !Objects.equals(season.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
            throw new ServiceException("赛季不存在");
        }
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(season.getPublishStatus())) {
            throw new ServiceException("赛季未发布或已下架");
        }
        return season;
    }

    private BasketDivision requireDivision(Long divisionId) {
        if (divisionId == null) {
            throw new ServiceException("赛区主键不能为空");
        }
        BasketDivision division = divisionDao.selectById(divisionId);
        if (division == null || !Objects.equals(division.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
            throw new ServiceException("赛区不存在");
        }
        requirePublishedSeason(division.getSeasonId());
        return division;
    }

    private BasketEventTeam requireEventTeam(Long eventTeamId) {
        if (eventTeamId == null) {
            throw new ServiceException("参赛球队主键不能为空");
        }
        BasketEventTeam team = eventTeamDao.selectById(eventTeamId);
        if (team == null || !Objects.equals(team.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
            throw new ServiceException("参赛球队不存在");
        }
        return team;
    }

    private BasketV4Match requirePublishedMatch(Long matchId) {
        if (matchId == null) {
            throw new ServiceException("比赛主键不能为空");
        }
        BasketV4Match match = matchDao.selectById(matchId);
        if (match == null || !Objects.equals(match.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
            throw new ServiceException("比赛不存在");
        }
        if (!MatchPublishStatusEnum.PUBLISHED.getCode().equals(match.getPublishStatus())) {
            throw new ServiceException("比赛未发布");
        }
        if (!matchSeriesDisplaySupport.isPubliclyVisibleMatch(match)) {
            throw new ServiceException("比赛未发布");
        }
        return match;
    }

    private int countEventTeams(Long divisionId) {
        Long count = eventTeamDao.selectCount(new LambdaQueryWrapper<BasketEventTeam>()
                .eq(BasketEventTeam::getDivisionId, divisionId)
                .eq(BasketEventTeam::getIsDelete, BasketPublicConstants.NOT_DELETED));
        return count != null ? count.intValue() : 0;
    }

    private int countPublishedMatches(Long divisionId, String matchStatus) {
        LambdaQueryWrapper<BasketV4Match> wrapper = new LambdaQueryWrapper<BasketV4Match>()
                .eq(BasketV4Match::getDivisionId, divisionId)
                .eq(BasketV4Match::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketV4Match::getPublishStatus, MatchPublishStatusEnum.PUBLISHED.getCode());
        if (StringUtils.hasText(matchStatus)) {
            wrapper.eq(BasketV4Match::getMatchStatus, matchStatus);
        }
        Long count = matchDao.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
    }

    private Map<Long, String> loadCaptainNameMap(List<BasketEventTeam> teams) {
        if (CollectionUtils.isEmpty(teams)) {
            return Collections.emptyMap();
        }
        Set<Long> teamIds = teams.stream().map(BasketEventTeam::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (teamIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketEventPlayer> captains = eventPlayerDao.selectList(new LambdaQueryWrapper<BasketEventPlayer>()
                .in(BasketEventPlayer::getEventTeamId, teamIds)
                .eq(BasketEventPlayer::getIsCaptain, 1)
                .eq(BasketEventPlayer::getIsDelete, BasketPublicConstants.NOT_DELETED));
        Map<Long, String> map = new HashMap<>();
        for (BasketEventPlayer captain : captains) {
            map.put(captain.getEventTeamId(), captain.getPlayerNameSnapshot());
        }
        return map;
    }

    private V4PublicEventTeamItemResp toEventTeamItem(BasketEventTeam team, Map<Long, String> captainMap,
                                                      Map<Long, String> urlMap) {
        V4PublicEventTeamItemResp vo = new V4PublicEventTeamItemResp();
        copyEventTeamBase(team, vo, captainMap, urlMap);
        return vo;
    }

    private void copyEventTeamBase(BasketEventTeam team, V4PublicEventTeamItemResp vo,
                                   Map<Long, String> captainMap, Map<Long, String> urlMap) {
        vo.setEventTeamId(team.getId());
        vo.setSourceTeamId(team.getSourceTeamId());
        vo.setTeamName(team.getTeamNameSnapshot());
        vo.setTeamShortName(team.getTeamShortName());
        vo.setTeamLogoFileId(team.getTeamLogoFileId());
        vo.setTeamLogoUrl(urlMap.get(team.getTeamLogoFileId()));
        vo.setCaptainPlayerId(team.getCaptainPlayerId());
        vo.setCaptainName(captainMap.get(team.getId()));
        vo.setPlatformCode(team.getPlatformCode());
        vo.setEventTeamStatus(team.getEventTeamStatus());
        vo.setTemporaryTeam(PublicTeamDisplaySupport.isTemporary(team));
    }

    private List<V4PublicEventPlayerItemResp> listPlayersByEventTeamId(Long eventTeamId) {
        List<BasketEventPlayer> players = eventPlayerDao.selectList(new LambdaQueryWrapper<BasketEventPlayer>()
                .eq(BasketEventPlayer::getEventTeamId, eventTeamId)
                .eq(BasketEventPlayer::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByDesc(BasketEventPlayer::getIsCaptain)
                .orderByDesc(BasketEventPlayer::getIsStarter)
                .orderByAsc(BasketEventPlayer::getJerseyNo));
        if (CollectionUtils.isEmpty(players)) {
            return Collections.emptyList();
        }
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(
                players.stream().map(BasketEventPlayer::getAvatarFileId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<V4PublicEventPlayerItemResp> result = new ArrayList<>();
        for (BasketEventPlayer player : players) {
            V4PublicEventPlayerItemResp vo = new V4PublicEventPlayerItemResp();
            vo.setEventPlayerId(player.getId());
            vo.setSourcePlayerId(player.getSourcePlayerId());
            vo.setPlayerName(player.getPlayerNameSnapshot());
            vo.setAvatarFileId(player.getAvatarFileId());
            vo.setAvatarUrl(urlMap.get(player.getAvatarFileId()));
            vo.setGameIdSnapshot(player.getGameIdSnapshot());
            vo.setPlatformCode(player.getPlatformCode());
            vo.setPosition(player.getPosition());
            vo.setJerseyNo(player.getJerseyNo());
            vo.setIsCaptain(player.getIsCaptain());
            vo.setIsStarter(player.getIsStarter());
            result.add(vo);
        }
        return result;
    }

    private List<BasketV4Match> filterPublishedStageMatches(List<BasketV4Match> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return matches;
        }
        List<BasketV4Match> kept = new ArrayList<>();
        for (BasketV4Match match : matches) {
            if (matchSeriesDisplaySupport.isPubliclyVisibleMatch(match)) {
                kept.add(match);
            }
        }
        return kept;
    }

    private List<V4PublicMatchItemResp> enrichMatchItems(List<BasketV4Match> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return Collections.emptyList();
        }
        Set<Long> teamIds = new HashSet<>();
        for (BasketV4Match match : matches) {
            if (match.getHomeEventTeamId() != null && match.getHomeEventTeamId() > 0) teamIds.add(match.getHomeEventTeamId());
            if (match.getAwayEventTeamId() != null && match.getAwayEventTeamId() > 0) teamIds.add(match.getAwayEventTeamId());
        }
        Map<Long, BasketEventTeam> teamMap = loadEventTeamMap(new ArrayList<>(teamIds));
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(teamMap.values().stream()
                .map(BasketEventTeam::getTeamLogoFileId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Set<Long> matchSeriesIds = matches.stream()
                .map(BasketV4Match::getSeriesId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BasketMatchSeries> seriesMap = matchSeriesDisplaySupport.loadSeriesMap(matchSeriesIds);
        List<V4PublicMatchItemResp> result = new ArrayList<>();
        for (BasketV4Match match : matches) {
            V4PublicMatchItemResp vo = new V4PublicMatchItemResp();
            vo.setMatchId(match.getId());
            vo.setSeasonId(match.getSeasonId());
            vo.setDivisionId(match.getDivisionId());
            vo.setRoundNo(match.getRoundNo());
            vo.setMatchNo(match.getMatchNo());
            vo.setMatchTime(match.getMatchTime());
            if (match.getHomeEventTeamId() != null && match.getHomeEventTeamId() <= 0) {
                vo.setHomeEventTeamId(null);
            }
            if (match.getAwayEventTeamId() != null && match.getAwayEventTeamId() <= 0) {
                vo.setAwayEventTeamId(null);
            }
            vo.setHomeEventTeamId(match.getHomeEventTeamId());
            vo.setAwayEventTeamId(match.getAwayEventTeamId());
            vo.setHomeScore(match.getHomeScore());
            vo.setAwayScore(match.getAwayScore());
            vo.setWinnerEventTeamId(match.getWinnerEventTeamId());
            vo.setMatchStatus(match.getMatchStatus());
            vo.setPublishStatus(match.getPublishStatus());
            vo.setVenueText(match.getVenueText());
            if (match.getSeriesId() != null) {
                vo.setMatchSeriesId(match.getSeriesId());
                vo.setSeriesGameNo(match.getSeriesGameNo());
                BasketMatchSeries ms = seriesMap.get(match.getSeriesId());
                if (ms != null) {
                    vo.setBestOf(ms.getBestOf());
                    vo.setSeriesHomeWinCount(ms.getHomeWinCount());
                    vo.setSeriesAwayWinCount(ms.getAwayWinCount());
                    vo.setSeriesStatus(ms.getSeriesStatus());
                    vo.setSeriesStatusName(seriesStatusLabel(ms.getSeriesStatus()));
                    vo.setHomeSourceType(ms.getHomeSourceType());
                    vo.setAwaySourceType(ms.getAwaySourceType());
                    vo.setHomeSourceDisplay(ms.getHomeSourceDisplay());
                    vo.setAwaySourceDisplay(ms.getAwaySourceDisplay());
                    if (vo.getMatchTime() == null && ms.getScheduledDate() != null && ms.getScheduledStartTime() != null) {
                        vo.setMatchTime(java.util.Date.from(java.time.LocalDateTime.of(ms.getScheduledDate(), ms.getScheduledStartTime())
                                .atZone(java.time.ZoneId.of("Asia/Shanghai")).toInstant()));
                    }
                }
            }
            BasketEventTeam home = teamMap.get(match.getHomeEventTeamId());
            BasketEventTeam away = teamMap.get(match.getAwayEventTeamId());
            if (home != null) {
                vo.setHomeTeamName(home.getTeamNameSnapshot());
                vo.setHomeTeamLogoFileId(home.getTeamLogoFileId());
                vo.setHomeTeamLogoUrl(urlMap.get(home.getTeamLogoFileId()));
            } else if (vo.getHomeSourceDisplay() != null && !vo.getHomeSourceDisplay().isBlank()) {
                vo.setHomeTeamName(vo.getHomeSourceDisplay());
            } else {
                vo.setHomeTeamName("待定");
            }
            if (away != null) {
                vo.setAwayTeamName(away.getTeamNameSnapshot());
                vo.setAwayTeamLogoFileId(away.getTeamLogoFileId());
                vo.setAwayTeamLogoUrl(urlMap.get(away.getTeamLogoFileId()));
            } else if (vo.getAwaySourceDisplay() != null && !vo.getAwaySourceDisplay().isBlank()) {
                vo.setAwayTeamName(vo.getAwaySourceDisplay());
            } else {
                vo.setAwayTeamName("待定");
            }
            result.add(vo);
        }
        return result;
    }

    private Map<Long, BasketEventTeam> loadEventTeamMap(Collection<Long> teamIds) {
        if (CollectionUtils.isEmpty(teamIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = teamIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketEventTeam> list = eventTeamDao.selectByIds(ids);
        Map<Long, BasketEventTeam> map = new HashMap<>();
        for (BasketEventTeam item : list) {
            if (item != null && Objects.equals(item.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    private Map<Long, BasketEventPlayer> loadEventPlayerMap(Collection<Long> playerIds) {
        if (CollectionUtils.isEmpty(playerIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = playerIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketEventPlayer> list = eventPlayerDao.selectByIds(ids);
        Map<Long, BasketEventPlayer> map = new HashMap<>();
        for (BasketEventPlayer item : list) {
            if (item != null && Objects.equals(item.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    private List<V4PublicMatchTeamStatItemResp> listMatchTeamStats(Long matchId, Map<Long, BasketEventTeam> teamMap) {
        List<BasketV4MatchTeamStat> rows = matchTeamStatDao.selectList(new LambdaQueryWrapper<BasketV4MatchTeamStat>()
                .eq(BasketV4MatchTeamStat::getMatchId, matchId)
                .eq(BasketV4MatchTeamStat::getIsDelete, BasketPublicConstants.NOT_DELETED));
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        List<V4PublicMatchTeamStatItemResp> result = new ArrayList<>();
        for (BasketV4MatchTeamStat row : rows) {
            V4PublicMatchTeamStatItemResp vo = new V4PublicMatchTeamStatItemResp();
            vo.setEventTeamId(row.getEventTeamId());
            vo.setIsWinner(row.getIsWinner());
            vo.setScore(row.getScore());
            vo.setPointsFor(row.getPointsFor());
            vo.setPointsAgainst(row.getPointsAgainst());
            vo.setFieldGoalMade(row.getFieldGoalMade());
            vo.setFieldGoalAttempt(row.getFieldGoalAttempt());
            vo.setThreePointMade(row.getThreePointMade());
            vo.setThreePointAttempt(row.getThreePointAttempt());
            vo.setFreeThrowMade(row.getFreeThrowMade());
            vo.setFreeThrowAttempt(row.getFreeThrowAttempt());
            vo.setRebound(row.getRebound());
            vo.setAssist(row.getAssist());
            vo.setSteal(row.getSteal());
            vo.setBlock(row.getBlock());
            vo.setTurnover(row.getTurnover());
            vo.setFoul(row.getFoul());
            BasketEventTeam team = teamMap.get(row.getEventTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamNameSnapshot());
            }
            result.add(vo);
        }
        return result;
    }

    private List<V4PublicMatchPlayerStatItemResp> listMatchPlayerStats(Long matchId, Map<Long, BasketEventTeam> teamMap) {
        List<BasketV4MatchPlayerStat> rows = matchPlayerStatDao.selectList(new LambdaQueryWrapper<BasketV4MatchPlayerStat>()
                .eq(BasketV4MatchPlayerStat::getMatchId, matchId)
                .eq(BasketV4MatchPlayerStat::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByDesc(BasketV4MatchPlayerStat::getPoints));
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        Map<Long, BasketEventPlayer> playerMap = loadEventPlayerMap(
                rows.stream().map(BasketV4MatchPlayerStat::getEventPlayerId).collect(Collectors.toList()));
        List<V4PublicMatchPlayerStatItemResp> result = new ArrayList<>();
        for (BasketV4MatchPlayerStat row : rows) {
            V4PublicMatchPlayerStatItemResp vo = new V4PublicMatchPlayerStatItemResp();
            vo.setEventPlayerId(row.getEventPlayerId());
            vo.setEventTeamId(row.getEventTeamId());
            vo.setPoints(row.getPoints());
            vo.setRebound(row.getRebound());
            vo.setAssist(row.getAssist());
            vo.setSteal(row.getSteal());
            vo.setBlock(row.getBlock());
            vo.setTurnover(row.getTurnover());
            vo.setFoul(row.getFoul());
            vo.setMinutes(row.getMinutes());
            BasketEventPlayer player = playerMap.get(row.getEventPlayerId());
            if (player != null) {
                vo.setSourcePlayerId(player.getSourcePlayerId());
                vo.setPlayerName(player.getPlayerNameSnapshot());
            }
            BasketEventTeam team = teamMap.get(row.getEventTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamNameSnapshot());
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 正式赛事上下文：仅读 Season / Division / Series / Stage 主数据，不读 pending / Team ACTIVE。
     */
    private void fillMatchContextNames(V4PublicMatchDetailResp resp, BasketV4Match match) {
        BasketSeason season = seasonDao.selectById(match.getSeasonId());
        if (season != null && Objects.equals(season.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
            resp.setSeasonName(StringUtils.hasText(season.getDisplayTitle()) ? season.getDisplayTitle() : season.getSeasonName());
            resp.setSeriesId(season.getSeriesId());
            if (season.getSeriesId() != null) {
                BasketSeries series = seriesDao.selectById(season.getSeriesId());
                if (series != null && Objects.equals(series.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
                    resp.setSeriesName(series.getSeriesName());
                }
            }
        }
        BasketDivision division = divisionDao.selectById(match.getDivisionId());
        if (division != null && Objects.equals(division.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
            resp.setDivisionName(division.getDivisionName());
        }
        if (match.getStageId() != null) {
            List<V4PublicStageItemResp> stages = publicStageMapper.selectStagesByDivisionId(match.getDivisionId());
            if (!CollectionUtils.isEmpty(stages)) {
                for (V4PublicStageItemResp stage : stages) {
                    if (Objects.equals(stage.getStageId(), match.getStageId())) {
                        resp.setStageName(stage.getStageName());
                        resp.setStageType(stage.getStageType());
                        break;
                    }
                }
            }
        }
        if (!StringUtils.hasText(resp.getStageName()) && StringUtils.hasText(match.getStageCode())) {
            resp.setStageName(match.getStageCode());
        }
    }

    private String resolveMatchStatusName(String matchStatus) {
        if (!StringUtils.hasText(matchStatus)) {
            return "未开始";
        }
        return switch (matchStatus.toUpperCase()) {
            case "PLAYING" -> "进行中";
            case "FINISHED" -> "已结束";
            case "CANCELLED" -> "取消";
            case "FORFEITED", "FORFEIT" -> "弃权";
            case "SCHEDULED", "DRAFT" -> "未开始";
            default -> matchStatus;
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
}
