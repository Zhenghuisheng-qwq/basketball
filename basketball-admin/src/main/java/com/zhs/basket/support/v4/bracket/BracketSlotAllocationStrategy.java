package com.zhs.basket.support.v4.bracket;

import java.util.List;

public interface BracketSlotAllocationStrategy {

    BracketSlotPlan allocate(List<BracketParticipant> participants, SlotAllocationRequest request);
}
