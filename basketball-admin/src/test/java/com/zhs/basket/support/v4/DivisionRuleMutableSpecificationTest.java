package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionFormatSnapshotMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketPersonalRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DivisionRuleMutableSpecificationTest {

    @Mock private BasketRegistrationMapper registrationMapper;
    @Mock private BasketEventTeamMapper eventTeamMapper;
    @Mock private BasketScheduleBatchMapper scheduleBatchMapper;
    @Mock private BasketV4MatchMapper matchMapper;
    @Mock private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Mock private BasketDivisionFormatSnapshotMapper snapshotMapper;

    @InjectMocks
    private DivisionRuleMutableSpecification specification;

    private BasketDivision division;
    private BasketDivisionFormatSnapshot snapshot;

    @BeforeEach
    void setUp() {
        division = new BasketDivision();
        division.setId(100L);
        division.setDivisionStatus(DivisionStatusEnum.DRAFT.getCode());

        snapshot = new BasketDivisionFormatSnapshot();
        snapshot.setId(11L);
        snapshot.setDivisionId(100L);
        snapshot.setSnapshotStatus(SnapshotStatusEnum.DRAFT.getCode());
        snapshot.setIsCurrent(1);
    }

    @Test
    void assertCanEditRules_lockedSnapshot_rejected() {
        snapshot.setSnapshotStatus(SnapshotStatusEnum.LOCKED.getCode());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> specification.assertCanEditRules(division, snapshot));
        assertTrue(ex.getMessage().contains("草稿"));
    }

    @Test
    void assertCanChangeKeyFields_lockedSnapshot_rejected() {
        snapshot.setSnapshotStatus(SnapshotStatusEnum.LOCKED.getCode());
        when(snapshotMapper.selectActiveByDivisionId(100L)).thenReturn(snapshot);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> specification.assertCanChangeKeyFields(division));
        assertTrue(ex.getMessage().contains("规则已锁定"));
    }

    @Test
    void assertCanEditRules_draft_allowedWhenNoBusinessData() {
        when(registrationMapper.countActiveByDivisionId(100L)).thenReturn(0);
        when(personalRegistrationMapper.countActiveByDivisionId(100L)).thenReturn(0);
        when(eventTeamMapper.countActiveByDivisionId(100L)).thenReturn(0);
        when(scheduleBatchMapper.countByDivisionId(100L)).thenReturn(0);
        when(matchMapper.countByDivisionId(100L)).thenReturn(0);

        assertDoesNotThrow(() -> specification.assertCanEditRules(division, snapshot));
    }

    @Test
    void assertCanRebuild_firstApplyWithoutSnapshot_allowedOnDraft() {
        when(registrationMapper.countActiveByDivisionId(100L)).thenReturn(0);
        when(personalRegistrationMapper.countActiveByDivisionId(100L)).thenReturn(0);
        when(eventTeamMapper.countActiveByDivisionId(100L)).thenReturn(0);
        when(scheduleBatchMapper.countByDivisionId(100L)).thenReturn(0);
        when(matchMapper.countByDivisionId(100L)).thenReturn(0);

        assertDoesNotThrow(() -> specification.assertCanRebuild(division, null));
    }

    @Test
    void assertCanRebuild_withoutSnapshot_rejectedWhenNotDraft() {
        division.setDivisionStatus(DivisionStatusEnum.REGISTRATION_OPEN.getCode());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> specification.assertCanRebuild(division, null));
        assertTrue(ex.getMessage().contains("草稿"));
    }

    @Test
    void assertCanEditRules_withoutSnapshot_stillRejected() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> specification.assertCanEditRules(division, null));
        assertTrue(ex.getMessage().contains("缺少规则快照"));
    }

    @Test
    void resolveFormatTemplateIdForUpdate_keepsCurrentSnapshotTemplate() {
        snapshot.setSourceTemplateId(6L);
        assertEquals(6L, specification.resolveFormatTemplateIdForUpdate(3L, snapshot));
    }

    @Test
    void resolveFormatTemplateIdForUpdate_withoutSnapshot_usesRequested() {
        assertEquals(3L, specification.resolveFormatTemplateIdForUpdate(3L, null));
    }

    @Test
    void canApplyStandardTemplate_locked_rejected() {
        snapshot.setSnapshotStatus(SnapshotStatusEnum.LOCKED.getCode());
        assertTrue(!specification.canApplyStandardTemplate(division, snapshot));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> specification.assertCanApplyStandardTemplate(division, snapshot));
        assertTrue(ex.getMessage().contains("已锁定"));
    }
}
