package com.zhs.support;

import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.enums.v4.DivisionStatusEnum;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.v4.BasketDivision;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 第四批「当前球队 / 历史球队」归类规则（与 AccountTeamServiceImpl 语义对齐）。
 */
class TeamMineScopeRulesTest {

    @Test
    void currentRequiresActiveMembershipAndOpenDivision() {
        BasketTeam team = team(118L, TeamStatusEnum.ACTIVE.getCode());
        BasketTeamMember membership = membership(118L, TeamMembershipStatusEnum.ACTIVE.getCode(), 1);
        BasketDivision division = division(118L, DivisionStatusEnum.REGISTRATION_OPEN.getCode());
        assertTrue(isCurrent(team, membership, division));
    }

    @Test
    void finishedDivisionIsNotCurrent() {
        BasketTeam team = team(128L, TeamStatusEnum.ACTIVE.getCode());
        BasketTeamMember membership = membership(128L, TeamMembershipStatusEnum.ACTIVE.getCode(), 1);
        BasketDivision division = division(128L, DivisionStatusEnum.FINISHED.getCode());
        assertFalse(isCurrent(team, membership, division));
    }

    @Test
    void archivedTeamIsNotCurrent() {
        BasketTeam team = team(128L, TeamStatusEnum.ARCHIVED.getCode());
        BasketTeamMember membership = membership(128L, TeamMembershipStatusEnum.LEFT.getCode(), 0);
        BasketDivision division = division(128L, DivisionStatusEnum.FINISHED.getCode());
        assertFalse(isCurrent(team, membership, division));
    }

    @Test
    void legacyTeamIsNotCurrent() {
        BasketTeam team = team(null, TeamStatusEnum.ACTIVE.getCode());
        BasketTeamMember membership = membership(null, TeamMembershipStatusEnum.ACTIVE.getCode(), 1);
        assertFalse(isCurrent(team, membership, null));
    }

    @Test
    void leftMembershipIsHistoryEvenIfTeamStillActive() {
        BasketTeam team = team(120L, TeamStatusEnum.ACTIVE.getCode());
        BasketTeamMember membership = membership(120L, TeamMembershipStatusEnum.REMOVED.getCode(), 0);
        BasketDivision division = division(120L, DivisionStatusEnum.PLAYING.getCode());
        assertFalse(isCurrent(team, membership, division));
    }

    private static boolean isCurrent(BasketTeam team, BasketTeamMember membership, BasketDivision division) {
        if (team == null || membership == null) {
            return false;
        }
        if (team.getDivisionId() == null || membership.getDivisionId() == null) {
            return false;
        }
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            return false;
        }
        if (!Objects.equals(TeamMembershipStatusEnum.ACTIVE.getCode(), membership.getMembershipStatus())
                || !Objects.equals(1, membership.getIsCurrent())) {
            return false;
        }
        if (division == null || !Objects.equals(0, division.getIsDelete())) {
            return false;
        }
        String status = division.getDivisionStatus();
        return !DivisionStatusEnum.FINISHED.getCode().equals(status)
                && !DivisionStatusEnum.ARCHIVED.getCode().equals(status);
    }

    private static BasketTeam team(Long divisionId, Integer status) {
        BasketTeam team = new BasketTeam();
        team.setId(1L);
        team.setDivisionId(divisionId);
        team.setTeamStatus(status);
        team.setIsDelete(0);
        return team;
    }

    private static BasketTeamMember membership(Long divisionId, Integer status, Integer isCurrent) {
        BasketTeamMember membership = new BasketTeamMember();
        membership.setTeamId(1L);
        membership.setDivisionId(divisionId);
        membership.setMembershipStatus(status);
        membership.setIsCurrent(isCurrent);
        membership.setIsDelete(0);
        return membership;
    }

    private static BasketDivision division(Long id, String status) {
        BasketDivision division = new BasketDivision();
        division.setId(id);
        division.setDivisionStatus(status);
        division.setIsDelete(0);
        return division;
    }
}
