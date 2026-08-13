package com.zhs.basket.support.v4.bracket;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Value
@Builder
public class SlotAllocationRequest {

    String pairingRule;
    String seedRule;
    String drawRule;
    String byeRule;
    Long randomSeed;
    @Builder.Default
    Set<Long> byeTeamIds = Collections.emptySet();
    @Builder.Default
    List<BracketSlot> confirmedSlots = Collections.emptyList();
}
