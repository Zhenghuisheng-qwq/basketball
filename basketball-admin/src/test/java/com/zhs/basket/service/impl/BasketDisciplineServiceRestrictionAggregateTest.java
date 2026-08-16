package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.enums.DisciplineRecordStatusEnum;
import com.zhs.basket.enums.DisciplineStatusEnum;
import com.zhs.basket.mapper.BasketDisciplineRecordMapper;
import com.zhs.basket.req.DisciplineRevokeReq;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.support.restriction.DisciplineRestrictionAggregator;
import com.zhs.common.utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketDisciplineServiceRestrictionAggregateTest {

    @Mock private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Mock private DisciplineRestrictionAggregator restrictionAggregator;
    @Mock private BasketBusinessAuditWriter auditWriter;
    @InjectMocks private BasketDisciplineServiceImpl service;

    @Test
    void activateRecomputesSubjectInsteadOfLastWriteWins() {
        BasketDisciplineRecord record = activeDraft(11L, "PLAYER", "MATCH_SUSPENSION", 100L);
        record.setRecordStatus(DisciplineRecordStatusEnum.DRAFT.name());
        when(disciplineRecordMapper.selectById(11L)).thenReturn(record);
        when(disciplineRecordMapper.updateRecordStatusIfMatch(eq(11L), eq("DRAFT"), eq("ACTIVE"),
                eq(DisciplineStatusEnum.ACTIVE.getCode()), eq(9L), isNull())).thenReturn(1);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(9L);
            service.activate(11L);
        }
        verify(restrictionAggregator).recompute(any(BasketDisciplineRecord.class));
    }

    @Test
    void revokeRecomputesSubjectInsteadOfDisablingWholeRestriction() {
        BasketDisciplineRecord record = activeDraft(12L, "PLAYER", "CAPTAIN_BAN", 100L);
        record.setRecordStatus(DisciplineRecordStatusEnum.ACTIVE.name());
        when(disciplineRecordMapper.selectById(12L)).thenReturn(record);
        when(disciplineRecordMapper.updateRecordStatusIfMatch(eq(12L), eq("ACTIVE"), eq("REVOKED"),
                eq(DisciplineStatusEnum.INACTIVE.getCode()), eq(9L), eq("误操作"))).thenReturn(1);

        DisciplineRevokeReq req = new DisciplineRevokeReq();
        req.setRevokeReason("误操作");
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(9L);
            service.revoke(12L, req);
        }
        verify(restrictionAggregator).recompute(any(BasketDisciplineRecord.class));
    }

    private static BasketDisciplineRecord activeDraft(Long id, String targetType, String penaltyCode, Long playerId) {
        BasketDisciplineRecord record = new BasketDisciplineRecord();
        record.setId(id);
        record.setTargetType(targetType);
        record.setPenaltyCode(penaltyCode);
        record.setPlayerId(playerId);
        record.setSeasonId(130L);
        return record;
    }
}
