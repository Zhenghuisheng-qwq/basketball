package com.zhs.service.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicVisibilityPolicyScheduleTest {

    @Test
    void versionedPublishedBatch_returnsSeriesWithoutChildPublish() {
        assertTrue(PublicVisibilityPolicy.keepPublishedSeries(true, true, false));
    }

    @Test
    void versionedUnpublishedBatch_hidesSeriesEvenIfChildPublished() {
        assertFalse(PublicVisibilityPolicy.keepPublishedSeries(true, false, true));
    }

    @Test
    void placeholderNullTeamsDoNotAffectVisibilityPredicate() {
        assertTrue(PublicVisibilityPolicy.keepPublishedSeries(true, true, false));
    }
}
