package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.enums.v4.StructureStatusEnum;
import com.zhs.basket.enums.v4.RosterWorkflowModeEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.basket.service.v4.IBasketV4RuleVersionService;
import com.zhs.basket.support.v4.version.RuleVersionView;
import com.zhs.basket.support.v4.version.StructureVersionContext;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketV4StructureVersionServiceImplTest {

    @Mock private BasketScheduleBatchMapper scheduleBatchMapper;
    @Mock private IBasketV4RuleVersionService ruleVersionService;
    @Mock private IBasketV4RosterVersionService rosterVersionService;
    @Mock private BasketDivisionMapper divisionMapper;

    @InjectMocks
    private BasketV4StructureVersionServiceImpl service;

    @Test
    void resolveForGenerate_withRoster_bindsBothVersions() {
        RuleVersionView rule = new RuleVersionView();
        rule.setRuleVersionId(11L);
        rule.setRuleVersionNo(1);
        BasketDivisionRosterVersion roster = new BasketDivisionRosterVersion();
        roster.setId(201L);
        roster.setVersionNo(1);
        when(ruleVersionService.getCurrent(100L)).thenReturn(rule);
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(roster);

        StructureVersionContext context = service.resolveForGenerate(100L);
        assertEquals(100L, context.getDivisionId());
        assertEquals(11L, context.getRuleVersionId());
        assertEquals(1, context.getRuleVersionNo());
        assertEquals(201L, context.getRosterVersionId());
        assertEquals(1, context.getRosterVersionNo());
    }

    @Test
    void resolveForGenerate_legacyWithoutRoster_keepsNullRoster() {
        RuleVersionView rule = new RuleVersionView();
        rule.setRuleVersionId(11L);
        rule.setRuleVersionNo(1);
        rule.setRosterWorkflowMode(RosterWorkflowModeEnum.LEGACY.getCode());
        when(ruleVersionService.getCurrent(100L)).thenReturn(rule);
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(null);

        StructureVersionContext context = service.resolveForGenerate(100L);
        assertEquals(11L, context.getRuleVersionId());
        assertNull(context.getRosterVersionId());
    }

    @Test
    void resolveForGenerate_versionedWithoutRoster_rejected() {
        RuleVersionView rule = new RuleVersionView();
        rule.setRuleVersionId(11L);
        rule.setRosterWorkflowMode(RosterWorkflowModeEnum.VERSIONED.getCode());
        when(ruleVersionService.getCurrent(100L)).thenReturn(rule);
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.resolveForGenerate(100L));
        assertTrue(ex.getMessage().contains("锁定"));
    }

    @Test
    void bindOnCreate_savesVersionIdsAndActiveStatus() {
        BasketScheduleBatch batch = new BasketScheduleBatch();
        StructureVersionContext context = new StructureVersionContext();
        context.setRuleVersionId(11L);
        context.setRosterVersionId(201L);

        service.bindOnCreate(batch, context);

        assertEquals(11L, batch.getRuleVersionId());
        assertEquals(201L, batch.getRosterVersionId());
        assertEquals(StructureStatusEnum.ACTIVE.getCode(), batch.getStructureStatus());
    }

    @Test
    void bindOnCreate_legacyNullContext_stillActive() {
        BasketScheduleBatch batch = new BasketScheduleBatch();
        service.bindOnCreate(batch, null);
        assertNull(batch.getRuleVersionId());
        assertNull(batch.getRosterVersionId());
        assertEquals(StructureStatusEnum.ACTIVE.getCode(), batch.getStructureStatus());
    }

    @Test
    void markStale_thenNotUsable() {
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setId(501L);
        batch.setStructureStatus(StructureStatusEnum.ACTIVE.getCode());
        when(scheduleBatchMapper.selectById(501L)).thenReturn(batch);
        when(scheduleBatchMapper.markStructureStaleById(501L)).thenReturn(1);

        service.markStale(501L);
        verify(scheduleBatchMapper).markStructureStaleById(501L);

        batch.setStructureStatus(StructureStatusEnum.STALE.getCode());
        assertFalse(service.isStructureUsable(batch));
        assertFalse(service.isStructureUsable(501L));
    }

    @Test
    void isStructureUsable_legacyNullStatus_true() {
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setId(502L);
        assertTrue(service.isStructureUsable(batch));
    }

    @Test
    void assertBoundRuleVersion_mismatch_rejected() {
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setRuleVersionId(11L);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.assertBoundRuleVersion(batch, 99L));
        assertTrue(ex.getMessage().contains("规则版本"));
    }

    @Test
    void assertBoundRuleVersion_legacyNull_skipped() {
        BasketScheduleBatch batch = new BasketScheduleBatch();
        service.assertBoundRuleVersion(batch, 11L);
    }
}
