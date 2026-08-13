package com.zhs.basket.support.v4.bracket;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HIGH_VS_LOW / PLAYOFF_BRACKET: standard seeded placement. Generator never branches on Series.
 */
@Component
public class SeededHighVsLowSlotStrategy implements BracketSlotAllocationStrategy {

    @Override
    public BracketSlotPlan allocate(List<BracketParticipant> participants, SlotAllocationRequest request) {
        if (request != null && request.getByeTeamIds() != null && !request.getByeTeamIds().isEmpty()) {
            return SeededBracketPlacement.placeWithByeTeams(participants, request.getByeTeamIds());
        }
        return SeededBracketPlacement.placeBySeed(participants);
    }
}
