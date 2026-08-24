package com.zhs.basket.support.standing;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.enums.DisciplineRecordStatusEnum;
import com.zhs.basket.mapper.BasketDisciplineRecordMapper;
import com.zhs.basket.support.restriction.DisciplineClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisciplineStandingAdjustmentResolverTest {

    @Mock private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Mock private DisciplineClock disciplineClock;
    @InjectMocks private DisciplineStandingAdjustmentResolver resolver;

    private Date now;

    @BeforeEach
    void setUp() {
        now = at(2026, 8, 17, 12, 0);
        org.mockito.Mockito.lenient().when(disciplineClock.now()).thenReturn(now);
    }

    @Test
    void multipleActiveDeductionsAccumulate() {
        when(disciplineRecordMapper.selectActivePointDeductionsByEventTeamIds(List.of(100L)))
                .thenReturn(List.of(deduction(1L, 100L, 2), deduction(2L, 100L, 3)));

        Map<Long, Integer> result = resolver.resolveByEventTeamIds(List.of(100L));

        assertEquals(-5, result.get(100L));
    }

    @Test
    void revokedOrInactiveRowsAreIgnoredByFilter() {
        BasketDisciplineRecord revoked = deduction(1L, 100L, 2);
        revoked.setRecordStatus(DisciplineRecordStatusEnum.REVOKED.name());
        when(disciplineRecordMapper.selectActivePointDeductionsByEventTeamIds(List.of(100L)))
                .thenReturn(List.of(revoked, deduction(2L, 100L, 3)));

        Map<Long, Integer> result = resolver.resolveByEventTeamIds(List.of(100L));

        assertEquals(-3, result.get(100L));
    }

    @Test
    void futureEffectiveWindowIsIgnored() {
        BasketDisciplineRecord future = deduction(1L, 100L, 4);
        future.setEffectiveStartTime(at(2026, 8, 18, 0, 0));
        when(disciplineRecordMapper.selectActivePointDeductionsByEventTeamIds(List.of(100L)))
                .thenReturn(List.of(future));

        assertTrue(resolver.resolveByEventTeamIds(List.of(100L)).isEmpty());
    }

    @Test
    void storedNegativeValueIsNormalizedToPositiveDeduction() {
        when(disciplineRecordMapper.selectActivePointDeductionsByEventTeamIds(List.of(100L)))
                .thenReturn(List.of(deduction(1L, 100L, -2)));

        assertEquals(-2, resolver.resolveByEventTeamIds(List.of(100L)).get(100L));
        assertEquals(2, DisciplineStandingAdjustmentResolver.deductionAmount(-2));
        assertEquals(2, DisciplineStandingAdjustmentResolver.deductionAmount(2));
        assertEquals(0, DisciplineStandingAdjustmentResolver.deductionAmount(0));
    }

    @Test
    void emptyIdsReturnEmptyMap() {
        assertTrue(resolver.resolveByEventTeamIds(List.of()).isEmpty());
        assertTrue(resolver.resolveByEventTeamIds(null).isEmpty());
    }

    private static BasketDisciplineRecord deduction(Long id, Long eventTeamId, int amount) {
        BasketDisciplineRecord row = new BasketDisciplineRecord();
        row.setId(id);
        row.setTargetType("EVENT_TEAM");
        row.setPenaltyCode("POINT_DEDUCTION");
        row.setEventTeamId(eventTeamId);
        row.setPointDeduction(amount);
        row.setRecordStatus(DisciplineRecordStatusEnum.ACTIVE.name());
        return row;
    }

    private static Date at(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
