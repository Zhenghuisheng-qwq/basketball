package com.zhs.basket.support.restriction;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.mapper.BasketDisciplineRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectiveDisciplineRestrictionResolverTest {

    @Mock private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Mock private DisciplineClock disciplineClock;
    @InjectMocks private EffectiveDisciplineRestrictionResolver resolver;

    @Test
    void futureActivePenaltyBecomesEffectiveWhenClockMovesWithoutReactivate() {
        Date start = DisciplineRestrictionFixtures.at(2026, 8, 18, 0, 0);
        BasketDisciplineRecord row = DisciplineRestrictionFixtures.record(1L, "PLAYER", "DATE_RANGE_SUSPENSION", 100L,
                130L, null, start, null);
        when(disciplineRecordMapper.selectActiveBySubject("PLAYER", 100L, 130L))
                .thenReturn(Collections.singletonList(row));

        Date today = DisciplineRestrictionFixtures.at(2026, 8, 17, 12, 0);
        when(disciplineClock.now()).thenReturn(today);
        assertTrue(resolver.resolve("PLAYER", 100L, 130L).isUnrestricted());

        Date tomorrow = DisciplineRestrictionFixtures.at(2026, 8, 18, 0, 1);
        when(disciplineClock.now()).thenReturn(tomorrow);
        EffectiveDisciplineRestriction later = resolver.resolve("PLAYER", 100L, 130L);
        assertFalse(later.isUnrestricted());
        assertFalse(later.isAllowLineup());
        assertFalse(later.isAllowParticipation());
    }

    @Test
    void expiredActivePenaltyIsUnrestrictedWithoutStatusChange() {
        Date start = DisciplineRestrictionFixtures.at(2026, 8, 10, 0, 0);
        Date end = DisciplineRestrictionFixtures.at(2026, 8, 16, 23, 59);
        BasketDisciplineRecord row = DisciplineRestrictionFixtures.record(1L, "PLAYER", "DATE_RANGE_SUSPENSION", 100L,
                130L, null, start, end);
        when(disciplineRecordMapper.selectActiveBySubject("PLAYER", 100L, 130L))
                .thenReturn(Collections.singletonList(row));
        when(disciplineClock.now()).thenReturn(DisciplineRestrictionFixtures.at(2026, 8, 17, 12, 0));

        EffectiveDisciplineRestriction r = resolver.resolve("PLAYER", 100L, 130L);
        assertTrue(r.isUnrestricted());
        assertEqualsActive(row);
    }

    private static void assertEqualsActive(BasketDisciplineRecord row) {
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", row.getRecordStatus());
    }
}
