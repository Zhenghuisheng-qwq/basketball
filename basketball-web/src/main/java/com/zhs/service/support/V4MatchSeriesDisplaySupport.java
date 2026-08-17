package com.zhs.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.v4.BasketDivisionFormatSnapshotDao;
import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.dao.v4.BasketScheduleBatchDao;
import com.zhs.dao.v4.BasketV4MatchDao;
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
import com.zhs.response.v4.publicapi.V4PublicMatchSeriesGameResp;
import com.zhs.response.v4.publicapi.V4PublicScheduleItemResp;
import com.zhs.response.v4.publicapi.V4PublicStageItemResp;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Resource;
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
    private BasketV4PublicStageMapper publicStageMapper;
    @Resource
    private BasketScheduleBatchDao scheduleBatchDao;
    @Resource
    private BasketDivisionFormatSnapshotDao formatSnapshotDao;
    @Resource
    private BasketPublicSupport publicSupport;

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
        return buildDetail(series, currentMatchId);
    }

    public List<V4PublicMatchSeriesDetailResp> listByStageId(Long stageId) {
        List<BasketMatchSeries> list = matchSeriesMapper.selectByStageId(stageId);
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }
        List<V4PublicMatchSeriesDetailResp> result = new ArrayList<>();
        for (BasketMatchSeries series : list) {
            if (!isPubliclyVisibleSeries(series)) {
                continue;
            }
            result.add(buildDetail(series, null));
        }
        return result;
    }

    public List<V4PublicScheduleItemResp> listScheduleByDivisionId(Long divisionId) {
        if (divisionId == null) {
            return List.of();
        }
        List<V4PublicStageItemResp> stages = publicStageMapper.selectStagesByDivisionId(divisionId);
        List<BasketMatchSeries> seriesList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(stages)) {
            for (V4PublicStageItemResp stage : stages) {
                if (stage == null || stage.getStageId() == null) {
                    continue;
                }
                List<BasketMatchSeries> part = matchSeriesMapper.selectByStageId(stage.getStageId());
                if (!CollectionUtils.isEmpty(part)) {
                    seriesList.addAll(part);
                }
            }
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

        List<V4PublicScheduleItemResp> items = new ArrayList<>();
        Set<Long> exposedSeriesIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(seriesList)) {
            for (BasketMatchSeries series : seriesList) {
                if (!isPubliclyVisibleSeries(series)) {
                    continue;
                }
                if (!PublicScheduleUnitResolver.exposeAsSeries(series.getBestOf())) {
                    continue;
                }
                items.add(toSeriesScheduleItem(series, stageMap.get(series.getStageId()),
                        maxRoundByStage.get(series.getStageId())));
                if (series.getId() != null) {
                    exposedSeriesIds.add(series.getId());
                }
            }
        }

        List<BasketV4Match> matches = matchDao.selectList(new LambdaQueryWrapper<BasketV4Match>()
                .eq(BasketV4Match::getDivisionId, divisionId)
                .eq(BasketV4Match::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketV4Match::getRoundNo)
                .orderByAsc(BasketV4Match::getMatchNo));
        if (!CollectionUtils.isEmpty(matches)) {
            for (BasketV4Match match : matches) {
                if (match.getSeriesId() != null && exposedSeriesIds.contains(match.getSeriesId())) {
                    continue;
                }
                if (!isPubliclyVisibleMatch(match)) {
                    continue;
                }
                items.add(toMatchScheduleItem(match, stageMap.get(match.getStageId())));
            }
        }

        items.sort(Comparator
                .comparing(V4PublicScheduleItemResp::getStageId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(V4PublicScheduleItemResp::getRoundNo, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(V4PublicScheduleItemResp::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        fillScheduleTeamLogos(items);
        return items;
    }

    private void fillScheduleTeamLogos(List<V4PublicScheduleItemResp> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        Set<Long> teamIds = new HashSet<>();
        for (V4PublicScheduleItemResp item : items) {
            if (item.getHomeEventTeamId() != null && item.getHomeEventTeamId() > 0) {
                teamIds.add(item.getHomeEventTeamId());
            }
            if (item.getAwayEventTeamId() != null && item.getAwayEventTeamId() > 0) {
                teamIds.add(item.getAwayEventTeamId());
            }
        }
        if (teamIds.isEmpty()) {
            return;
        }
        List<BasketEventTeam> teams = eventTeamDao.selectByIds(teamIds);
        if (CollectionUtils.isEmpty(teams)) {
            return;
        }
        Map<Long, BasketEventTeam> teamMap = new HashMap<>();
        Set<Long> logoIds = new HashSet<>();
        for (BasketEventTeam team : teams) {
            if (team == null || team.getId() == null) {
                continue;
            }
            teamMap.put(team.getId(), team);
            if (team.getTeamLogoFileId() != null) {
                logoIds.add(team.getTeamLogoFileId());
            }
        }
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(logoIds);
        for (V4PublicScheduleItemResp item : items) {
            BasketEventTeam home = teamMap.get(item.getHomeEventTeamId());
            if (home != null) {
                item.setHomeTeamLogoFileId(home.getTeamLogoFileId());
                item.setHomeTeamLogoUrl(urlMap.get(home.getTeamLogoFileId()));
            }
            BasketEventTeam away = teamMap.get(item.getAwayEventTeamId());
            if (away != null) {
                item.setAwayTeamLogoFileId(away.getTeamLogoFileId());
                item.setAwayTeamLogoUrl(urlMap.get(away.getTeamLogoFileId()));
            }
        }
    }

    public V4PublicMatchSeriesDetailResp buildDetail(BasketMatchSeries series, Long currentMatchId) {
        V4PublicMatchSeriesDetailResp vo = new V4PublicMatchSeriesDetailResp();
        vo.setId(series.getId());
        vo.setSeriesCode(series.getSeriesCode());
        vo.setDivisionId(series.getDivisionId());
        vo.setStageId(series.getStageId());
        vo.setRoundNo(series.getRoundNo());
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

        fillStageInfo(vo, series);
        fillTeamNames(vo, series);
        fillForfeitSummary(vo, series);
        fillDisqualificationDisplay(vo, series);
        vo.setGames(buildGames(series, currentMatchId));
        return vo;
    }

    private void fillStageInfo(V4PublicMatchSeriesDetailResp vo, BasketMatchSeries series) {
        if (series.getDivisionId() == null || series.getStageId() == null) {
            return;
        }
        List<V4PublicStageItemResp> stages = publicStageMapper.selectStagesByDivisionId(series.getDivisionId());
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

    private void fillTeamNames(V4PublicMatchSeriesDetailResp vo, BasketMatchSeries series) {
        Set<Long> ids = new HashSet<>();
        if (series.getHomeEventTeamId() != null && series.getHomeEventTeamId() > 0) ids.add(series.getHomeEventTeamId());
        if (series.getAwayEventTeamId() != null && series.getAwayEventTeamId() > 0) ids.add(series.getAwayEventTeamId());
        if (series.getWinnerEventTeamId() != null) ids.add(series.getWinnerEventTeamId());
        if (ids.isEmpty()) {
            if (series.getHomeSourceDisplay() != null && !series.getHomeSourceDisplay().isBlank()) {
                vo.setHomeTeamName(series.getHomeSourceDisplay());
            } else {
                vo.setHomeTeamName("待定");
            }
            if (series.getAwaySourceDisplay() != null && !series.getAwaySourceDisplay().isBlank()) {
                vo.setAwayTeamName(series.getAwaySourceDisplay());
            } else {
                vo.setAwayTeamName("待定");
            }
            return;
        }
        List<BasketEventTeam> teams = eventTeamDao.selectByIds(ids);
        Map<Long, BasketEventTeam> map = new HashMap<>();
        for (BasketEventTeam team : teams) {
            if (team != null) {
                map.put(team.getId(), team);
            }
        }
        BasketEventTeam home = map.get(series.getHomeEventTeamId());
        if (home != null) {
            vo.setHomeTeamName(home.getTeamNameSnapshot());
        } else if (series.getHomeSourceDisplay() != null && !series.getHomeSourceDisplay().isBlank()) {
            vo.setHomeTeamName(series.getHomeSourceDisplay());
        } else {
            vo.setHomeTeamName(series.getHomeSourceDisplay() != null && !series.getHomeSourceDisplay().isBlank()
                    ? series.getHomeSourceDisplay() : "待定");
        }
        BasketEventTeam away = map.get(series.getAwayEventTeamId());
        if (away != null) {
            vo.setAwayTeamName(away.getTeamNameSnapshot());
        } else if (series.getAwaySourceDisplay() != null && !series.getAwaySourceDisplay().isBlank()) {
            vo.setAwayTeamName(series.getAwaySourceDisplay());
        } else {
            vo.setAwayTeamName(series.getAwaySourceDisplay() != null && !series.getAwaySourceDisplay().isBlank()
                    ? series.getAwaySourceDisplay() : "待定");
        }
        BasketEventTeam winner = map.get(series.getWinnerEventTeamId());
        if (winner != null) {
            vo.setWinnerTeamName(winner.getTeamNameSnapshot());
        }
    }

    private List<V4PublicMatchSeriesGameResp> buildGames(BasketMatchSeries series, Long currentMatchId) {
        List<BasketV4Match> matches = matchDao.selectList(new LambdaQueryWrapper<BasketV4Match>()
                .eq(BasketV4Match::getSeriesId, series.getId())
                .eq(BasketV4Match::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketV4Match::getSeriesGameNo));
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
            game.setScheduledTime(match.getMatchTime() != null ? match.getMatchTime() : inheritSeriesTime(series));
            game.setCurrentGame(currentMatchId != null && currentMatchId.equals(match.getId()));
            if ((seriesCompleted || seriesForfeit) && !isOfficialResult(match)) {
                game.setSkipped(true);
                game.setSkippedReason(seriesForfeit ? "系列赛已判定弃赛，本局无需进行" : "系列赛已提前结束，本局无需进行");
            }
            games.add(game);
        }
        return games;
    }

    public Map<Long, BasketMatchSeries> loadSeriesMap(Collection<Long> seriesIds) {
        if (CollectionUtils.isEmpty(seriesIds)) {
            return Collections.emptyMap();
        }
        Map<Long, BasketMatchSeries> map = new HashMap<>();
        for (Long id : seriesIds) {
            if (id == null) continue;
            BasketMatchSeries series = matchSeriesMapper.selectById(id);
            if (series != null) {
                map.put(id, series);
            }
        }
        return map;
    }

    public boolean isPublishedBatch(Long batchId) {
        if (batchId == null) {
            return false;
        }
        BasketScheduleBatch batch = scheduleBatchDao.selectById(batchId);
        return batch != null && PublishStatusEnum.PUBLISHED.getCode().equals(batch.getPublishStatus());
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

    public boolean isPubliclyVisibleMatch(BasketV4Match match) {
        if (match == null) {
            return false;
        }
        boolean matchPublished = MatchPublishStatusEnum.PUBLISHED.getCode().equals(match.getPublishStatus());
        boolean batchPublished = isPublishedBatch(match.getScheduleBatchId());
        return PublicVisibilityPolicy.keepPublishedMatch(isVersionedDivision(match.getDivisionId()), matchPublished, batchPublished);
    }

    public boolean isPubliclyVisibleSeries(BasketMatchSeries series) {
        if (series == null) {
            return false;
        }
        boolean batchPublished = isPublishedBatch(series.getScheduleBatchId());
        boolean hasPublishedChild = false;
        if (!isVersionedDivision(series.getDivisionId()) && !batchPublished && series.getId() != null) {
            Long count = matchDao.selectCount(new LambdaQueryWrapper<BasketV4Match>()
                    .eq(BasketV4Match::getSeriesId, series.getId())
                    .eq(BasketV4Match::getIsDelete, 0)
                    .eq(BasketV4Match::getPublishStatus, MatchPublishStatusEnum.PUBLISHED.getCode()));
            hasPublishedChild = count != null && count > 0;
        }
        return PublicVisibilityPolicy.keepPublishedSeries(
                isVersionedDivision(series.getDivisionId()), batchPublished, hasPublishedChild);
    }

    private V4PublicScheduleItemResp toSeriesScheduleItem(BasketMatchSeries series,
                                                          V4PublicStageItemResp stage,
                                                          Integer maxRound) {
        V4PublicMatchSeriesDetailResp detail = buildDetail(series, null);
        V4PublicScheduleItemResp item = new V4PublicScheduleItemResp();
        item.setId(series.getId());
        item.setUnitType(PublicScheduleUnitResolver.MATCH_SERIES);
        item.setStageId(series.getStageId());
        item.setStageName(detail.getStageName() != null ? detail.getStageName()
                : (stage != null ? stage.getStageName() : null));
        item.setRoundNo(series.getRoundNo());
        item.setRoundName(PublicKnockoutRoundNames.name(series.getRoundNo(), maxRound));
        item.setSeriesCode(series.getSeriesCode());
        item.setHomeEventTeamId(detail.getHomeEventTeamId());
        item.setAwayEventTeamId(detail.getAwayEventTeamId());
        item.setHomeTeam(detail.getHomeTeamName());
        item.setAwayTeam(detail.getAwayTeamName());
        item.setBestOf(series.getBestOf());
        int[] displayWins = resolveDisplayWinCounts(series);
        int homeWins = displayWins[0];
        int awayWins = displayWins[1];
        item.setHomeWinCount(homeWins);
        item.setAwayWinCount(awayWins);
        item.setWinnerEventTeamId(series.getWinnerEventTeamId());
        item.setRequiredWins(detail.getRequiredWins());
        item.setSeriesScore(formatSeriesScore(series, homeWins, awayWins));
        int gamesPlayed = 0;
        if (detail.getGames() != null) {
            for (V4PublicMatchSeriesGameResp game : detail.getGames()) {
                if (game != null && !Boolean.TRUE.equals(game.getSkipped()) && isOfficialGameStatus(game.getMatchStatus())) {
                    gamesPlayed++;
                }
            }
        }
        item.setGamesPlayed(gamesPlayed);
        item.setGameProgress(gamesPlayed + "/" + (series.getBestOf() == null ? 0 : series.getBestOf()));
        item.setScheduleDate(formatScheduleDate(series.getScheduledDate()));
        item.setStartTime(formatScheduleTime(series.getScheduledStartTime()));
        item.setStatus(series.getSeriesStatus());
        item.setStatusName(detail.getSeriesStatusName());
        item.setPlaceholderSource(joinPlaceholderSource(series.getHomeSourceType(), series.getAwaySourceType()));
        item.setPublished(true);
        BasketEventTeam homeTeam = series.getHomeEventTeamId() != null && series.getHomeEventTeamId() > 0
                ? eventTeamDao.selectById(series.getHomeEventTeamId()) : null;
        BasketEventTeam awayTeam = series.getAwayEventTeamId() != null && series.getAwayEventTeamId() > 0
                ? eventTeamDao.selectById(series.getAwayEventTeamId()) : null;
        com.zhs.support.v4.EventTeamPublicDisplaySupport.fillSchedule(item, series, homeTeam, awayTeam);
        return item;
    }

    private V4PublicScheduleItemResp toMatchScheduleItem(BasketV4Match match, V4PublicStageItemResp stage) {
        V4PublicScheduleItemResp item = new V4PublicScheduleItemResp();
        item.setId(match.getId());
        item.setUnitType(PublicScheduleUnitResolver.MATCH);
        item.setStageId(match.getStageId());
        item.setStageName(stage != null ? stage.getStageName() : null);
        item.setRoundNo(match.getRoundNo());
        item.setRoundName(match.getRoundNo() == null ? "--" : ("R" + match.getRoundNo()));
        item.setSeriesCode(match.getMatchNo());
        item.setHomeEventTeamId(positiveOrNull(match.getHomeEventTeamId()));
        item.setAwayEventTeamId(positiveOrNull(match.getAwayEventTeamId()));
        item.setHomeTeam(null);
        item.setAwayTeam(null);
        BasketEventTeam home = null;
        BasketEventTeam away = null;
        if (item.getHomeEventTeamId() != null || item.getAwayEventTeamId() != null) {
            Set<Long> ids = new HashSet<>();
            if (item.getHomeEventTeamId() != null) ids.add(item.getHomeEventTeamId());
            if (item.getAwayEventTeamId() != null) ids.add(item.getAwayEventTeamId());
            List<BasketEventTeam> teams = eventTeamDao.selectByIds(ids);
            Map<Long, BasketEventTeam> map = new HashMap<>();
            if (teams != null) {
                for (BasketEventTeam team : teams) {
                    if (team != null) {
                        map.put(team.getId(), team);
                    }
                }
            }
            home = map.get(item.getHomeEventTeamId());
            away = map.get(item.getAwayEventTeamId());
            if (home != null) {
                item.setHomeTeam(home.getTeamNameSnapshot());
            }
            if (away != null) {
                item.setAwayTeam(away.getTeamNameSnapshot());
            }
        }
        if (item.getHomeTeam() == null || item.getHomeTeam().isBlank()) {
            item.setHomeTeam("待定");
        }
        if (item.getAwayTeam() == null || item.getAwayTeam().isBlank()) {
            item.setAwayTeam("待定");
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

    private java.util.Date inheritSeriesTime(BasketMatchSeries series) {
        if (series == null || series.getScheduledDate() == null || series.getScheduledStartTime() == null) {
            return null;
        }
        return java.util.Date.from(java.time.LocalDateTime.of(series.getScheduledDate(), series.getScheduledStartTime())
                .atZone(java.time.ZoneId.of("Asia/Shanghai")).toInstant());
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

    private void fillDisqualificationDisplay(V4PublicMatchSeriesDetailResp vo, BasketMatchSeries series) {
        BasketEventTeam home = series.getHomeEventTeamId() != null && series.getHomeEventTeamId() > 0
                ? eventTeamDao.selectById(series.getHomeEventTeamId()) : null;
        BasketEventTeam away = series.getAwayEventTeamId() != null && series.getAwayEventTeamId() > 0
                ? eventTeamDao.selectById(series.getAwayEventTeamId()) : null;
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
