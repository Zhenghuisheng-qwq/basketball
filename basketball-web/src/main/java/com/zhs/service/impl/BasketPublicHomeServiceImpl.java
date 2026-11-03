package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.v4.BasketEventPlayerDao;
import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.dao.v4.BasketSeasonDao;
import com.zhs.dao.v4.BasketV4MatchDao;
import com.zhs.enums.PublicPlayerRankTypeEnum;
import com.zhs.enums.PublicTeamRankTypeEnum;
import com.zhs.enums.v4.MatchPublishStatusEnum;
import com.zhs.enums.v4.MatchStatusEnum;
import com.zhs.enums.v4.PublishStatusEnum;
import com.zhs.model.v4.BasketEventPlayer;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketMatchSeries;
import com.zhs.model.v4.BasketSeason;
import com.zhs.model.v4.BasketV4Match;
import com.zhs.response.publicapi.PublicHomeDataFocusVo;
import com.zhs.response.publicapi.PublicHomeFocusTeamVo;
import com.zhs.response.publicapi.PublicHomePlayerLeaderVo;
import com.zhs.response.publicapi.PublicHomeVo;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.response.publicapi.PublicPlayerRankItemVo;
import com.zhs.response.publicapi.PublicSeasonSummaryVo;
import com.zhs.response.publicapi.PublicTeamRankItemVo;
import com.zhs.response.v4.publicapi.V4PublicDivisionBriefResp;
import com.zhs.response.v4.publicapi.V4PublicMatchItemResp;
import com.zhs.response.v4.publicapi.V4PublicPlayerStatItemResp;
import com.zhs.response.v4.publicapi.V4PublicSeasonItemResp;
import com.zhs.response.v4.publicapi.V4PublicStandingItemResp;
import com.zhs.service.IBasketPublicAnnouncementService;
import com.zhs.service.IBasketPublicBannerService;
import com.zhs.service.IBasketPublicDataCenterService;
import com.zhs.service.IBasketPublicHomeService;
import com.zhs.service.IBasketPublicPartnerService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import com.zhs.service.support.HomeRealtimeFocusSupport;
import com.zhs.service.support.V4CurrentSeasonSelector;
import com.zhs.service.support.V4MatchSeriesDisplaySupport;
import com.zhs.service.v4.IBasketV4PublicService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BasketPublicHomeServiceImpl implements IBasketPublicHomeService {

    /**
     * 热门球队无独立热度字段，取当前公开赛季球队榜 WIN_RATE 前 N，来源是赛季排名而非热度。
     */
    private static final String HOT_TEAM_SOURCE_NOTE =
            "热门球队来源：当前公开赛季球队排行榜 WIN_RATE 前 N（非热度字段）";

    @Resource
    private BasketV4MatchDao matchDao;
    @Resource
    private BasketSeasonDao seasonDao;
    @Resource
    private BasketPublicSupport publicSupport;
    @Resource
    private BasketPublicMatchServiceImpl matchService;
    @Resource
    private IBasketPublicDataCenterService dataCenterService;
    @Resource
    private IBasketPublicBannerService bannerService;
    @Resource
    private IBasketPublicAnnouncementService announcementService;
    @Resource
    private IBasketPublicPartnerService partnerService;
    @Resource
    private IBasketV4PublicService v4PublicService;
    @Resource
    private BasketEventPlayerDao eventPlayerDao;
    @Resource
    private BasketEventTeamDao eventTeamDao;
    @Resource
    private V4MatchSeriesDisplaySupport matchSeriesDisplaySupport;
    @Resource
    private com.zhs.support.v4.PublicCurrentRosterSupport currentRosterSupport;
    @Resource
    private com.zhs.service.impl.v4.BasketV4PublicLeaderboardService leaderboardService;

    @Override
    public PublicHomeVo getHome() {
        return publicSupport.cacheGetOrLoad("home:aggregate", () -> {
            PublicHomeVo vo = new PublicHomeVo();
            vo.setRecentMatches(safeList(() -> listRecentMatches(BasketPublicConstants.HOME_LIMIT)));
            vo.setTeamRankings(safeList(() -> listTeamRanking(BasketPublicConstants.HOME_LIMIT)));
            vo.setPlayerRankings(safeList(() -> listPlayerRanking(BasketPublicConstants.HOME_LIMIT)));
            try {
                vo.setSeasonSummary(getStatSummary());
            } catch (Exception ignored) {
                vo.setSeasonSummary(null);
            }
            vo.setHotTeamSourceNote(HOT_TEAM_SOURCE_NOTE);
            vo.setBanners(safeList(() -> bannerService.listBanners(null)));
            vo.setLatestAnnouncements(safeList(() ->
                    announcementService.listLatest(BasketPublicConstants.HOME_ANNOUNCEMENT_LIMIT)));
            vo.setPartners(safeList(partnerService::listPartners));
            return vo;
        });
    }

    private <T> List<T> safeList(java.util.function.Supplier<List<T>> supplier) {
        try {
            List<T> list = supplier.get();
            return list == null ? Collections.emptyList() : list;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<PublicMatchItemVo> listRecentMatches(Integer limit) {
        int size = normalizeLimit(limit);
        Date now = new Date();
        Set<Long> publishedSeasonIds = loadPublishedV4SeasonIds();
        if (publishedSeasonIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<BasketV4Match> upcoming = matchDao.selectList(new LambdaQueryWrapper<BasketV4Match>()
                .eq(BasketV4Match::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketV4Match::getPublishStatus, MatchPublishStatusEnum.PUBLISHED.getCode())
                .in(BasketV4Match::getSeasonId, publishedSeasonIds)
                .in(BasketV4Match::getMatchStatus,
                        MatchStatusEnum.SCHEDULED.getCode(), MatchStatusEnum.PLAYING.getCode())
                .isNotNull(BasketV4Match::getHomeEventTeamId)
                .isNotNull(BasketV4Match::getAwayEventTeamId)
                .apply(effectiveStartAtOrAfterNowSql(), now)
                .apply(oneCardPerSeriesSql())
                .last("order by " + effectiveStartExpr() + " asc, round_no asc, id asc limit " + size));
        upcoming = matchSeriesDisplaySupport.excludeRemainingUnplayed(upcoming);
        stampSeriesStartWhenMatchTimeMissing(upcoming);

        List<BasketV4Match> finished = matchDao.selectList(new LambdaQueryWrapper<BasketV4Match>()
                .eq(BasketV4Match::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketV4Match::getPublishStatus, MatchPublishStatusEnum.PUBLISHED.getCode())
                .in(BasketV4Match::getSeasonId, publishedSeasonIds)
                .eq(BasketV4Match::getMatchStatus, MatchStatusEnum.FINISHED.getCode())
                .orderByDesc(BasketV4Match::getMatchTime)
                .orderByDesc(BasketV4Match::getRoundNo)
                .orderByDesc(BasketV4Match::getId)
                .last("limit " + size));

        List<BasketV4Match> merged = new java.util.ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (BasketV4Match match : upcoming) {
            if (seen.add(match.getId())) {
                merged.add(match);
            }
        }
        for (BasketV4Match match : finished) {
            if (seen.add(match.getId()) && merged.size() < size) {
                merged.add(match);
            }
        }

        if (CollectionUtils.isEmpty(merged)) {
            return Collections.emptyList();
        }
        return matchService.enrichMatches(merged.stream().limit(size).collect(Collectors.toList()));
    }

    /** 子局未单独写 match_time 时，用系列赛开赛时间。 */
    private static String effectiveStartExpr() {
        return "coalesce(match_time, (select timestamp(s.scheduled_date, s.scheduled_start_time)"
                + " from basket_match_series s where s.id = basket_match.series_id and s.is_delete = 0"
                + " and s.scheduled_date is not null and s.scheduled_start_time is not null))";
    }

    private static String effectiveStartAtOrAfterNowSql() {
        return "(match_status = 'PLAYING' or " + effectiveStartExpr() + " >= {0})";
    }

    /** 系列赛首页只出一张卡片，取尚未结束的最小局次。 */
    private static String oneCardPerSeriesSql() {
        return "(series_id is null or series_game_no is null or series_game_no = ("
                + "select min(c.series_game_no) from basket_match c"
                + " where c.series_id = basket_match.series_id and c.is_delete = 0"
                + " and c.publish_status = 'PUBLISHED'"
                + " and c.match_status in ('SCHEDULED','PLAYING')"
                + " and c.home_event_team_id is not null and c.away_event_team_id is not null))";
    }

    private void stampSeriesStartWhenMatchTimeMissing(List<BasketV4Match> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return;
        }
        Set<Long> seriesIds = matches.stream()
                .filter(match -> match.getMatchTime() == null && match.getSeriesId() != null)
                .map(BasketV4Match::getSeriesId)
                .collect(Collectors.toSet());
        if (seriesIds.isEmpty()) {
            return;
        }
        Map<Long, BasketMatchSeries> seriesMap = matchSeriesDisplaySupport.loadSeriesMap(seriesIds);
        for (BasketV4Match match : matches) {
            if (match.getMatchTime() != null) {
                continue;
            }
            match.setMatchTime(matchSeriesDisplaySupport.resolveDisplayTime(match, seriesMap.get(match.getSeriesId())));
        }
    }

    @Override
    public List<PublicTeamRankItemVo> listTeamRanking(Integer limit) {
        // 无热度字段：使用当前公开赛季 WIN_RATE 排名前 N
        return dataCenterService.listTeamRanking(null, PublicTeamRankTypeEnum.WIN_RATE.getCode(), normalizeLimit(limit));
    }

    @Override
    public List<PublicPlayerRankItemVo> listPlayerRanking(Integer limit) {
        return dataCenterService.listPlayerRanking(null, PublicPlayerRankTypeEnum.AVG_POINTS.getCode(), normalizeLimit(limit));
    }

    @Override
    public PublicSeasonSummaryVo getStatSummary() {
        BasketSeason season = publicSupport.findCurrentPublicSeason();
        if (season == null) {
            return null;
        }
        return dataCenterService.getSeasonSummary(season.getId());
    }

    @Override
    public PublicHomeDataFocusVo getDataFocus(String seriesCode) {
        String normalized = HomeRealtimeFocusSupport.normalizeSeriesCode(seriesCode);
        PublicHomeDataFocusVo vo = new PublicHomeDataFocusVo();
        vo.setSeriesCode(normalized);
        try {
            return buildDataFocus(normalized, vo);
        } catch (Exception ignored) {
            return vo;
        }
    }

    private PublicHomeDataFocusVo buildDataFocus(String normalized, PublicHomeDataFocusVo vo) {
        if (!HomeRealtimeFocusSupport.isHomeSeriesCode(normalized)) {
            return vo;
        }

        List<V4PublicSeasonItemResp> seasons = safeList(() ->
                v4PublicService.listSeasons(normalized, null, null));
        V4PublicSeasonItemResp current = V4CurrentSeasonSelector.pickCurrentPublic(seasons);
        if (current == null || current.getSeasonId() == null) {
            return vo;
        }
        vo.setSeasonId(current.getSeasonId());
        V4PublicDivisionBriefResp division = HomeRealtimeFocusSupport.pickDefaultDivision(current);
        if (division == null || division.getDivisionId() == null) {
            return vo;
        }
        Long featuredDivisionId = leaderboardService.featuredDivisionId();
        Long leaderboardDivisionId = featuredDivisionId != null ? featuredDivisionId : division.getDivisionId();
        vo.setDivisionId(leaderboardDivisionId);
        applyPublishedLeaders(vo, leaderboardDivisionId);

        List<V4PublicMatchItemResp> officialMatches = safeList(() ->
                v4PublicService.listMatches(leaderboardDivisionId, null, null, null, Boolean.TRUE, null));
        Map<Long, Integer> rankMap = loadStandingRankMap(leaderboardDivisionId);
        Date now = new Date();
        PublicHomeFocusTeamVo focusTeam = HomeRealtimeFocusSupport.pickWeeklyFocusTeam(
                officialMatches,
                HomeRealtimeFocusSupport.weekStartInclusive(now),
                HomeRealtimeFocusSupport.weekEndExclusive(now),
                rankMap);
        if (focusTeam != null) {
            fillFocusTeamDisplay(focusTeam);
        }
        vo.setFocusTeam(focusTeam);
        return vo;
    }

    private void applyPublishedLeaders(PublicHomeDataFocusVo vo, Long divisionId) {
        vo.setPointsLeader(leaderboardService.leader(divisionId, "OVERALL", "PTS"));
        vo.setAssistsLeader(leaderboardService.leader(divisionId, "OVERALL", "AST"));
        vo.setReboundsLeader(leaderboardService.leader(divisionId, "OVERALL", "REB"));
        enrichLeaders(vo.getPointsLeader(), vo.getAssistsLeader(), vo.getReboundsLeader());
    }

    private Map<Long, Integer> loadStandingRankMap(Long divisionId) {
        List<V4PublicStandingItemResp> rows = safeList(() -> v4PublicService.listStanding(divisionId));
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> ranks = new HashMap<>();
        for (V4PublicStandingItemResp row : rows) {
            if (row.getEventTeamId() != null && row.getRankNo() != null) {
                ranks.put(row.getEventTeamId(), row.getRankNo());
            }
        }
        return ranks;
    }

    private void enrichLeaders(PublicHomePlayerLeaderVo... leaders) {
        Set<Long> playerIds = new LinkedHashSet<>();
        Set<Long> teamIds = new LinkedHashSet<>();
        List<PublicHomePlayerLeaderVo> present = new ArrayList<>();
        for (PublicHomePlayerLeaderVo leader : leaders) {
            if (leader == null) {
                continue;
            }
            present.add(leader);
            if (leader.getEventPlayerId() != null) {
                playerIds.add(leader.getEventPlayerId());
            }
            if (leader.getEventTeamId() != null) {
                teamIds.add(leader.getEventTeamId());
            }
        }
        if (present.isEmpty()) {
            return;
        }
        Map<Long, BasketEventPlayer> playerMap = loadEventPlayerMap(playerIds);
        Map<Long, BasketEventTeam> teamMap = loadEventTeamMap(teamIds);
        Set<Long> fileIds = new HashSet<>();
        for (BasketEventPlayer player : playerMap.values()) {
            if (player.getAvatarFileId() != null) {
                fileIds.add(player.getAvatarFileId());
            }
        }
        for (BasketEventTeam team : teamMap.values()) {
            if (team.getTeamLogoFileId() != null) {
                fileIds.add(team.getTeamLogoFileId());
            }
        }
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(fileIds);
        Map<Long, String> currentNames = currentRosterSupport.loadCurrentNames(playerMap.values());
        for (PublicHomePlayerLeaderVo leader : present) {
            BasketEventPlayer player = playerMap.get(leader.getEventPlayerId());
            if (player != null) {
                if (leader.getSourcePlayerId() == null) {
                    leader.setSourcePlayerId(player.getSourcePlayerId());
                }
                String currentName = currentNames.get(player.getId());
                if (org.springframework.util.StringUtils.hasText(currentName)) {
                    leader.setPlayerName(currentName);
                } else if (!StringUtils.hasText(leader.getPlayerName())) {
                    leader.setPlayerName(player.getPlayerNameSnapshot());
                }
                leader.setAvatarUrl(urlMap.get(player.getAvatarFileId()));
            }
            BasketEventTeam team = teamMap.get(leader.getEventTeamId());
            if (team != null) {
                if (!StringUtils.hasText(leader.getTeamName())) {
                    leader.setTeamName(team.getTeamNameSnapshot());
                }
                leader.setTeamLogoUrl(urlMap.get(team.getTeamLogoFileId()));
            }
        }
    }

    private void fillFocusTeamDisplay(PublicHomeFocusTeamVo focusTeam) {
        if (focusTeam.getEventTeamId() == null) {
            return;
        }
        BasketEventTeam team = eventTeamDao.selectById(focusTeam.getEventTeamId());
        if (team == null) {
            return;
        }
        if (!StringUtils.hasText(focusTeam.getTeamName())) {
            focusTeam.setTeamName(team.getTeamNameSnapshot());
        }
        if (!StringUtils.hasText(focusTeam.getTeamLogoUrl()) && team.getTeamLogoFileId() != null) {
            Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(Collections.singleton(team.getTeamLogoFileId()));
            focusTeam.setTeamLogoUrl(urlMap.get(team.getTeamLogoFileId()));
        }
    }

    private Map<Long, BasketEventPlayer> loadEventPlayerMap(Set<Long> playerIds) {
        if (CollectionUtils.isEmpty(playerIds)) {
            return Collections.emptyMap();
        }
        List<BasketEventPlayer> rows = eventPlayerDao.selectBatchIds(playerIds);
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyMap();
        }
        return rows.stream()
                .filter(row -> row.getId() != null)
                .collect(Collectors.toMap(BasketEventPlayer::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, BasketEventTeam> loadEventTeamMap(Set<Long> teamIds) {
        if (CollectionUtils.isEmpty(teamIds)) {
            return Collections.emptyMap();
        }
        List<BasketEventTeam> rows = eventTeamDao.selectBatchIds(teamIds);
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyMap();
        }
        return rows.stream()
                .filter(row -> row.getId() != null)
                .collect(Collectors.toMap(BasketEventTeam::getId, Function.identity(), (left, right) -> left));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return BasketPublicConstants.HOME_LIMIT;
        }
        return Math.min(limit, BasketPublicConstants.MAX_SIZE);
    }

    private Set<Long> loadPublishedV4SeasonIds() {
        List<BasketSeason> seasons = seasonDao.selectList(new LambdaQueryWrapper<BasketSeason>()
                .eq(BasketSeason::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketSeason::getPublishStatus, PublishStatusEnum.PUBLISHED.getCode()));
        if (CollectionUtils.isEmpty(seasons)) {
            return Collections.emptySet();
        }
        return seasons.stream()
                .map(BasketSeason::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
