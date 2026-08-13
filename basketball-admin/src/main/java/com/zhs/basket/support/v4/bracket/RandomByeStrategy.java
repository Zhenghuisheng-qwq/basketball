package com.zhs.basket.support.v4.bracket;

import com.zhs.common.exception.ServiceException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Random BYE with a persisted seed. Same seed + same participants => same result.
 */
public class RandomByeStrategy implements ByeAllocationStrategy {

    @Override
    public Set<Long> allocateByeTeamIds(List<BracketParticipant> orderedParticipants,
                                        EliminationCapacity capacity,
                                        ByeAllocationRequest request) {
        if (request == null || request.getRandomSeed() == null) {
            throw new ServiceException("Random BYE 必须提供可持久化 randomSeed");
        }
        int byeCount = capacity.getByeCount();
        if (byeCount <= 0) {
            return Set.of();
        }
        List<BracketParticipant> shuffled = new ArrayList<>(orderedParticipants);
        Collections.shuffle(shuffled, new Random(request.getRandomSeed()));
        Set<Long> byeTeamIds = new LinkedHashSet<>();
        for (int i = 0; i < byeCount; i++) {
            byeTeamIds.add(shuffled.get(i).getEventTeamId());
        }
        return byeTeamIds;
    }
}
