package com.zhs.support.v4;

import com.zhs.enums.v4.CaptainGrantStatusEnum;
import com.zhs.model.v4.BasketCaptainGrant;
import com.zhs.model.v4.BasketEventTeam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTeamCaptainAuthorizationTest {

    private final EventTeamCaptainAuthorization auth = new EventTeamCaptainAuthorization();

    @Test
    void test21_temporaryCaptainCanSubmitWithoutGrant() {
        BasketEventTeam team = new BasketEventTeam();
        team.setCaptainPlayerId(7L);
        team.setSourceTeamId(null);
        assertTrue(auth.canSubmitAsCaptain(team, 7L, null));
    }

    @Test
    void test22_ordinaryMemberCannotSubmitAsCaptain() {
        BasketEventTeam team = new BasketEventTeam();
        team.setCaptainPlayerId(7L);
        team.setSourceTeamId(null);
        assertFalse(auth.canSubmitAsCaptain(team, 8L, null));
        assertFalse(auth.isEventTeamCaptain(team, 8L));
    }

    @Test
    void teamCaptainStillRequiresGrant() {
        BasketEventTeam team = new BasketEventTeam();
        team.setCaptainPlayerId(7L);
        team.setSourceTeamId(99L);
        assertFalse(auth.canSubmitAsCaptain(team, 7L, null));
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setGrantStatus(CaptainGrantStatusEnum.ACTIVE.getCode());
        grant.setTeamId(99L);
        assertTrue(auth.canSubmitAsCaptain(team, 7L, grant));
    }
}
