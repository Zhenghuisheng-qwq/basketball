package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.enums.v4.ScheduleGenerationStrategyEnum;
import com.zhs.basket.enums.v4.StructureStatusEnum;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.support.v4.version.StructureVersionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StructureVersionBindingTest {

    @Mock
    private BasketScheduleBatchMapper scheduleBatchMapper;

    @Test
    void createBatch_withRoster_persistsBothVersionIds() {
        when(scheduleBatchMapper.insert(any())).thenReturn(1);
        StageExecutionContext context = baseContext();
        StructureVersionContext version = new StructureVersionContext();
        version.setRuleVersionId(11L);
        version.setRosterVersionId(201L);
        context.setVersionContext(version);

        StageScheduleSupport.createBatch(scheduleBatchMapper, context,
                ScheduleGenerationStrategyEnum.ROUND_ROBIN, "RR", 4, 6, null);

        ArgumentCaptor<BasketScheduleBatch> captor = ArgumentCaptor.forClass(BasketScheduleBatch.class);
        verify(scheduleBatchMapper).insert(captor.capture());
        BasketScheduleBatch batch = captor.getValue();
        assertEquals(11L, batch.getRuleVersionId());
        assertEquals(201L, batch.getRosterVersionId());
        assertEquals(StructureStatusEnum.ACTIVE.getCode(), batch.getStructureStatus());
        assertEquals(0, batch.getScheduleRevision());
        assertEquals(1, batch.getExecutionVersion());
    }

    @Test
    void createBatch_withoutRoster_legacyStillGenerates() {
        when(scheduleBatchMapper.insert(any())).thenReturn(1);
        StageExecutionContext context = baseContext();
        StructureVersionContext version = new StructureVersionContext();
        version.setRuleVersionId(11L);
        context.setVersionContext(version);

        StageScheduleSupport.createBatch(scheduleBatchMapper, context,
                ScheduleGenerationStrategyEnum.ROUND_ROBIN, "RR", 4, 6, null);

        ArgumentCaptor<BasketScheduleBatch> captor = ArgumentCaptor.forClass(BasketScheduleBatch.class);
        verify(scheduleBatchMapper).insert(captor.capture());
        BasketScheduleBatch batch = captor.getValue();
        assertEquals(11L, batch.getRuleVersionId());
        assertNull(batch.getRosterVersionId());
        assertEquals(StructureStatusEnum.ACTIVE.getCode(), batch.getStructureStatus());
    }

    @Test
    void createBatch_nullVersionContext_doesNotFail() {
        when(scheduleBatchMapper.insert(any())).thenReturn(1);
        StageExecutionContext context = baseContext();

        StageScheduleSupport.createBatch(scheduleBatchMapper, context,
                ScheduleGenerationStrategyEnum.ROUND_ROBIN, "RR", 4, 6, null);

        ArgumentCaptor<BasketScheduleBatch> captor = ArgumentCaptor.forClass(BasketScheduleBatch.class);
        verify(scheduleBatchMapper).insert(captor.capture());
        assertNull(captor.getValue().getRosterVersionId());
        assertEquals(StructureStatusEnum.ACTIVE.getCode(), captor.getValue().getStructureStatus());
    }

    private StageExecutionContext baseContext() {
        BasketDivision division = new BasketDivision();
        division.setId(100L);
        division.setSeasonId(1L);
        division.setFormatType("REGULAR_ROUND_ROBIN");
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setId(10L);
        StageExecutionContext context = new StageExecutionContext();
        context.setDivision(division);
        context.setStage(stage);
        context.setExecutionVersion(1);
        return context;
    }
}
