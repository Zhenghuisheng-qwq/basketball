package com.zhs.basket.support.v4.bracket;

import com.zhs.common.exception.ServiceException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Highest seed / ranking receives BYE first.
 */
public class SeedPriorityByeStrategy implements ByeAllocationStrategy {

    @Override
    public Set<Long> allocateByeTeamIds(List<BracketParticipant> orderedParticipants,
                                        EliminationCapacity capacity,
                                        ByeAllocationRequest request) {
        if (orderedParticipants.size() != capacity.getParticipantCount()) {
            throw new ServiceException("BYE 分配参赛人数与容量计算不一致");
        }
        List<BracketParticipant> ordered = new ArrayList<>(orderedParticipants);
        ordered.sort(Comparator
                .comparing(BracketParticipant::getSeedNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BracketParticipant::getRankingNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BracketParticipant::getEventTeamId));
        Set<Long> byeTeamIds = new LinkedHashSet<>();
        int byeCount = capacity.getByeCount();
        for (int i = 0; i < byeCount; i++) {
            byeTeamIds.add(ordered.get(i).getEventTeamId());
        }
        return byeTeamIds;
    }
}
