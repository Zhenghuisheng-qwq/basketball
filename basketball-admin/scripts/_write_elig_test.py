from pathlib import Path

# --- unit test ---
Path(r"d:\code\dz\basketball\scaffolding-v2\basketball-web\src\test\java\com\zhs\support\v4\TeamRegistrationEligibilitySupportTest.java").write_text(r'''package com.zhs.support.v4;

import com.zhs.enums.TeamAuditStatusEnum;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.enums.v4.DivisionStatusEnum;
import com.zhs.enums.v4.RegistrationEligibilityReasonEnum;
import com.zhs.enums.v4.RegistrationStatusEnum;
import com.zhs.enums.v4.RegistrationWindowStatusEnum;
import com.zhs.model.BasketTeam;
import com.zhs.model.v4.BasketCaptainGrant;
import com.zhs.model.v4.BasketDivision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class TeamRegistrationEligibilitySupportTest {

    private TeamRegistrationEligibilitySupport support;
    private Date now;

    @BeforeEach
    void setUp() {
        support = new TeamRegistrationEligibilitySupport();
        now = fixed(2026, Calendar.AUGUST, 11, 17, 0, 0);
    }

    @Test
    void case1_notStarted() {
        TeamRegistrationEligibility e = support.evaluate(
                division(DivisionStatusEnum.REGISTRATION_OPEN, fixed(2026, Calendar.AUGUST, 12, 0, 0, 0), fixed(2026, Calendar.AUGUST, 20, 0, 0, 0)),
                usableGrant(), approvedTeam(), null, 10, now);
        assertFalse(e.isCanRegister());
        assertEquals(RegistrationWindowStatusEnum.NOT_STARTED, e.getWindowStatus());
        assertEquals(RegistrationEligibilityReasonEnum.NOT_STARTED, e.getReason());
    }

    @Test
    void case2_openWithGrantAndApprovedTeam() {
        Date start = fixed(2026, Calendar.JULY, 31, 16, 0, 0);
        Date end = fixed(2026, Calendar.AUGUST, 12, 16, 0, 0);
        TeamRegistrationEligibility e = support.evaluate(
                division(DivisionStatusEnum.REGISTRATION_OPEN, start, end),
                usableGrant(), approvedTeam(), null, 10, now);
        assertTrue(e.isCanRegister());
        assertEquals(RegistrationWindowStatusEnum.OPEN, e.getWindowStatus());
        assertNull(e.getReason());
    }

    @Test
    void case3_closedWindow() {
        Date start = fixed(2026, Calendar.JULY, 31, 16, 0, 0);
        Date end = fixed(2026, Calendar.AUGUST, 11, 16, 0, 0);
        TeamRegistrationEligibility e = support.evaluate(
                division(DivisionStatusEnum.REGISTRATION_OPEN, start, end),
                usableGrant(), approvedTeam(), null, 10, now);
        assertFalse(e.isCanRegister());
        assertEquals(RegistrationWindowStatusEnum.CLOSED, e.getWindowStatus());
        assertEquals(RegistrationEligibilityReasonEnum.REGISTRATION_CLOSED, e.getReason());
        assertTrue(e.getReasonMessage().contains("截止"));
    }

    @Test
    void case4_noGrant() {
        TeamRegistrationEligibility e = support.evaluate(
                openDivision(), null, approvedTeam(), null, 10, now);
        assertFalse(e.isCanRegister());
        assertEquals(RegistrationEligibilityReasonEnum.NO_CAPTAIN_GRANT, e.getReason());
    }

    @Test
    void case5_teamDraft() {
        BasketTeam team = approvedTeam();
        team.setAuditStatus(TeamAuditStatusEnum.DRAFT.getCode());
        TeamRegistrationEligibility e = support.evaluate(openDivision(), usableGrant(), team, null, 10, now);
        assertEquals(RegistrationEligibilityReasonEnum.TEAM_NOT_CERTIFIED, e.getReason());
    }

    @Test
    void case6_teamPending() {
        BasketTeam team = approvedTeam();
        team.setAuditStatus(TeamAuditStatusEnum.PENDING.getCode());
        TeamRegistrationEligibility e = support.evaluate(openDivision(), usableGrant(), team, null, 10, now);
        assertEquals(RegistrationEligibilityReasonEnum.TEAM_NOT_CERTIFIED, e.getReason());
    }

    @Test
    void case7_teamRejected() {
        BasketTeam team = approvedTeam();
        team.setAuditStatus(TeamAuditStatusEnum.REJECTED.getCode());
        TeamRegistrationEligibility e = support.evaluate(openDivision(), usableGrant(), team, null, 10, now);
        assertEquals(RegistrationEligibilityReasonEnum.TEAM_NOT_CERTIFIED, e.getReason());
    }

    @Test
    void case8_approvedCanRegister() {
        TeamRegistrationEligibility e = support.evaluate(
                openDivision(), usableGrant(), approvedTeam(), null, 10, now);
        assertTrue(e.isCanRegister());
    }

    @Test
    void case9_alreadyRegisteredAfterClose_canView() {
        Date start = fixed(2026, Calendar.JULY, 31, 16, 0, 0);
        Date end = fixed(2026, Calendar.AUGUST, 11, 16, 0, 0);
        TeamRegistrationEligibility e = support.evaluate(
                division(DivisionStatusEnum.REGISTRATION_OPEN, start, end),
                usableGrant(), approvedTeam(), RegistrationStatusEnum.PENDING_REVIEW.getCode(), 10, now);
        assertFalse(e.isCanRegister());
        assertTrue(e.isCanViewRegistration());
        assertEquals(RegistrationEligibilityReasonEnum.REGISTRATION_CLOSED, e.getReason());
    }

    @Test
    void case10_divisionMissing() {
        TeamRegistrationEligibility e = support.evaluate(null, null, null, null, null, now);
        assertEquals(RegistrationEligibilityReasonEnum.DIVISION_NOT_FOUND, e.getReason());
        assertFalse(e.isCanViewRegistration());
    }

    @Test
    void boundary_endTimeInclusive() {
        Date end = fixed(2026, Calendar.AUGUST, 11, 17, 0, 0);
        TeamRegistrationEligibility e = support.evaluate(
                division(DivisionStatusEnum.REGISTRATION_OPEN, fixed(2026, Calendar.JULY, 1, 0, 0, 0), end),
                usableGrant(), approvedTeam(), null, 10, now);
        assertTrue(e.isCanRegister());
        assertEquals(RegistrationWindowStatusEnum.OPEN, e.getWindowStatus());
    }

    private BasketDivision openDivision() {
        return division(DivisionStatusEnum.REGISTRATION_OPEN,
                fixed(2026, Calendar.JULY, 31, 16, 0, 0),
                fixed(2026, Calendar.AUGUST, 20, 16, 0, 0));
    }

    private BasketDivision division(DivisionStatusEnum status, Date start, Date end) {
        BasketDivision d = new BasketDivision();
        d.setId(100L);
        d.setDivisionStatus(status.getCode());
        d.setRegistrationStartTime(start);
        d.setRegistrationEndTime(end);
        d.setIsDelete(0);
        return d;
    }

    private BasketCaptainGrant usableGrant() {
        BasketCaptainGrant g = new BasketCaptainGrant();
        g.setDivisionId(100L);
        g.setTeamId(1L);
        g.setGrantStatus("ACTIVE");
        return g;
    }

    private BasketTeam approvedTeam() {
        BasketTeam t = new BasketTeam();
        t.setId(1L);
        t.setDivisionId(100L);
        t.setTeamStatus(TeamStatusEnum.ACTIVE.getCode());
        t.setAuditStatus(TeamAuditStatusEnum.APPROVED.getCode());
        t.setIsDelete(0);
        return t;
    }

    private Date fixed(int year, int month, int day, int hour, int minute, int second) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
''', encoding='utf-8', newline='\n')
print('test ok')
