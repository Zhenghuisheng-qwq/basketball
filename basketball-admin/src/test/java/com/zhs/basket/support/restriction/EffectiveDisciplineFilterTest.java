package com.zhs.basket.support.restriction;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.enums.DisciplineRecordStatusEnum;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectiveDisciplineFilterTest {

    private static final Date NOW = DisciplineRestrictionFixtures.at(2026, 8, 17, 12, 0);

    @Test
    void nullStartAndEndAreAlwaysEffectiveWhenActive() {
        BasketDisciplineRecord record = DisciplineRestrictionFixtures.record(1L, "PLAYER", "CAPTAIN_BAN", 100L, 130L,
                null, null, null);
        assertTrue(EffectiveDisciplineFilter.isCurrentlyEffective(record, NOW));
    }

    @Test
    void futureStartIsNotEffective() {
        Date start = DisciplineRestrictionFixtures.at(2026, 8, 18, 0, 0);
        BasketDisciplineRecord record = DisciplineRestrictionFixtures.record(1L, "PLAYER", "DATE_RANGE_SUSPENSION",
                100L, 130L, null, start, null);
        assertFalse(EffectiveDisciplineFilter.isCurrentlyEffective(record, NOW));
    }

    @Test
    void startBoundaryIsEffective() {
        Date start = DisciplineRestrictionFixtures.at(2026, 8, 17, 12, 0);
        BasketDisciplineRecord record = DisciplineRestrictionFixtures.record(1L, "PLAYER", "DATE_RANGE_SUSPENSION",
                100L, 130L, null, start, null);
        assertTrue(EffectiveDisciplineFilter.isCurrentlyEffective(record, NOW));
    }

    @Test
    void endBoundaryIsEffective() {
        Date start = DisciplineRestrictionFixtures.at(2026, 8, 10, 0, 0);
        Date end = DisciplineRestrictionFixtures.at(2026, 8, 17, 12, 0);
        BasketDisciplineRecord record = DisciplineRestrictionFixtures.record(1L, "PLAYER", "DATE_RANGE_SUSPENSION",
                100L, 130L, null, start, end);
        assertTrue(EffectiveDisciplineFilter.isCurrentlyEffective(record, NOW));
    }

    @Test
    void afterEndIsNotEffectiveWhileStatusStaysActive() {
        Date start = DisciplineRestrictionFixtures.at(2026, 8, 10, 0, 0);
        Date end = DisciplineRestrictionFixtures.at(2026, 8, 16, 23, 59);
        BasketDisciplineRecord record = DisciplineRestrictionFixtures.record(1L, "PLAYER", "DATE_RANGE_SUSPENSION",
                100L, 130L, null, start, end);
        assertTrue(DisciplineRecordStatusEnum.ACTIVE.name().equals(record.getRecordStatus()));
        assertFalse(EffectiveDisciplineFilter.isCurrentlyEffective(record, NOW));
    }

    @Test
    void revokedIsNeverEffective() {
        BasketDisciplineRecord record = DisciplineRestrictionFixtures.record(1L, "PLAYER", "CAPTAIN_BAN", 100L, 130L,
                null, null, null);
        record.setRecordStatus(DisciplineRecordStatusEnum.REVOKED.name());
        assertFalse(EffectiveDisciplineFilter.isCurrentlyEffective(record, NOW));
    }
}
