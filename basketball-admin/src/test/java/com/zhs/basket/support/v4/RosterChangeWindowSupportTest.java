package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RosterChangeWindowSupportTest {

    @Test
    void openWindowAllowsChange() {
        assertDoesNotThrow(() -> RosterChangeWindowSupport.assertCanRequest(openDivision(), new Date(), 0, 0));
    }

    @Test
    void closedRegistrationStillAllowsChangeBeforeMatch() {
        BasketDivision division = openDivision();
        division.setRegistrationEndTime(new Date(System.currentTimeMillis() - 60_000L));
        assertDoesNotThrow(() -> RosterChangeWindowSupport.assertCanRequest(division, new Date(), 0, 0));
    }

    @Test
    void scheduledButNotStartedStillAllowsChange() {
        assertDoesNotThrow(() -> RosterChangeWindowSupport.assertCanRequest(openDivision(), new Date(), 0, 0));
    }

    @Test
    void startedMatchOrStatsReject() {
        ServiceException started = assertThrows(ServiceException.class,
                () -> RosterChangeWindowSupport.assertCanRequest(openDivision(), new Date(), 1, 0));
        assertEquals("比赛已开始或已有球员统计，不能调整参赛名单", started.getMessage());
        ServiceException stats = assertThrows(ServiceException.class,
                () -> RosterChangeWindowSupport.assertCanRequest(openDivision(), new Date(), 0, 1));
        assertEquals("比赛已开始或已有球员统计，不能调整参赛名单", stats.getMessage());
    }

    @Test
    void finishedDivisionRejects() {
        BasketDivision division = openDivision();
        division.setDivisionStatus(DivisionStatusEnum.FINISHED.getCode());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> RosterChangeWindowSupport.assertCanRequest(division, new Date(), 0, 0));
        assertEquals("赛区已结束或归档，不能调整参赛名单", ex.getMessage());
    }

    private BasketDivision openDivision() {
        BasketDivision division = new BasketDivision();
        division.setDivisionStatus(DivisionStatusEnum.REGISTRATION_OPEN.getCode());
        division.setRegistrationStartTime(new Date(System.currentTimeMillis() - 86_400_000L));
        division.setRegistrationEndTime(new Date(System.currentTimeMillis() + 86_400_000L));
        return division;
    }
}
