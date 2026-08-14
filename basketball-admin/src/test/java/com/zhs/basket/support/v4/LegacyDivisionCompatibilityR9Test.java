package com.zhs.basket.support.v4;

import com.zhs.basket.vo.v4.DivisionOperationCapabilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDivisionCompatibilityR9Test {

    @Test
    void legacyWithoutRosterVersionIsReadCompatibleAndDoesNotRequireLockRoster() {
        DivisionOperationCapabilities caps = DivisionOperationCapabilitySupport.divisionCapabilities(
                "LEGACY", "LOCKED", "DRAFT", false, false);
        assertTrue(caps.isLegacyCompatible());
        assertFalse(caps.isRequireRulePublished());
        assertTrue(caps.isCanGenerateStructure());
        assertFalse(DivisionOperationCapabilitySupport.isVersioned("LEGACY"));
        assertFalse(DivisionOperationCapabilitySupport.isVersioned(null));
    }

    @Test
    void versionedWithoutLockedRosterStillBlocksStructure() {
        DivisionOperationCapabilities caps = DivisionOperationCapabilitySupport.divisionCapabilities(
                "VERSIONED", "LOCKED", "DRAFT", false, false);
        assertFalse(caps.isLegacyCompatible());
        assertTrue(caps.isRequireRulePublished());
        assertFalse(caps.isCanGenerateStructure());
        assertTrue(caps.getBlockReason() != null && caps.getBlockReason().contains("名单"));
    }
}
