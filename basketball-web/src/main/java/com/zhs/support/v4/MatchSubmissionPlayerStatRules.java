package com.zhs.support.v4;

import com.zhs.exception.ServiceException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Match Submission 球员行校验：同一 EventPlayer 不能出现两次。
 */
public final class MatchSubmissionPlayerStatRules {

    private MatchSubmissionPlayerStatRules() {
    }

    public static void assertNoDuplicateEventPlayers(List<Long> eventPlayerIds) {
        if (eventPlayerIds == null || eventPlayerIds.isEmpty()) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        for (Long eventPlayerId : eventPlayerIds) {
            if (eventPlayerId == null) {
                continue;
            }
            if (!seen.add(eventPlayerId)) {
                throw new ServiceException("同一球员不能重复填写");
            }
        }
    }
}
