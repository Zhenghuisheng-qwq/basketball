package com.zhs.service.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicVisibilityPolicyR9Test {

    @Test
    void versionedDraftBatchDoesNotLeakEvenIfMatchMarkedPublished() {
        assertFalse(PublicVisibilityPolicy.keepPublishedMatch(true, true, false));
        assertTrue(PublicVisibilityPolicy.keepPublishedMatch(true, true, true));
        assertFalse(PublicVisibilityPolicy.keepPublishedMatch(true, false, true));
    }

    @Test
    void legacyKeepsHistoricalPublishedMatchWithoutStagePublishRecord() {
        assertTrue(PublicVisibilityPolicy.keepPublishedMatch(false, true, false));
        assertTrue(PublicVisibilityPolicy.keepPublishedMatch(false, true, true));
        assertFalse(PublicVisibilityPolicy.keepPublishedMatch(false, false, false));
    }

    @Test
    void legacySeriesVisibleWhenChildMatchPublished() {
        assertTrue(PublicVisibilityPolicy.keepPublishedSeries(false, false, true));
        assertFalse(PublicVisibilityPolicy.keepPublishedSeries(true, false, true));
        assertTrue(PublicVisibilityPolicy.keepPublishedSeries(true, true, false));
    }

    @Test
    void nullWorkflowIsLegacyNotVersioned() {
        assertFalse(PublicVisibilityPolicy.isVersionedWorkflow(null));
        assertFalse(PublicVisibilityPolicy.isVersionedWorkflow("LEGACY"));
        assertTrue(PublicVisibilityPolicy.isVersionedWorkflow("VERSIONED"));
    }
}
