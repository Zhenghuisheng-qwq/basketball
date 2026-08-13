package com.zhs.basket.support.v4.bracket;

import com.zhs.common.exception.ServiceException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Admin-specified BYE teams. Count must equal capacity - N.
 */
public class ManualByeStrategy implements ByeAllocationStrategy {

    @Override
    public Set<Long> allocateByeTeamIds(List<BracketParticipant> orderedParticipants,
                                        EliminationCapacity capacity,
                                        ByeAllocationRequest request) {
        Set<Long> specified = request == null || request.getManualByeTeamIds() == null
                ? Set.of() : request.getManualByeTeamIds();
        if (specified.size() != capacity.getByeCount()) {
            throw new ServiceException("手动 BYE 数量必须等于 capacity - 参赛队数（期望 "
                    + capacity.getByeCount() + "，实际 " + specified.size() + "）");
        }
        Set<Long> formalIds = new HashSet<>();
        for (BracketParticipant participant : orderedParticipants) {
            formalIds.add(participant.getEventTeamId());
        }
        for (Long teamId : specified) {
            if (teamId == null || !formalIds.contains(teamId)) {
                throw new ServiceException("手动 BYE 只能指定当前正式参赛队");
            }
        }
        return Set.copyOf(specified);
    }
}
