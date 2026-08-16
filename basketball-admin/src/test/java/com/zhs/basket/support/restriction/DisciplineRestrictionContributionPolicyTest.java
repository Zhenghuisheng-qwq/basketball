package com.zhs.basket.support.restriction;

import com.zhs.basket.domain.BasketDisciplineRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisciplineRestrictionContributionPolicyTest {

    @Test
    void warningContributesNoRestriction() {
        DisciplineRestrictionContribution c = DisciplineRestrictionContributionPolicy.contribute(
                DisciplineRestrictionFixtures.record(1L, "PLAYER", "WARNING", 100L, 130L, null, null, null));
        assertTrue(c.isAllowRegistration());
        assertTrue(c.isAllowTransfer());
        assertTrue(c.isAllowCaptain());
        assertTrue(c.isAllowLineup());
        assertTrue(c.isAllowParticipation());
        assertEquals(0, c.getRemainingMatchSuspensions());
    }

    @Test
    void pointDeductionContributesNoParticipationRestriction() {
        DisciplineRestrictionContribution c = DisciplineRestrictionContributionPolicy.contribute(
                DisciplineRestrictionFixtures.record(2L, "EVENT_TEAM", "POINT_DEDUCTION", 231L, 130L, null, null, null));
        assertTrue(c.isAllowRegistration());
        assertTrue(c.isAllowTransfer());
        assertTrue(c.isAllowCaptain());
        assertTrue(c.isAllowLineup());
        assertTrue(c.isAllowParticipation());
        assertEquals(0, c.getRemainingMatchSuspensions());
    }

    @Test
    void matchSuspensionContributesLineupAndRemaining() {
        DisciplineRestrictionContribution c = DisciplineRestrictionContributionPolicy.contribute(
                DisciplineRestrictionFixtures.record(3L, "PLAYER", "MATCH_SUSPENSION", 100L, 130L, 2, null, null));
        assertTrue(c.isAllowCaptain());
        assertTrue(c.isAllowParticipation());
        assertFalse(c.isAllowLineup());
        assertEquals(2, c.getRemainingMatchSuspensions());
    }

    @Test
    void captainBanContributesCaptainOnly() {
        DisciplineRestrictionContribution c = DisciplineRestrictionContributionPolicy.contribute(
                DisciplineRestrictionFixtures.record(4L, "PLAYER", "CAPTAIN_BAN", 100L, 130L, null, null, null));
        assertFalse(c.isAllowCaptain());
        assertTrue(c.isAllowLineup());
        assertTrue(c.isAllowParticipation());
        assertEquals(0, c.getRemainingMatchSuspensions());
    }

    @ParameterizedTest
    @CsvSource({
            "REGISTRATION_BAN,false,true,true,true,true,0",
            "TRANSFER_BAN,true,false,true,true,true,0",
            "DATE_RANGE_SUSPENSION,true,true,true,false,false,0",
            "EVENT_DISQUALIFICATION,true,true,true,false,false,0",
            "PLAYER_QUALIFICATION_SUSPENSION,true,true,true,false,false,0",
            "TEAM_SUSPENSION,false,true,true,true,false,0",
            "MATCH_ELIGIBILITY_CANCEL,true,true,true,false,true,3"
    })
    void historicalMappingPreserved(String penalty, boolean reg, boolean transfer, boolean captain, boolean lineup,
            boolean participation, int remaining) {
        BasketDisciplineRecord record = DisciplineRestrictionFixtures.record(9L, "PLAYER", penalty, 100L, 130L,
                remaining, null, null);
        DisciplineRestrictionContribution c = DisciplineRestrictionContributionPolicy.contribute(record);
        assertEquals(reg, c.isAllowRegistration());
        assertEquals(transfer, c.isAllowTransfer());
        assertEquals(captain, c.isAllowCaptain());
        assertEquals(lineup, c.isAllowLineup());
        assertEquals(participation, c.isAllowParticipation());
        assertEquals(remaining, c.getRemainingMatchSuspensions());
    }
}
