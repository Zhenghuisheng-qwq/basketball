package com.zhs.support;

import com.zhs.dao.v4.BasketCaptainGrantDao;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketTeam;
import com.zhs.model.v4.BasketCaptainGrant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CaptainGrantBindCasTest {

    @Mock
    private BasketCaptainGrantDao captainGrantDao;

    @InjectMocks
    private CaptainGrantSupport support;

    @Test
    void assertGrantUnbound_rejectsBoundGrant() {
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setTeamId(10L);
        ServiceException ex = assertThrows(ServiceException.class, () -> support.assertGrantUnbound(grant));
        assertEquals(CaptainGrantSupport.GRANT_ALREADY_BOUND_MESSAGE, ex.getMessage());
    }

    @Test
    void bindGrantToTeam_casZeroRollsBackMessage() {
        ServiceException ex = assertThrows(ServiceException.class, () -> support.assertBindUpdatedRows(0));
        assertEquals(CaptainGrantSupport.GRANT_BIND_RACE_MESSAGE, ex.getMessage());
    }

    @Test
    void assertGrantTeamDivisionMatch_rejectsLegacyTeam() {
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setDivisionId(118L);
        BasketTeam team = new BasketTeam();
        team.setDivisionId(null);
        assertThrows(ServiceException.class, () -> support.assertGrantTeamDivisionMatch(grant, team));
    }

    @Test
    void assertGrantTeamDivisionMatch_rejectsMismatch() {
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setDivisionId(118L);
        BasketTeam team = new BasketTeam();
        team.setDivisionId(120L);
        assertThrows(ServiceException.class, () -> support.assertGrantTeamDivisionMatch(grant, team));
    }

    @Test
    void assertCanManageTeam_requiresGrantBoundToTeam() {
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setDivisionId(118L);
        grant.setMemberId(1L);
        grant.setTeamId(null);
        BasketTeam team = new BasketTeam();
        team.setId(10L);
        team.setDivisionId(118L);
        team.setCaptainMemberId(1L);
        assertThrows(ServiceException.class, () -> support.assertCanManageTeam(grant, team, 1L));
    }
}
