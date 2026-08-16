package com.zhs.basket.support.restriction;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.domain.BasketDisciplineRestriction;
import com.zhs.basket.mapper.BasketDisciplineRecordMapper;
import com.zhs.basket.mapper.BasketDisciplineRestrictionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisciplineRestrictionAggregatorTest {

    private static final Date NOW = DisciplineRestrictionFixtures.at(2026, 8, 17, 12, 0);

    @Mock private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Mock private BasketDisciplineRestrictionMapper restrictionMapper;
    @Mock private DisciplineClock disciplineClock;
    @InjectMocks private DisciplineRestrictionAggregator aggregator;

    @Test
    void singleMatchSuspensionAggregatesRemainingAndLineup() {
        BasketDisciplineRecord a = DisciplineRestrictionFixtures.record(1L, "PLAYER", "MATCH_SUSPENSION", 100L, 130L,
                2, null, null);
        EffectiveDisciplineRestriction r = DisciplineRestrictionAggregator.aggregate(Collections.singletonList(a), NOW);
        assertEquals(2, r.getRemainingMatchSuspensions());
        assertFalse(r.isAllowLineup());
        assertTrue(r.isAllowCaptain());
        assertTrue(r.isAllowParticipation());
        assertFalse(r.isUnrestricted());
    }

    @Test
    void matchSuspensionThenCaptainBanKeepsBothContributions() {
        BasketDisciplineRecord a = DisciplineRestrictionFixtures.record(1L, "PLAYER", "MATCH_SUSPENSION", 100L, 130L,
                2, null, null);
        BasketDisciplineRecord b = DisciplineRestrictionFixtures.record(2L, "PLAYER", "CAPTAIN_BAN", 100L, 130L,
                null, null, null);
        EffectiveDisciplineRestriction r = DisciplineRestrictionAggregator.aggregate(Arrays.asList(a, b), NOW);
        assertEquals(2, r.getRemainingMatchSuspensions());
        assertFalse(r.isAllowLineup());
        assertFalse(r.isAllowCaptain());
        assertTrue(r.isAllowParticipation());
    }

    @Test
    void captainBanThenMatchSuspensionIsOrderIndependent() {
        BasketDisciplineRecord a = DisciplineRestrictionFixtures.record(1L, "PLAYER", "MATCH_SUSPENSION", 100L, 130L,
                2, null, null);
        BasketDisciplineRecord b = DisciplineRestrictionFixtures.record(2L, "PLAYER", "CAPTAIN_BAN", 100L, 130L,
                null, null, null);
        EffectiveDisciplineRestriction ab = DisciplineRestrictionAggregator.aggregate(Arrays.asList(a, b), NOW);
        EffectiveDisciplineRestriction ba = DisciplineRestrictionAggregator.aggregate(Arrays.asList(b, a), NOW);
        assertEquals(ab.isAllowLineup(), ba.isAllowLineup());
        assertEquals(ab.isAllowCaptain(), ba.isAllowCaptain());
        assertEquals(ab.getRemainingMatchSuspensions(), ba.getRemainingMatchSuspensions());
        assertEquals(ab.isAllowParticipation(), ba.isAllowParticipation());
    }

    @Test
    void revokeCaptainBanLeavesMatchSuspension() {
        BasketDisciplineRecord a = DisciplineRestrictionFixtures.record(1L, "PLAYER", "MATCH_SUSPENSION", 100L, 130L,
                2, null, null);
        EffectiveDisciplineRestriction r = DisciplineRestrictionAggregator.aggregate(Collections.singletonList(a), NOW);
        assertEquals(2, r.getRemainingMatchSuspensions());
        assertFalse(r.isAllowLineup());
        assertTrue(r.isAllowCaptain());
        assertFalse(r.isUnrestricted());
    }

    @Test
    void revokeLastPenaltyReturnsUnrestricted() {
        EffectiveDisciplineRestriction r = DisciplineRestrictionAggregator.aggregate(Collections.<BasketDisciplineRecord>emptyList(), NOW);
        assertTrue(r.isUnrestricted());
        assertTrue(r.isAllowRegistration());
        assertTrue(r.isAllowTransfer());
        assertTrue(r.isAllowCaptain());
        assertTrue(r.isAllowLineup());
        assertTrue(r.isAllowParticipation());
        assertEquals(0, r.getRemainingMatchSuspensions());
    }

    @Test
    void multipleMatchSuspensionsSumRemaining() {
        BasketDisciplineRecord a = DisciplineRestrictionFixtures.record(1L, "PLAYER", "MATCH_SUSPENSION", 100L, 130L,
                2, null, null);
        BasketDisciplineRecord b = DisciplineRestrictionFixtures.record(2L, "PLAYER", "MATCH_SUSPENSION", 100L, 130L,
                1, null, null);
        EffectiveDisciplineRestriction both = DisciplineRestrictionAggregator.aggregate(Arrays.asList(a, b), NOW);
        assertEquals(3, both.getRemainingMatchSuspensions());
        EffectiveDisciplineRestriction afterB = DisciplineRestrictionAggregator.aggregate(Collections.singletonList(a), NOW);
        assertEquals(2, afterB.getRemainingMatchSuspensions());
        EffectiveDisciplineRestriction none = DisciplineRestrictionAggregator.aggregate(Collections.<BasketDisciplineRecord>emptyList(), NOW);
        assertEquals(0, none.getRemainingMatchSuspensions());
        assertTrue(none.isUnrestricted());
    }

    @Test
    void warningOnlyIsUnrestricted() {
        BasketDisciplineRecord warning = DisciplineRestrictionFixtures.record(1L, "PLAYER", "WARNING", 100L, 130L,
                null, null, null);
        EffectiveDisciplineRestriction r = DisciplineRestrictionAggregator.aggregate(Collections.singletonList(warning), NOW);
        assertTrue(r.isUnrestricted());
    }

    @Test
    void pointDeductionOnlyIsUnrestricted() {
        BasketDisciplineRecord row = DisciplineRestrictionFixtures.record(1L, "EVENT_TEAM", "POINT_DEDUCTION", 231L,
                130L, null, null, null);
        EffectiveDisciplineRestriction r = DisciplineRestrictionAggregator.aggregate(Collections.singletonList(row), NOW);
        assertTrue(r.isUnrestricted());
        assertTrue(r.isAllowLineup());
        assertTrue(r.isAllowParticipation());
    }

    @Test
    void revokeDisqualificationLeavesOtherEventTeamPenalty() {
        BasketDisciplineRecord warning = DisciplineRestrictionFixtures.record(2L, "EVENT_TEAM", "WARNING", 231L, 130L,
                null, null, null);
        EffectiveDisciplineRestriction afterDqRevoke = DisciplineRestrictionAggregator.aggregate(
                Collections.singletonList(warning), NOW);
        assertTrue(afterDqRevoke.isUnrestricted());
    }

    @Test
    void futureStartDoesNotContributeUntilClockMoves() {
        Date start = DisciplineRestrictionFixtures.at(2026, 8, 18, 0, 0);
        BasketDisciplineRecord row = DisciplineRestrictionFixtures.record(1L, "PLAYER", "DATE_RANGE_SUSPENSION", 100L,
                130L, null, start, null);
        EffectiveDisciplineRestriction today = DisciplineRestrictionAggregator.aggregate(Collections.singletonList(row), NOW);
        assertTrue(today.isUnrestricted());
        Date later = DisciplineRestrictionFixtures.at(2026, 8, 18, 0, 1);
        EffectiveDisciplineRestriction tomorrow = DisciplineRestrictionAggregator.aggregate(Collections.singletonList(row), later);
        assertFalse(tomorrow.isUnrestricted());
        assertFalse(tomorrow.isAllowLineup());
        assertFalse(tomorrow.isAllowParticipation());
    }

    @Test
    void expiredEndDoesNotContributeWhileStatusStaysActive() {
        Date start = DisciplineRestrictionFixtures.at(2026, 8, 10, 0, 0);
        Date end = DisciplineRestrictionFixtures.at(2026, 8, 16, 23, 59);
        BasketDisciplineRecord row = DisciplineRestrictionFixtures.record(1L, "PLAYER", "DATE_RANGE_SUSPENSION", 100L,
                130L, null, start, end);
        EffectiveDisciplineRestriction r = DisciplineRestrictionAggregator.aggregate(Collections.singletonList(row), NOW);
        assertTrue(r.isUnrestricted());
        assertEquals("ACTIVE", row.getRecordStatus());
    }

    @Test
    void recomputeWritesAggregatedProjection() {
        when(disciplineClock.now()).thenReturn(NOW);
        BasketDisciplineRecord a = DisciplineRestrictionFixtures.record(1L, "PLAYER", "MATCH_SUSPENSION", 100L, 130L,
                2, null, null);
        BasketDisciplineRecord b = DisciplineRestrictionFixtures.record(2L, "PLAYER", "CAPTAIN_BAN", 100L, 130L,
                null, null, null);
        BasketDisciplineRestriction existing = new BasketDisciplineRestriction();
        existing.setId(77L);
        when(restrictionMapper.selectBySubjectForUpdate("PLAYER", 100L, 130L)).thenReturn(existing);
        when(disciplineRecordMapper.selectActiveBySubjectForUpdate("PLAYER", 100L, 130L))
                .thenReturn(Arrays.asList(a, b));

        EffectiveDisciplineRestriction r = aggregator.recompute(
                new DisciplineRestrictionSubject("PLAYER", 100L), 130L);

        assertEquals(2, r.getRemainingMatchSuspensions());
        assertFalse(r.isAllowCaptain());
        assertFalse(r.isAllowLineup());
        verify(restrictionMapper).updateProjection(eq(77L), eq(1), eq(1), eq(0), eq(0), eq(1), eq(2), any(), eq(1));
    }

    @Test
    void recomputeAfterLastRevokeWritesUnrestrictedProjection() {
        when(disciplineClock.now()).thenReturn(NOW);
        BasketDisciplineRestriction existing = new BasketDisciplineRestriction();
        existing.setId(77L);
        when(restrictionMapper.selectBySubjectForUpdate("PLAYER", 100L, 130L)).thenReturn(existing);
        when(disciplineRecordMapper.selectActiveBySubjectForUpdate("PLAYER", 100L, 130L))
                .thenReturn(Collections.<BasketDisciplineRecord>emptyList());

        EffectiveDisciplineRestriction r = aggregator.recompute(
                new DisciplineRestrictionSubject("PLAYER", 100L), 130L);

        assertTrue(r.isUnrestricted());
        verify(restrictionMapper).updateProjection(eq(77L), eq(1), eq(1), eq(1), eq(1), eq(1), eq(0), isNull(), eq(0));
    }

    @Test
    void activateOrderDoesNotChangePersistedProjection() {
        when(disciplineClock.now()).thenReturn(NOW);
        BasketDisciplineRecord a = DisciplineRestrictionFixtures.record(1L, "PLAYER", "MATCH_SUSPENSION", 100L, 130L,
                2, null, null);
        BasketDisciplineRecord b = DisciplineRestrictionFixtures.record(2L, "PLAYER", "CAPTAIN_BAN", 100L, 130L,
                null, null, null);
        BasketDisciplineRestriction existing = new BasketDisciplineRestriction();
        existing.setId(77L);
        when(restrictionMapper.selectBySubjectForUpdate("PLAYER", 100L, 130L)).thenReturn(existing);
        when(disciplineRecordMapper.selectActiveBySubjectForUpdate("PLAYER", 100L, 130L))
                .thenReturn(Arrays.asList(b, a));

        aggregator.recompute(new DisciplineRestrictionSubject("PLAYER", 100L), 130L);
        ArgumentCaptor<Integer> remaining = ArgumentCaptor.forClass(Integer.class);
        verify(restrictionMapper).updateProjection(eq(77L), eq(1), eq(1), eq(0), eq(0), eq(1), remaining.capture(),
                any(), eq(1));
        assertEquals(2, remaining.getValue());
    }
}
