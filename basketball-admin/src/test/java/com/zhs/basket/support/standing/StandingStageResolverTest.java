package com.zhs.basket.support.standing;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandingStageResolverTest {

    @Mock private BasketDivisionStageMapper stageMapper;
    @InjectMocks private StandingStageResolver resolver;

    @Test
    void uniqueRoundRobinStageIsResolved() {
        when(stageMapper.selectByDivisionId(10L)).thenReturn(List.of(
                stage(1L, "ROUND_ROBIN"),
                stage(2L, "SINGLE_ELIMINATION")));

        assertEquals(1L, resolver.resolveStandingStage(10L).getId());
    }

    @Test
    void arenaOrOpenWithoutStandingStageReturnsNull() {
        when(stageMapper.selectByDivisionId(11L)).thenReturn(List.of(stage(3L, "SINGLE_ELIMINATION")));

        assertNull(resolver.resolveStandingStage(11L));
    }

    @Test
    void multipleStandingStagesAreAmbiguous() {
        when(stageMapper.selectByDivisionId(12L)).thenReturn(List.of(
                stage(4L, "ROUND_ROBIN"),
                stage(5L, "ROUND_ROBIN")));

        ServiceException ex = assertThrows(ServiceException.class, () -> resolver.resolveStandingStage(12L));
        assertEquals(StandingStageResolver.MSG_AMBIGUOUS_STAGE, ex.getMessage());
    }

    @Test
    void missingDivisionReturnsNull() {
        assertNull(resolver.resolveStandingStage(null));
    }

    private static BasketDivisionStage stage(Long id, String format) {
        BasketDivisionStage row = new BasketDivisionStage();
        row.setId(id);
        row.setCompetitionFormat(format);
        return row;
    }
}
