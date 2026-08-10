package com.zhs.support;

import com.zhs.enums.TeamStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketTeam;
import com.zhs.model.v4.BasketCaptainGrant;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 报名入口 Division 一致性守卫（与 BasketV4PlayerRegistrationServiceImpl 规则对齐的纯逻辑校验）。
 */
class RegistrationTeamDivisionGuardTest {

    @Test
    void rejectsLegacyTeamRegistration() {
        BasketTeam team = new BasketTeam();
        team.setId(1L);
        team.setDivisionId(null);
        team.setTeamStatus(TeamStatusEnum.ACTIVE.getCode());
        assertTrue(isRejectedForRegistration(team, 118L));
    }

    @Test
    void rejectsCrossDivisionRegistration() {
        BasketTeam team = new BasketTeam();
        team.setId(1L);
        team.setDivisionId(118L);
        team.setTeamStatus(TeamStatusEnum.ACTIVE.getCode());
        assertTrue(isRejectedForRegistration(team, 120L));
    }

    @Test
    void rejectsArchivedTeamRegistration() {
        BasketTeam team = new BasketTeam();
        team.setId(1L);
        team.setDivisionId(118L);
        team.setTeamStatus(TeamStatusEnum.ARCHIVED.getCode());
        assertTrue(isRejectedForRegistration(team, 118L));
    }

    @Test
    void grantBindingRejectsLegacy() {
        CaptainGrantSupport support = new CaptainGrantSupport();
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setDivisionId(118L);
        BasketTeam team = new BasketTeam();
        team.setId(1L);
        team.setDivisionId(null);
        assertThrows(ServiceException.class, () -> support.assertTeamBindingForRegistration(grant, team));
    }

    private boolean isRejectedForRegistration(BasketTeam team, Long requestDivisionId) {
        if (team == null || team.getDivisionId() == null) {
            return true;
        }
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            return true;
        }
        return !Objects.equals(team.getDivisionId(), requestDivisionId);
    }
}
