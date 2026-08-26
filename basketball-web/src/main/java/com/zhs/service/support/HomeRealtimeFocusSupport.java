package com.zhs.service.support;

import com.zhs.enums.v4.MatchStatusEnum;
import com.zhs.response.publicapi.PublicHomeFocusTeamVo;
import com.zhs.response.publicapi.PublicHomePlayerLeaderVo;
import com.zhs.response.v4.publicapi.V4PublicDivisionBriefResp;
import com.zhs.response.v4.publicapi.V4PublicMatchItemResp;
import com.zhs.response.v4.publicapi.V4PublicPlayerStatItemResp;
import com.zhs.response.v4.publicapi.V4PublicSeasonItemResp;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Homepage realtime aggregation only. Does not recalculate PlayerStat / Standing / Match results.
 */
public final class HomeRealtimeFocusSupport {

    public static final List<String> HOME_SERIES_CODES = List.of("LEAGUE", "CUP", "ARENA", "OPEN");

    /** Project public JSON / JDBC convention is GMT+8. */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("GMT+8");

    public static final int RECENT_FORM_LIMIT = 5;

    private HomeRealtimeFocusSupport() {
    }

    public static String normalizeSeriesCode(String seriesCode) {
        if (!StringUtils.hasText(seriesCode)) {
            return "LEAGUE";
        }
        return seriesCode.trim().toUpperCase();
    }

    public static boolean isHomeSeriesCode(String seriesCode) {
        return HOME_SERIES_CODES.contains(normalizeSeriesCode(seriesCode));
    }

    public static V4PublicDivisionBriefResp pickDefaultDivision(V4PublicSeasonItemResp season) {
        if (season == null || CollectionUtils.isEmpty(season.getDivisions())) {
            return null;
        }
        return season.getDivisions().get(0);
    }

    public static Date weekStartInclusive(Date now) {
        ZonedDateTime zoned = toZoned(now);
        return Date.from(zoned.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay(BUSINESS_ZONE).toInstant());
    }

    public static Date weekEndExclusive(Date now) {
        return Date.from(weekStartInclusive(now).toInstant().atZone(BUSINESS_ZONE).plusWeeks(1).toInstant());
    }

    public static boolean inWeek(Date matchTime, Date weekStartInclusive, Date weekEndExclusive) {
        if (matchTime == null || weekStartInclusive == null || weekEndExclusive == null) {
            return false;
        }
        return !matchTime.before(weekStartInclusive) && matchTime.before(weekEndExclusive);
    }

    public static boolean isOfficialResult(String matchStatus) {
        if (!StringUtils.hasText(matchStatus)) {
            return false;
        }
        String status = matchStatus.trim().toUpperCase();
        return MatchStatusEnum.FINISHED.getCode().equals(status)
                || MatchStatusEnum.FORFEITED.getCode().equals(status);
    }

    public static PublicHomePlayerLeaderVo pickLeader(
            List<V4PublicPlayerStatItemResp> rows,
            Function<V4PublicPlayerStatItemResp, BigDecimal> valueFn) {
        if (CollectionUtils.isEmpty(rows) || valueFn == null) {
            return null;
        }
        V4PublicPlayerStatItemResp best = rows.stream()
                .filter(row -> row != null && row.getEventPlayerId() != null && valueFn.apply(row) != null)
                .min(Comparator
                        .comparing(valueFn, Comparator.reverseOrder())
                        .thenComparing(row -> row.getMatchCount() == null ? 0 : row.getMatchCount(),
                                Comparator.reverseOrder())
                        .thenComparing(V4PublicPlayerStatItemResp::getEventPlayerId))
                .orElse(null);
        if (best == null) {
            return null;
        }
        PublicHomePlayerLeaderVo vo = new PublicHomePlayerLeaderVo();
        vo.setEventPlayerId(best.getEventPlayerId());
        vo.setSourcePlayerId(best.getSourcePlayerId());
        vo.setPlayerName(best.getPlayerName());
        vo.setEventTeamId(best.getEventTeamId());
        vo.setTeamName(best.getTeamName());
        vo.setMatchCount(best.getMatchCount());
        vo.setValue(valueFn.apply(best));
        return vo;
    }

    public static PublicHomeFocusTeamVo pickWeeklyFocusTeam(
            List<V4PublicMatchItemResp> officialMatches,
            Date weekStartInclusive,
            Date weekEndExclusive,
            Map<Long, Integer> standingRankByTeamId) {
        Map<Long, WeekTeamAgg> aggregates = new LinkedHashMap<>();
        if (!CollectionUtils.isEmpty(officialMatches)) {
            for (V4PublicMatchItemResp match : officialMatches) {
                if (!isOfficialResult(match.getMatchStatus())
                        || !inWeek(match.getMatchTime(), weekStartInclusive, weekEndExclusive)
                        || match.getWinnerEventTeamId() == null
                        || match.getHomeEventTeamId() == null
                        || match.getAwayEventTeamId() == null) {
                    continue;
                }
                applyMatch(aggregates, match);
            }
        }
        List<WeekTeamAgg> candidates = new ArrayList<>();
        for (WeekTeamAgg agg : aggregates.values()) {
            if (agg.matchCount > 0) {
                candidates.add(agg);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        Map<Long, Integer> ranks = standingRankByTeamId == null ? Collections.emptyMap() : standingRankByTeamId;
        candidates.sort((left, right) -> {
            int rate = right.winRate().compareTo(left.winRate());
            if (rate != 0) {
                return rate;
            }
            int wins = Integer.compare(right.winCount, left.winCount);
            if (wins != 0) {
                return wins;
            }
            int diff = Integer.compare(right.pointDiff, left.pointDiff);
            if (diff != 0) {
                return diff;
            }
            int rank = Integer.compare(
                    ranks.getOrDefault(left.eventTeamId, Integer.MAX_VALUE),
                    ranks.getOrDefault(right.eventTeamId, Integer.MAX_VALUE));
            if (rank != 0) {
                return rank;
            }
            return Long.compare(left.eventTeamId, right.eventTeamId);
        });
        WeekTeamAgg best = candidates.get(0);
        PublicHomeFocusTeamVo vo = new PublicHomeFocusTeamVo();
        vo.setEventTeamId(best.eventTeamId);
        vo.setTeamName(best.teamName);
        vo.setTeamLogoUrl(best.teamLogoUrl);
        vo.setWeekMatchCount(best.matchCount);
        vo.setWeekWinCount(best.winCount);
        vo.setWeekLoseCount(best.loseCount);
        vo.setWeekPointDiff(best.pointDiff);
        vo.setWeekAvgPointDiff(best.avgPointDiff());
        vo.setWeekWinRate(best.winRate());
        vo.setRecentForm(recentForm(officialMatches, best.eventTeamId));
        return vo;
    }

    public static List<String> recentForm(List<V4PublicMatchItemResp> officialMatches, Long eventTeamId) {
        if (eventTeamId == null || CollectionUtils.isEmpty(officialMatches)) {
            return Collections.emptyList();
        }
        List<V4PublicMatchItemResp> teamMatches = new ArrayList<>();
        for (V4PublicMatchItemResp match : officialMatches) {
            if (!isOfficialResult(match.getMatchStatus())
                    || match.getWinnerEventTeamId() == null
                    || !participated(match, eventTeamId)) {
                continue;
            }
            teamMatches.add(match);
        }
        teamMatches.sort(Comparator
                .comparing(V4PublicMatchItemResp::getMatchTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(V4PublicMatchItemResp::getMatchId, Comparator.nullsLast(Comparator.reverseOrder())));
        int size = Math.min(RECENT_FORM_LIMIT, teamMatches.size());
        List<String> newestFirst = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            newestFirst.add(Objects.equals(teamMatches.get(i).getWinnerEventTeamId(), eventTeamId) ? "W" : "L");
        }
        Collections.reverse(newestFirst);
        return newestFirst;
    }

    private static void applyMatch(Map<Long, WeekTeamAgg> aggregates, V4PublicMatchItemResp match) {
        int homeScore = match.getHomeScore() == null ? 0 : match.getHomeScore();
        int awayScore = match.getAwayScore() == null ? 0 : match.getAwayScore();
        boolean homeWon = Objects.equals(match.getWinnerEventTeamId(), match.getHomeEventTeamId());
        boolean awayWon = Objects.equals(match.getWinnerEventTeamId(), match.getAwayEventTeamId());
        if (!homeWon && !awayWon) {
            return;
        }
        WeekTeamAgg home = aggregates.computeIfAbsent(match.getHomeEventTeamId(), WeekTeamAgg::new);
        WeekTeamAgg away = aggregates.computeIfAbsent(match.getAwayEventTeamId(), WeekTeamAgg::new);
        home.accept(homeWon, homeScore, awayScore, match.getHomeTeamName(), match.getHomeTeamLogoUrl());
        away.accept(awayWon, awayScore, homeScore, match.getAwayTeamName(), match.getAwayTeamLogoUrl());
    }

    private static boolean participated(V4PublicMatchItemResp match, Long eventTeamId) {
        return Objects.equals(match.getHomeEventTeamId(), eventTeamId)
                || Objects.equals(match.getAwayEventTeamId(), eventTeamId);
    }

    private static ZonedDateTime toZoned(Date now) {
        Instant instant = now == null ? Instant.now() : now.toInstant();
        return instant.atZone(BUSINESS_ZONE);
    }

    private static final class WeekTeamAgg {
        private final Long eventTeamId;
        private int matchCount;
        private int winCount;
        private int loseCount;
        private int pointDiff;
        private String teamName;
        private String teamLogoUrl;

        private WeekTeamAgg(Long eventTeamId) {
            this.eventTeamId = eventTeamId;
        }

        private void accept(boolean won, int pointsFor, int pointsAgainst, String name, String logoUrl) {
            matchCount++;
            if (won) {
                winCount++;
            } else {
                loseCount++;
            }
            pointDiff += pointsFor - pointsAgainst;
            if (!StringUtils.hasText(teamName) && StringUtils.hasText(name)) {
                teamName = name;
            }
            if (!StringUtils.hasText(teamLogoUrl) && StringUtils.hasText(logoUrl)) {
                teamLogoUrl = logoUrl;
            }
        }

        private BigDecimal winRate() {
            if (matchCount <= 0) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(winCount).divide(BigDecimal.valueOf(matchCount), 4, RoundingMode.HALF_UP);
        }

        private BigDecimal avgPointDiff() {
            if (matchCount <= 0) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(pointDiff).divide(BigDecimal.valueOf(matchCount), 1, RoundingMode.HALF_UP);
        }
    }
}
