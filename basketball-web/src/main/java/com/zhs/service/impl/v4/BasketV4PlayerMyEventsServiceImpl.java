package com.zhs.service.impl.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.exception.ServiceException;
import com.zhs.mapper.v4.BasketV4PlayerMyEventsMapper;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.response.v4.V4AvailableDivisionResp;
import com.zhs.response.v4.V4MyAvailableEventItemResp;
import com.zhs.response.v4.V4MyEventResp;
import com.zhs.response.v4.V4PersonalAvailableDivisionResp;
import com.zhs.service.v4.IBasketV4PersonalRegistrationService;
import com.zhs.service.v4.IBasketV4PlayerMyEventsService;
import com.zhs.service.v4.IBasketV4PlayerRegistrationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class BasketV4PlayerMyEventsServiceImpl implements IBasketV4PlayerMyEventsService {

    private static final Integer NOT_DELETED = 0;

    @Resource
    private BasketV4PlayerMyEventsMapper myEventsMapper;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private IBasketV4PlayerRegistrationService playerRegistrationService;
    @Resource
    private IBasketV4PersonalRegistrationService personalRegistrationService;

    @Override
    public List<V4MyEventResp> listMyEvents() {
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        List<V4MyEventResp> rows = myEventsMapper.selectMyEvents(memberId, player.getId());
        if (rows != null) {
            for (V4MyEventResp row : rows) {
                applyEventDisplay(row);
            }
        }
        return rows;
    }

    @Override
    public IPage<V4MyEventResp> pageMyEvents(Integer currentPage, Integer pageSize) {
        return toPage(listMyEvents(), currentPage, pageSize);
    }

    @Override
    public IPage<V4MyAvailableEventItemResp> pageAvailableEvents(Integer currentPage, Integer pageSize) {
        List<V4MyAvailableEventItemResp> merged = new ArrayList<>();
        List<V4PersonalAvailableDivisionResp> personalRows = personalRegistrationService.listAvailableDivisions();
        if (personalRows != null) {
            for (V4PersonalAvailableDivisionResp row : personalRows) {
                V4MyAvailableEventItemResp item = new V4MyAvailableEventItemResp();
                item.setSource("PERSONAL");
                item.setPersonalItem(row);
                merged.add(item);
            }
        }
        List<V4AvailableDivisionResp> teamRows = playerRegistrationService.listAvailableDivisions();
        if (teamRows != null) {
            for (V4AvailableDivisionResp row : teamRows) {
                V4MyAvailableEventItemResp item = new V4MyAvailableEventItemResp();
                item.setSource("TEAM");
                item.setTeamItem(row);
                merged.add(item);
            }
        }
        return toPage(merged, currentPage, pageSize);
    }

    private <T> IPage<T> toPage(List<T> items, Integer currentPage, Integer pageSize) {
        long page = currentPage == null || currentPage < 1 ? 1L : currentPage;
        long size = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, 50L);
        List<T> source = items == null ? Collections.emptyList() : items;
        long total = source.size();
        if (total == 0) {
            Page<T> empty = new Page<>(page, size, 0);
            empty.setRecords(Collections.emptyList());
            return empty;
        }
        long maxPage = Math.max(1L, (total + size - 1) / size);
        if (page > maxPage) {
            page = maxPage;
        }
        int from = (int) ((page - 1) * size);
        int to = (int) Math.min(from + size, total);
        Page<T> resp = new Page<>(page, size, total);
        resp.setRecords(from >= to ? Collections.emptyList() : source.subList(from, to));
        return resp;
    }

    private BasketPlayerProfile requirePlayerByMemberId(Long memberId) {
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            throw new ServiceException("当前账号未关联认证球员");
        }
        return profile;
    }

    private void applyEventDisplay(V4MyEventResp row) {
        if (row == null) {
            return;
        }
        boolean elimination = eliminationFormat(
                row.getLatestCompetitionFormat() != null ? row.getLatestCompetitionFormat() : row.getCompetitionFormat(),
                row.getLatestStageType() != null ? row.getLatestStageType() : row.getStageType());
        String format = row.getLatestCompetitionFormat() != null ? row.getLatestCompetitionFormat() : row.getCompetitionFormat();
        String stageType = row.getLatestStageType() != null ? row.getLatestStageType() : row.getStageType();
        boolean ended = "FINISHED".equals(row.getDivisionStatus())
                || "ARCHIVED".equals(row.getDivisionStatus())
                || (row.getMatchEndTime() != null && row.getMatchEndTime().before(new Date()));
        row.setEventEnded(ended);
        row.setPointsApplicable(!elimination);
        row.setFormatCode(elimination ? "ELIMINATION" : pointsFormat(format, stageType));
        if (elimination) {
            row.setStandingRank(null);
            row.setStandingPoints(null);
        }
        if (ended) {
            row.setCurrentStageName(null);
            row.setCurrentGroupName(null);
            row.setNextMatchId(null);
            row.setNextMatchTime(null);
            if (StringUtils.hasText(row.getPublishedResultLabel())) {
                row.setFinalResultCode("HONOR");
                row.setFinalResultLabel(row.getPublishedResultLabel().trim());
            } else if (elimination && row.getFinalSeriesWinnerTeamId() != null
                    && Objects.equals(row.getFinalSeriesWinnerTeamId(), row.getEventTeamId())) {
                row.setFinalResultCode("CHAMPION");
            } else if (elimination) {
                row.setFinalResultCode("PENDING");
            } else if (row.getStandingRank() != null) {
                row.setFinalResultCode("RANK");
                row.setFinalResultRank(row.getStandingRank());
            } else {
                row.setFinalResultCode("PENDING");
            }
            return;
        }
        row.setCurrentStageName(StringUtils.hasText(row.getOpenStageName()) ? row.getOpenStageName() : null);
        if (!StringUtils.hasText(row.getOpenStageName())) {
            row.setCurrentGroupName(null);
        }
    }

    private boolean eliminationFormat(String format, String stageType) {
        String value = ((format == null ? "" : format) + " " + (stageType == null ? "" : stageType)).toUpperCase();
        return value.contains("ELIM") || value.contains("KNOCK") || value.contains("FINAL");
    }

    private String pointsFormat(String format, String stageType) {
        String value = ((format == null ? "" : format) + " " + (stageType == null ? "" : stageType)).toUpperCase();
        if (value.contains("GROUP")) {
            return "GROUP";
        }
        if (value.contains("ROUND") || value.contains("REGULAR")) {
            return "ROUND_ROBIN";
        }
        return null;
    }
}