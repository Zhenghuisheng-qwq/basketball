package com.zhs.support;

import com.zhs.model.v4.BasketCaptainGrant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaptainGrantSupportTest {

    @Test
    void buildActiveUniqueKey_usesDivisionPlayer() {
        assertEquals("5_10", CaptainGrantSupport.buildActiveUniqueKey(5L, 10L));
    }

    @Test
    void bindGrantToTeam_rejectsOtherTeam() {
        CaptainGrantSupport support = new CaptainGrantSupport();
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setId(1L);
        grant.setTeamId(100L);

        assertThrows(com.zhs.exception.ServiceException.class,
                () -> support.bindGrantToTeam(grant, 200L));
    }
}
