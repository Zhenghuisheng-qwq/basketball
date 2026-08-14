package com.zhs.service.support;

import com.zhs.model.v4.BasketEventTeam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicTeamDisplaySupportR8Test {

    @Test
    void temporaryTeamUsesDrawTeamNotLongTermTeam() {
        BasketEventTeam temporary = new BasketEventTeam();
        temporary.setSourceDrawTeamId(88L);
        temporary.setSourceTeamId(null);
        temporary.setTeamNameSnapshot("Open 临时队 A");
        assertTrue(PublicTeamDisplaySupport.isTemporary(temporary));
        assertTrue(PublicTeamDisplaySupport.publicTeamName(temporary).contains("临时队"));

        BasketEventTeam longTerm = new BasketEventTeam();
        longTerm.setSourceTeamId(12L);
        longTerm.setTeamNameSnapshot("CNPA Club");
        assertFalse(PublicTeamDisplaySupport.isTemporary(longTerm));
    }
}
