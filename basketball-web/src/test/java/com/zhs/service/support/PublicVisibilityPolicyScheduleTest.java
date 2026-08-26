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

    @Test
    void bo3SweepHidesRemainingScheduledGame() {
        assertTrue(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(true, true));
        assertTrue(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(true, true));
        assertFalse(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(true, false));
    }

    @Test
    void bo3FullSeriesKeepsEveryOfficialGame() {
        assertTrue(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(true, true));
        assertTrue(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(true, true));
        assertTrue(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(true, true));
    }

    @Test
    void bo5SweepHidesRemainingScheduledGames() {
        assertTrue(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(true, true));
        assertTrue(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(true, true));
        assertTrue(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(true, true));
        assertFalse(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(true, false));
        assertFalse(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(true, false));
    }

    @Test
    void bo5FullSeriesKeepsEveryOfficialGame() {
        for (int i = 0; i < 5; i++) {
            assertTrue(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(true, true));
        }
    }

    @Test
    void openSeriesKeepsNextScheduledGame() {
        assertTrue(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(false, false));
    }

    @Test
    void finishedHistoryStaysPublicAfterSeriesEnds() {
        assertTrue(PublicVisibilityPolicy.keepUnplayedChildInPublicSchedule(true, true));
    }
}
