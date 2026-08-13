package com.zhs.basket.support.v4.bracket;

import java.util.List;
import java.util.Set;

/**
 * Decides which participants receive a first-round BYE. Does not create Matches.
 */
public interface ByeAllocationStrategy {

    Set<Long> allocateByeTeamIds(List<BracketParticipant> orderedParticipants,
                                 EliminationCapacity capacity,
                                 ByeAllocationRequest request);
}
