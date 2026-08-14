package com.zhs.service.support;

/**
 * VERSIONED public schedule requires a PUBLISHED schedule batch.
 * LEGACY public schedule keeps historical match.publish_status compatibility
 * and must not be folded into the same batch-only predicate.
 */
public final class PublicVisibilityPolicy {

    private PublicVisibilityPolicy() {
    }

    public static boolean isVersionedWorkflow(String rosterWorkflowMode) {
        return "VERSIONED".equals(rosterWorkflowMode);
    }

    public static boolean keepPublishedMatch(boolean versioned, boolean matchPublished, boolean batchPublished) {
        if (!matchPublished) {
            return false;
        }
        if (!versioned) {
            return true;
        }
        return batchPublished;
    }

    public static boolean keepPublishedSeries(boolean versioned, boolean batchPublished, boolean hasPublishedChildMatch) {
        if (!versioned) {
            return batchPublished || hasPublishedChildMatch;
        }
        return batchPublished;
    }
}
