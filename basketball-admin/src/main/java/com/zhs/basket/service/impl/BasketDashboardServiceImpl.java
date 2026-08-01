package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.mapper.BasketDashboardAdminMapper;
import com.zhs.basket.mapper.BasketTournamentMapper;
import com.zhs.basket.service.IBasketDashboardService;
import com.zhs.basket.support.BasketTournamentScopeSupport;
import com.zhs.basket.vo.DashboardDailyCountVo;
import com.zhs.basket.vo.DashboardSummaryVo;
import com.zhs.basket.vo.DashboardTrendSeriesVo;
import com.zhs.basket.vo.DashboardTrendSummaryVo;
import com.zhs.basket.vo.DashboardTrendVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class BasketDashboardServiceImpl implements IBasketDashboardService {

    private static final Logger log = LoggerFactory.getLogger(BasketDashboardServiceImpl.class);
    private static final int MAX_TREND_DAYS = 180;
    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter KEY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired private BasketDashboardAdminMapper dashboardMapper;
    @Autowired private BasketTournamentScopeSupport tournamentScopeSupport;
    @Autowired private BasketTournamentMapper tournamentMapper;

    @Override
    public DashboardSummaryVo getSummary() {
        List<Long> tournamentIds = tournamentScopeSupport.accessibleTournamentIds();
        if (!SecurityUtils.isAdmin() && (tournamentIds == null || tournamentIds.isEmpty())) {
            return emptySummary();
        }
        List<Long> scopedIds = SecurityUtils.isAdmin() ? null : tournamentIds;
        DashboardSummaryVo vo = new DashboardSummaryVo();
        vo.setPendingTeamAudit(dashboardMapper.countPendingTeamAudit());
        vo.setPendingPlayerCert(dashboardMapper.countPendingPlayerCert());
        vo.setPendingRegistration(dashboardMapper.countPendingRegistration(scopedIds));
        vo.setPendingOpenRegistration(dashboardMapper.countPendingOpenRegistration(scopedIds));
        vo.setPendingRosterChange(dashboardMapper.countPendingRosterChange(scopedIds));
        vo.setPendingPlatformChange(dashboardMapper.countPendingPlatformChange());
        vo.setPendingMatchDataAudit(dashboardMapper.countPendingMatchDataAudit(scopedIds));
        vo.setPendingMatchDispute(dashboardMapper.countPendingMatchDispute(scopedIds));
        fillCurrentTournament(vo, tournamentIds);
        return vo;
    }

    @Override
    public DashboardTrendVo getTrend(String rangeType, LocalDate startDate, LocalDate endDate, String metricGroup) {
        DateRange range = resolveDateRange(rangeType, startDate, endDate);
        List<LocalDate> dates = buildContinuousDates(range.startDate, range.endDate);
        List<String> dateList = dates.stream().map(d -> d.format(LABEL_FORMAT)).collect(Collectors.toList());

        List<MetricDefinition> metrics = resolveMetrics(metricGroup);
        List<DashboardTrendSeriesVo> series = new ArrayList<>();
        for (MetricDefinition metric : metrics) {
            List<DashboardDailyCountVo> dailyCounts = safeDailyCounts(() -> metric.dailySupplier.apply(range));
            DashboardTrendSeriesVo item = new DashboardTrendSeriesVo();
            item.setName(metric.name);
            item.setCode(metric.code);
            item.setData(fillSeriesData(dates, dailyCounts));
            series.add(item);
        }

        DashboardTrendVo vo = new DashboardTrendVo();
        vo.setDateList(dateList);
        vo.setSeries(series);
        vo.setSummary(buildTrendSummary(range));
        return vo;
    }

    private DashboardTrendSummaryVo buildTrendSummary(DateRange chartRange) {
        LocalDate today = LocalDate.now();
        Date todayStart = toDate(today);
        Date tomorrowStart = toDate(today.plusDays(1));
        LocalDate monthStart = today.withDayOfMonth(1);
        Date monthStartTime = toDate(monthStart);
        Date monthEndTime = tomorrowStart;

        DashboardTrendSummaryVo summary = new DashboardTrendSummaryVo();
        summary.setTodayNew(countAllNewBetween(todayStart, tomorrowStart));
        summary.setTodayHandled(countAllHandledBetween(todayStart, tomorrowStart));
        summary.setPendingTotal(countGlobalPendingTotal());
        summary.setMonthTotal(countAllNewBetween(monthStartTime, monthEndTime));
        return summary;
    }

    private int countGlobalPendingTotal() {
        return dashboardMapper.countPendingTeamAudit()
                + dashboardMapper.countPendingPlayerCert()
                + dashboardMapper.countPendingRegistration(null)
                + dashboardMapper.countPendingRosterChange(null)
                + dashboardMapper.countPendingPlatformChange()
                + dashboardMapper.countPendingMatchDataAudit(null)
                + dashboardMapper.countPendingMatchDispute(null);
    }

    private int countAllNewBetween(Date startTime, Date endTime) {
        int total = dashboardMapper.countPlayerCertNewBetween(startTime, endTime)
                + dashboardMapper.countTeamAuditNewBetween(startTime, endTime)
                + dashboardMapper.countTeamRegistrationNewBetween(startTime, endTime)
                + dashboardMapper.countRosterChangeNewBetween(startTime, endTime)
                + dashboardMapper.countPlatformChangeNewBetween(startTime, endTime)
                + dashboardMapper.countMatchDataSubmissionNewBetween(startTime, endTime)
                + dashboardMapper.countMatchDisputeNewBetween(startTime, endTime);
        total += safeCount(() -> dashboardMapper.countMatchExceptionNewBetween(startTime, endTime));
        return total;
    }

    private int countAllHandledBetween(Date startTime, Date endTime) {
        int total = dashboardMapper.countPlayerCertHandledBetween(startTime, endTime)
                + dashboardMapper.countTeamAuditHandledBetween(startTime, endTime)
                + dashboardMapper.countTeamRegistrationHandledBetween(startTime, endTime)
                + dashboardMapper.countRosterChangeHandledBetween(startTime, endTime)
                + dashboardMapper.countPlatformChangeHandledBetween(startTime, endTime)
                + dashboardMapper.countMatchDataHandledBetween(startTime, endTime)
                + dashboardMapper.countMatchDisputeHandledBetween(startTime, endTime);
        total += safeCount(() -> dashboardMapper.countMatchExceptionHandledBetween(startTime, endTime));
        return total;
    }

    private List<MetricDefinition> resolveMetrics(String metricGroup) {
        String group = metricGroup == null ? "ALL" : metricGroup.trim().toUpperCase();
        switch (group) {
            case "AUTH":
                return Arrays.asList(
                        metric("球员认证", "PLAYER_AUTH", dashboardMapper::countPlayerCertDaily),
                        metric("球队认证", "TEAM_AUTH", dashboardMapper::countTeamAuditDaily),
                        metric("平台游戏ID变更", "PLATFORM_CHANGE", dashboardMapper::countPlatformChangeDaily)
                );
            case "REGISTRATION":
                return Arrays.asList(
                        metric("球队报名", "TEAM_REGISTRATION", dashboardMapper::countTeamRegistrationDaily),
                        metric("阵容变更", "ROSTER_CHANGE", dashboardMapper::countRosterChangeDaily)
                );
            case "MATCH_DATA":
                return Arrays.asList(
                        metric("比赛数据", "MATCH_DATA", dashboardMapper::countMatchDataSubmissionDaily),
                        metric("数据争议", "DATA_DISPUTE", this::countDisputeAndExceptionDaily)
                );
            case "ALL":
            default:
                return Arrays.asList(
                        metric("球员认证", "PLAYER_AUTH", dashboardMapper::countPlayerCertDaily),
                        metric("球队认证", "TEAM_AUTH", dashboardMapper::countTeamAuditDaily),
                        metric("球队报名", "TEAM_REGISTRATION", dashboardMapper::countTeamRegistrationDaily),
                        metric("阵容变更", "ROSTER_CHANGE", dashboardMapper::countRosterChangeDaily),
                        metric("平台游戏ID变更", "PLATFORM_CHANGE", dashboardMapper::countPlatformChangeDaily),
                        metric("比赛数据", "MATCH_DATA", dashboardMapper::countMatchDataSubmissionDaily),
                        metric("数据争议", "DATA_DISPUTE", this::countDisputeAndExceptionDaily)
                );
        }
    }

    private List<DashboardDailyCountVo> countDisputeAndExceptionDaily(Date startTime, Date endTime) {
        Map<String, Integer> merged = new HashMap<>();
        mergeDailyCounts(merged, dashboardMapper.countMatchDisputeDaily(startTime, endTime));
        mergeDailyCounts(merged, safeDailyCounts(() -> dashboardMapper.countMatchExceptionDaily(startTime, endTime)));
        return merged.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    DashboardDailyCountVo vo = new DashboardDailyCountVo();
                    vo.setStatDate(entry.getKey());
                    vo.setTotal(entry.getValue());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private void mergeDailyCounts(Map<String, Integer> merged, List<DashboardDailyCountVo> counts) {
        for (DashboardDailyCountVo count : counts) {
            if (count == null || count.getStatDate() == null) {
                continue;
            }
            merged.merge(normalizeStatDate(count.getStatDate()), count.getTotal() == null ? 0 : count.getTotal(), Integer::sum);
        }
    }

    private MetricDefinition metric(String name, String code, DailyCounter counter) {
        MetricDefinition definition = new MetricDefinition();
        definition.name = name;
        definition.code = code;
        definition.dailySupplier = range -> counter.count(range.startTime, range.endTime);
        return definition;
    }

    private List<Integer> fillSeriesData(List<LocalDate> dates, List<DashboardDailyCountVo> counts) {
        Map<String, Integer> countMap = new HashMap<>();
        for (DashboardDailyCountVo count : counts) {
            if (count == null || count.getStatDate() == null) {
                continue;
            }
            countMap.put(normalizeStatDate(count.getStatDate()), count.getTotal() == null ? 0 : count.getTotal());
        }
        List<Integer> data = new ArrayList<>(dates.size());
        for (LocalDate date : dates) {
            data.add(countMap.getOrDefault(date.format(KEY_FORMAT), 0));
        }
        return data;
    }

    private String normalizeStatDate(String statDate) {
        if (statDate.length() >= 10) {
            return statDate.substring(0, 10);
        }
        return statDate;
    }

    private List<LocalDate> buildContinuousDates(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
            dates.add(cursor);
        }
        return dates;
    }

    private DateRange resolveDateRange(String rangeType, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        String type = rangeType == null ? "30d" : rangeType.trim().toLowerCase();
        LocalDate start;
        LocalDate end = today;
        switch (type) {
            case "7d":
                start = today.minusDays(6);
                break;
            case "90d":
                start = today.minusDays(89);
                break;
            case "custom":
                if (startDate == null || endDate == null) {
                    throw new ServiceException("自定义日期范围必须提供开始和结束日期");
                }
                if (endDate.isBefore(startDate)) {
                    throw new ServiceException("结束日期不能早于开始日期");
                }
                start = startDate;
                end = endDate;
                break;
            case "30d":
            default:
                start = today.minusDays(29);
                break;
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_TREND_DAYS) {
            throw new ServiceException("日期范围不能超过 " + MAX_TREND_DAYS + " 天");
        }
        DateRange range = new DateRange();
        range.startDate = start;
        range.endDate = end;
        range.startTime = toDate(start);
        range.endTime = toDate(end.plusDays(1));
        return range;
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private List<DashboardDailyCountVo> safeDailyCounts(Supplier<List<DashboardDailyCountVo>> supplier) {
        try {
            List<DashboardDailyCountVo> counts = supplier.get();
            return counts == null ? Collections.emptyList() : counts;
        } catch (Exception ex) {
            log.warn("Dashboard trend metric unavailable", ex);
            return Collections.emptyList();
        }
    }

    private int safeCount(Supplier<Integer> supplier) {
        try {
            Integer count = supplier.get();
            return count == null ? 0 : count;
        } catch (Exception ex) {
            log.warn("Dashboard trend count unavailable", ex);
            return 0;
        }
    }

    private void fillCurrentTournament(DashboardSummaryVo vo, List<Long> tournamentIds) {
        Long tournamentId = null;
        if (tournamentIds != null && !tournamentIds.isEmpty()) {
            tournamentId = tournamentIds.get(0);
        }
        if (tournamentId == null && SecurityUtils.isAdmin()) {
            return;
        }
        if (tournamentId != null) {
            BasketTournament tournament = tournamentMapper.selectById(tournamentId);
            if (tournament != null) {
                vo.setCurrentTournamentId(tournament.getId());
                vo.setCurrentTournamentName(tournament.getTournamentName());
            }
        }
    }

    private DashboardSummaryVo emptySummary() {
        DashboardSummaryVo vo = new DashboardSummaryVo();
        vo.setPendingTeamAudit(0);
        vo.setPendingPlayerCert(0);
        vo.setPendingRegistration(0);
        vo.setPendingOpenRegistration(0);
        vo.setPendingRosterChange(0);
        vo.setPendingPlatformChange(0);
        vo.setPendingMatchDataAudit(0);
        vo.setPendingMatchDispute(0);
        return vo;
    }

    @FunctionalInterface
    private interface DailyCounter {
        List<DashboardDailyCountVo> count(Date startTime, Date endTime);
    }

    private static class DateRange {
        private LocalDate startDate;
        private LocalDate endDate;
        private Date startTime;
        private Date endTime;
    }

    private static class MetricDefinition {
        private String name;
        private String code;
        private java.util.function.Function<DateRange, List<DashboardDailyCountVo>> dailySupplier;
    }
}
