package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.common.exception.ServiceException;

import java.util.Date;

public final class RosterChangeWindowSupport {

    private RosterChangeWindowSupport() {
    }

    /**
     * 已排赛但比赛未开始，仍允许调整参赛名单。
     * 比赛已开始、已结束、弃权，或已有球员统计时拒绝。
     */
    public static boolean isRosterFrozen(String divisionStatus, int startedMatchCount, int playerStatCount) {
        return DivisionStatusEnum.FINISHED.getCode().equals(divisionStatus)
                || DivisionStatusEnum.ARCHIVED.getCode().equals(divisionStatus)
                || startedMatchCount > 0
                || playerStatCount > 0;
    }

    public static void assertCanRequest(BasketDivision division, Date now, int startedMatchCount, int playerStatCount) {
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        if (DivisionStatusEnum.FINISHED.getCode().equals(division.getDivisionStatus())
                || DivisionStatusEnum.ARCHIVED.getCode().equals(division.getDivisionStatus())) {
            throw new ServiceException("赛区已结束或归档，不能调整参赛名单");
        }
        if (startedMatchCount > 0 || playerStatCount > 0) {
            throw new ServiceException("比赛已开始或已有球员统计，不能调整参赛名单");
        }
    }
}
