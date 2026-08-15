package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.ScheduleUnitTypeEnum;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import com.zhs.common.exception.ServiceException;

public final class ScheduleAdjustmentGuard {

    public static final String MSG_REASON_REQUIRED = "调整已发布比赛必须填写原因";
    public static final String MSG_NOT_PUBLISHED = "赛段尚未发布，请前往赛事工作台调整排期。";
    public static final String MSG_NOT_OPERATIONAL = "赛段尚未发布，请前往赛事工作台调整排期。";
    public static final String MSG_SERIES_STARTED = "该系列赛已经开始，不能使用普通时间调整。";
    public static final String MSG_SERIES_FINISHED = "系列赛已结束，不能修改日期时间";
    public static final String MSG_MATCH_STARTED = "比赛已经开始，不能使用普通时间调整。";
    public static final String MSG_MATCH_FINISHED = "比赛已结束，不能修改日期时间";
    public static final String MSG_CHILD_MATCH = "该比赛属于 BO 系列赛，请前往『系列赛』调整整个系列赛时间。";

    private ScheduleAdjustmentGuard() {}

    public static void assertReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ServiceException(MSG_REASON_REQUIRED);
        }
    }

    public static void assertPublished(BasketScheduleBatch batch) {
        if (batch == null || !PublishStatusEnum.PUBLISHED.getCode().equals(batch.getPublishStatus())) {
            throw new ServiceException(MSG_NOT_PUBLISHED);
        }
    }

    public static boolean isSeriesStarted(String seriesStatus, boolean childStarted) {
        if (childStarted) {
            return true;
        }
        return MatchSeriesStatusEnum.RUNNING.getCode().equals(seriesStatus)
                || "IN_PROGRESS".equalsIgnoreCase(seriesStatus);
    }

    public static boolean isSeriesFinished(String seriesStatus) {
        return MatchSeriesStatusEnum.COMPLETED.getCode().equals(seriesStatus)
                || MatchSeriesStatusEnum.CANCELLED.getCode().equals(seriesStatus)
                || "FORFEITED".equalsIgnoreCase(seriesStatus)
                || "FINISHED".equalsIgnoreCase(seriesStatus);
    }

    public static boolean isMatchStarted(String matchStatus) {
        return MatchStatusEnum.PLAYING.getCode().equals(matchStatus)
                || "IN_PROGRESS".equalsIgnoreCase(matchStatus);
    }

    public static boolean isMatchFinished(String matchStatus) {
        return MatchStatusEnum.FINISHED.getCode().equals(matchStatus)
                || MatchStatusEnum.FORFEITED.getCode().equals(matchStatus)
                || MatchStatusEnum.CANCELLED.getCode().equals(matchStatus);
    }

    public static String matchManagementBlockReason(String publishStatus, String seriesStatus, boolean childStarted) {
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(publishStatus)) {
            return MSG_NOT_OPERATIONAL;
        }
        if (isSeriesFinished(seriesStatus)) {
            return MSG_SERIES_FINISHED;
        }
        if (isSeriesStarted(seriesStatus, childStarted)) {
            return MSG_SERIES_STARTED;
        }
        return null;
    }

    public static boolean canAdjustInMatchManagement(String publishStatus, String seriesStatus, boolean childStarted) {
        return matchManagementBlockReason(publishStatus, seriesStatus, childStarted) == null;
    }

    public static boolean isSeriesChildMatch(Long seriesId) {
        return seriesId != null && seriesId > 0;
    }

    public static String matchManagementMatchBlockReason(Long seriesId, String publishStatus, String matchStatus) {
        if (isSeriesChildMatch(seriesId)) {
            return MSG_CHILD_MATCH;
        }
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(publishStatus)) {
            return MSG_NOT_OPERATIONAL;
        }
        if (isMatchFinished(matchStatus)) {
            return MSG_MATCH_FINISHED;
        }
        if (isMatchStarted(matchStatus)) {
            return MSG_MATCH_STARTED;
        }
        return null;
    }

    public static boolean canAdjustStandaloneMatch(Long seriesId, String publishStatus, String matchStatus) {
        return matchManagementMatchBlockReason(seriesId, publishStatus, matchStatus) == null;
    }

    public static void assertStandaloneMatch(Long seriesId) {
        if (isSeriesChildMatch(seriesId)) {
            throw new ServiceException(MSG_CHILD_MATCH);
        }
    }

    public static boolean canAdjust(ScheduleUnitView unit, boolean childStarted) {
        if (unit == null || !unit.isRequired()) {
            return false;
        }
        if (ScheduleUnitTypeEnum.MATCH_SERIES.getCode().equals(unit.getUnitType())) {
            return !isSeriesFinished(unit.getSeriesStatus()) && !isSeriesStarted(unit.getSeriesStatus(), childStarted);
        }
        return !isMatchFinished(unit.getMatchStatus()) && !isMatchStarted(unit.getMatchStatus());
    }

    public static void assertCanAdjust(ScheduleUnitView unit, boolean childStarted) {
        if (unit == null) {
            throw new ServiceException("排期单位不存在");
        }
        if (ScheduleUnitTypeEnum.MATCH_SERIES.getCode().equals(unit.getUnitType())) {
            if (isSeriesFinished(unit.getSeriesStatus())) {
                throw new ServiceException(MSG_SERIES_FINISHED);
            }
            if (isSeriesStarted(unit.getSeriesStatus(), childStarted)) {
                throw new ServiceException(MSG_SERIES_STARTED);
            }
            return;
        }
        if (isMatchFinished(unit.getMatchStatus())) {
            throw new ServiceException(MSG_MATCH_FINISHED);
        }
        if (isMatchStarted(unit.getMatchStatus())) {
            throw new ServiceException(MSG_MATCH_STARTED);
        }
    }

    public static String formatAuditReason(String unitType, Long unitId, String oldDate, String oldTime,
                                           String newDate, String newTime, String reason) {
        String body = "ADJUST " + safe(unitType) + ":" + (unitId == null ? "-" : unitId)
                + " " + safe(oldDate) + " " + shortTime(oldTime)
                + " -> " + safe(newDate) + " " + shortTime(newTime)
                + " | " + reason.trim();
        return body.length() <= 500 ? body : body.substring(0, 500);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static String shortTime(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String raw = value.trim();
        return raw.length() >= 5 ? raw.substring(0, 5) : raw;
    }
}
