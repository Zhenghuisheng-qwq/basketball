package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.mapper.BasketDisciplineExecutionDetailMapper;
import com.zhs.basket.mapper.BasketDisciplineRecordMapper;
import com.zhs.basket.support.DisciplinePenaltyTargetCompatibilityPolicy;
import com.zhs.basket.support.penalty.PenaltyExecutionStrategy;
import com.zhs.basket.support.penalty.PenaltyExecutionStrategyFactory;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PenaltyExecutionServiceCompatibilityTest {

    @Mock private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Mock private BasketDisciplineExecutionDetailMapper executionDetailMapper;
    @Mock private PenaltyExecutionStrategyFactory strategyFactory;
    @Mock private PenaltyExecutionStrategy strategy;

    @InjectMocks
    private PenaltyExecutionServiceImpl service;

    @Test
    void executeRejectsPlayerEventDisqualificationBeforeStrategy() {
        BasketDisciplineRecord record = new BasketDisciplineRecord();
        record.setId(10L);
        record.setTargetType("PLAYER");
        record.setPenaltyCode("EVENT_DISQUALIFICATION");
        record.setEventTeamId(231L);
        record.setPlayerId(1001L);
        when(disciplineRecordMapper.selectById(10L)).thenReturn(record);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.execute(10L));
        assertEquals(DisciplinePenaltyTargetCompatibilityPolicy.MSG_INCOMPATIBLE, ex.getMessage());
        verify(strategyFactory, never()).find(any());
        verify(strategy, never()).execute(any());
        verify(disciplineRecordMapper, never()).updateExecution(
                any(), any(), any(), any(), any(), any(), any(), any());
    }
}
