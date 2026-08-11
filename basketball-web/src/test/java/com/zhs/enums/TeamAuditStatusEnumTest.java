package com.zhs.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamAuditStatusEnumTest {

    @Test
    void draftCodeIsFour() {
        assertEquals(4, TeamAuditStatusEnum.DRAFT.getCode());
        assertEquals(TeamAuditStatusEnum.DRAFT, TeamAuditStatusEnum.fromCode(4));
    }

    @Test
    void pendingAuditIncludesPendingAndReviewing() {
        assertTrue(TeamAuditStatusEnum.PENDING.isPendingAudit());
        assertTrue(TeamAuditStatusEnum.REVIEWING.isPendingAudit());
        assertFalse(TeamAuditStatusEnum.DRAFT.isPendingAudit());
        assertFalse(TeamAuditStatusEnum.REJECTED.isPendingAudit());
        assertFalse(TeamAuditStatusEnum.APPROVED.isPendingAudit());
    }

    @Test
    void directEditAllowedOnlyForDraftAndRejected() {
        assertTrue(TeamAuditStatusEnum.DRAFT.allowsDirectProfileEdit());
        assertTrue(TeamAuditStatusEnum.REJECTED.allowsDirectProfileEdit());
        assertFalse(TeamAuditStatusEnum.PENDING.allowsDirectProfileEdit());
        assertFalse(TeamAuditStatusEnum.APPROVED.allowsDirectProfileEdit());
    }

    @Test
    void submitCertificationAllowedOnlyForDraftAndRejected() {
        assertTrue(TeamAuditStatusEnum.DRAFT.canSubmitCertification());
        assertTrue(TeamAuditStatusEnum.REJECTED.canSubmitCertification());
        assertFalse(TeamAuditStatusEnum.PENDING.canSubmitCertification());
        assertFalse(TeamAuditStatusEnum.APPROVED.canSubmitCertification());
    }
}
