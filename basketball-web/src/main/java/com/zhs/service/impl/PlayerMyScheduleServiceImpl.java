package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.PlayerMyScheduleDao;
import com.zhs.dto.PlayerMyScheduleEventOptionRow;
import com.zhs.dto.PlayerMyScheduleRow;
import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.request.matchdata.PlayerMyScheduleQuery;
import com.zhs.response.matchdata.PlayerMatchTaskResp;
import com.zhs.response.matchdata.PlayerMyScheduleEventOptionResp;
import com.zhs.response.matchdata.PlayerMySchedulePageResp;
import com.zhs.service.IMPlayerMatchDataService;
import com.zhs.service.IPlayerMyScheduleService;
import com.zhs.service.support.BasketPublicConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlayerMyScheduleServiceImpl implements IPlayerMyScheduleService {

    private static final Integer NOT_DELETED = 0;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Set<String> ALLOWED_SORT = Set.of("ASC", "DESC");
    private static final Set<String> ALLOWED_STATUS = Set.of("PENDING", "RUNNING", "FINISHED", "CANCELLED", "ALL");

    @Resource
    private PlayerMyScheduleDao scheduleDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private IMPlayerMatchDataService playerMatchDataService;

    @Override
    public PlayerMySchedulePageResp pageMySchedules(PlayerMyScheduleQuery query) {
        Long memberId = UserContext.getRequiredMemberId();
        PlayerMySchedulePageResp resp = new PlayerMySchedulePageResp();

        BasketPlayerProfile profile = findApprovedProfile(memberId);
        if (profile == null) {
            resp.setCertificationRequired(true);
            resp.setTotal(0);
            resp.setRecords(Collections.emptyList());
            return resp;
        }

        PlayerMyScheduleQuery effective = normalizeQuery(query);
        validateSeasonAccess(memberId, profile.getId(), effective.getSeasonId());

        Date startTime = parseStartDate(effective.getStartDate());
        Date endTimeExclusive = parseEndDateExclusive(effective.getEndDate());
        if (startTime != null && endTimeExclusive != null && !startTime.before(endTimeExclusive)) {
            throw new ServiceException("开始日期不能晚于结束日期");
        }

        long total = scheduleDao.countMySchedule(memberId, profile.getId(), effective, startTime, endTimeExclusive);
        resp.setTotal(total);
        resp.setPageNum(effective.getPageNum());
        resp.setPageSize(effective.getPageSize());

        if (total == 0) {
            List<PlayerMyScheduleEventOptionRow> options = scheduleDao.selectEventOptions(memberId, profile.getId());
            resp.setNoParticipation(options.isEmpty());
            resp.setRecords(Collections.emptyList());
            return resp;
        }

        int offset = (effective.getPageNum() - 1) * effective.getPageSize();
        List<PlayerMyScheduleRow> rows = scheduleDao.selectMySchedulePage(
                memberId, profile.getId(), effective, startTime, endTimeExclusive, offset, effective.getPageSize());

        Map<Long, Long> matchEventTeamMap = new LinkedHashMap<>();
        for (PlayerMyScheduleRow row : rows) {
            if (row.getMatchId() != null && row.getMyEventTeamId() != null) {
                matchEventTeamMap.put(row.getMatchId(), row.getMyEventTeamId());
            }
        }

        List<PlayerMatchTaskResp> records = playerMatchDataService.buildTaskItemsForEventTeams(memberId, matchEventTeamMap);
        resp.setRecords(records);

        if (isPendingDefaultQuery(effective) && effective.getPageNum() == 1 && !records.isEmpty()) {
            resp.setNextMatch(records.get(0));
        } else if ("PENDING".equalsIgnoreCase(effective.getMatchStatusGroup()) && effective.getPageNum() == 1) {
            resp.setNextMatch(findNextPendingMatch(memberId, profile.getId(), effective, startTime, endTimeExclusive));
        }
        return resp;
    }

    @Override
    public List<PlayerMyScheduleEventOptionResp> listEventOptions() {
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile profile = findApprovedProfile(memberId);
        if (profile == null) {
            return Collections.emptyList();
        }
        return scheduleDao.selectEventOptions(memberId, profile.getId()).stream()
                .map(this::toEventOption)
                .collect(Collectors.toList());
    }

    private PlayerMatchTaskResp findNextPendingMatch(Long memberId, Long playerId,
                                                     PlayerMyScheduleQuery query,
                                                     Date startTime, Date endTimeExclusive) {
        PlayerMyScheduleQuery pendingQuery = new PlayerMyScheduleQuery();
        pendingQuery.setSeasonId(query.getSeasonId());
        pendingQuery.setMatchStatusGroup("PENDING");
        pendingQuery.setStartDate(query.getStartDate());
        pendingQuery.setEndDate(query.getEndDate());
        pendingQuery.setSortOrder("ASC");
        pendingQuery.setPageNum(1);
        pendingQuery.setPageSize(1);
        List<PlayerMyScheduleRow> rows = scheduleDao.selectMySchedulePage(
                memberId, playerId, pendingQuery, startTime, endTimeExclusive, 0, 1);
        if (rows.isEmpty()) {
            return null;
        }
        PlayerMyScheduleRow row = rows.get(0);
        Map<Long, Long> map = Map.of(row.getMatchId(), row.getMyEventTeamId());
        List<PlayerMatchTaskResp> items = playerMatchDataService.buildTaskItemsForEventTeams(memberId, map);
        return items.isEmpty() ? null : items.get(0);
    }

    private boolean isPendingDefaultQuery(PlayerMyScheduleQuery query) {
        return "PENDING".equalsIgnoreCase(query.getMatchStatusGroup())
                && "ASC".equalsIgnoreCase(query.getSortOrder());
    }

    private PlayerMyScheduleQuery normalizeQuery(PlayerMyScheduleQuery query) {
        PlayerMyScheduleQuery effective = query == null ? new PlayerMyScheduleQuery() : query;
        int pageNum = effective.getPageNum() == null || effective.getPageNum() < 1
                ? BasketPublicConstants.DEFAULT_PAGE : effective.getPageNum();
        int pageSize = effective.getPageSize() == null || effective.getPageSize() < 1
                ? BasketPublicConstants.DEFAULT_SIZE
                : Math.min(effective.getPageSize(), BasketPublicConstants.MAX_SIZE);
        effective.setPageNum(pageNum);
        effective.setPageSize(pageSize);

        String statusGroup = StringUtils.hasText(effective.getMatchStatusGroup())
                ? effective.getMatchStatusGroup().trim().toUpperCase()
                : "PENDING";
        if (!ALLOWED_STATUS.contains(statusGroup)) {
            throw new ServiceException("比赛状态筛选无效");
        }
        effective.setMatchStatusGroup(statusGroup);

        String sortOrder = StringUtils.hasText(effective.getSortOrder())
                ? effective.getSortOrder().trim().toUpperCase()
                : defaultSortOrder(statusGroup);
        if (!ALLOWED_SORT.contains(sortOrder)) {
            throw new ServiceException("排序方式无效");
        }
        effective.setSortOrder(sortOrder);
        return effective;
    }

    private String defaultSortOrder(String statusGroup) {
        if ("FINISHED".equals(statusGroup)) {
            return "DESC";
        }
        return "ASC";
    }

    private void validateSeasonAccess(Long memberId, Long playerId, Long seasonId) {
        if (seasonId == null) {
            return;
        }
        List<PlayerMyScheduleEventOptionRow> options = scheduleDao.selectEventOptions(memberId, playerId);
        boolean allowed = options.stream().anyMatch(item -> Objects.equals(item.getSeasonId(), seasonId));
        if (!allowed) {
            throw new ServiceException("无权查看该赛事赛程", 403);
        }
    }

    private BasketPlayerProfile findApprovedProfile(Long memberId) {
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            return null;
        }
        if (!Objects.equals(PlayerCertStatusEnum.APPROVED.getCode(), profile.getCertificationStatus())) {
            return null;
        }
        return profile;
    }

    private Date parseStartDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        LocalDate date = LocalDate.parse(value.trim(), DATE_FORMAT);
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date parseEndDateExclusive(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        LocalDate date = LocalDate.parse(value.trim(), DATE_FORMAT);
        LocalDateTime nextDay = date.plusDays(1).atStartOfDay();
        return Date.from(nextDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    private PlayerMyScheduleEventOptionResp toEventOption(PlayerMyScheduleEventOptionRow row) {
        PlayerMyScheduleEventOptionResp resp = new PlayerMyScheduleEventOptionResp();
        resp.setSeasonId(row.getSeasonId() == null ? null : String.valueOf(row.getSeasonId()));
        resp.setSeasonName(row.getSeasonName());
        resp.setEventName(StringUtils.hasText(row.getTournamentName()) ? row.getTournamentName() : row.getSeasonName());
        resp.setEventCode(row.getSeasonCode());
        return resp;
    }
}
