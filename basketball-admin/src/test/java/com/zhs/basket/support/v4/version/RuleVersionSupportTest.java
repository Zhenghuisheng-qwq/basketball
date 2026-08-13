package com.zhs.basket.support.v4.version;

import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleVersionSupportTest {

    @Test
    void fromSnapshot_mapsRuleVersionFields() {
        BasketDivisionFormatSnapshot snapshot = new BasketDivisionFormatSnapshot();
        snapshot.setId(11L);
        snapshot.setDivisionId(100L);
        snapshot.setSnapshotVersion(1);
        snapshot.setSnapshotStatus(SnapshotStatusEnum.LOCKED.getCode());
        snapshot.setIsCurrent(1);

        RuleVersionView view = RuleVersionSupport.fromSnapshot(snapshot);

        assertEquals(11L, view.getRuleVersionId());
        assertEquals(1, view.getRuleVersionNo());
        assertEquals(SnapshotStatusEnum.LOCKED.getCode(), view.getRuleVersionStatus());
        assertEquals(100L, view.getDivisionId());
        assertTrue(view.isLocked());
    }

    @Test
    void fromSnapshot_draft_notLocked() {
        BasketDivisionFormatSnapshot snapshot = new BasketDivisionFormatSnapshot();
        snapshot.setId(12L);
        snapshot.setSnapshotStatus(SnapshotStatusEnum.DRAFT.getCode());

        RuleVersionView view = RuleVersionSupport.fromSnapshot(snapshot);
        assertFalse(view.isLocked());
        assertFalse(RuleVersionSupport.isLocked(snapshot));
    }

    @Test
    void fromSnapshot_null_returnsNull() {
        assertNull(RuleVersionSupport.fromSnapshot(null));
    }
}
