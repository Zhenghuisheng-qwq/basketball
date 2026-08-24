package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.vo.v4.ScheduleUnitView;
import com.zhs.common.exception.ServiceException;

/**
 * Pairing resolve is independent from schedule.
 * Unresolved placeholder sides may be scheduled, but cannot execute results.
 */
public final class PairingResolutionGuard {

    public static final String MSG_EXECUTION_BLOCKED = "对阵尚未确定，不能录入比赛结果或弃赛";
    public static final String MSG_ILLEGAL_PLACEHOLDER = "对阵来源不完整，不能排期";

    private PairingResolutionGuard() {
    }

    public static boolean isResolved(Long homeEventTeamId, Long awayEventTeamId) {
        return positive(homeEventTeamId) != null && positive(awayEventTeamId) != null;
    }

    public static boolean isLegalPlaceholderSide(Long eventTeamId, String sourceType, Long sourceSeriesId) {
        if (positive(eventTeamId) != null) {
            return true;
        }
        if ("WINNER_OF_SERIES".equals(sourceType) || "GROUP_RANK".equals(sourceType)) {
            return true;
        }
        return sourceSeriesId != null && sourceSeriesId > 0;
    }

    public static boolean isLegalPlaceholder(Long homeEventTeamId, Long awayEventTeamId,
                                             String homeSourceType, String awaySourceType,
                                             Long homeSourceSeriesId, Long awaySourceSeriesId) {
        return isLegalPlaceholderSide(homeEventTeamId, homeSourceType, homeSourceSeriesId)
                && isLegalPlaceholderSide(awayEventTeamId, awaySourceType, awaySourceSeriesId);
    }

    public static void assertResolvedForExecution(Long homeEventTeamId, Long awayEventTeamId) {
        if (!isResolved(homeEventTeamId, awayEventTeamId)) {
            throw new ServiceException(MSG_EXECUTION_BLOCKED);
        }
    }

    public static boolean isSchedulable(ScheduleUnitView unit) {
        if (unit == null || !unit.isRequired()) {
            return false;
        }
        if (isResolved(unit.getHomeParticipantId(), unit.getAwayParticipantId())) {
            return true;
        }
        return isLegalPlaceholder(unit.getHomeParticipantId(), unit.getAwayParticipantId(),
                unit.getHomeSourceType(), unit.getAwaySourceType(), null, null);
    }

    public static void assertSchedulable(ScheduleUnitView unit) {
        if (!isSchedulable(unit)) {
            throw new ServiceException(MSG_ILLEGAL_PLACEHOLDER);
        }
    }

    private static Long positive(Long id) {
        return id == null || id <= 0 ? null : id;
    }
}
