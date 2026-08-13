package com.zhs.basket.support.v4.bracket;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.Set;

@Value
@Builder
public class ByeAllocationRequest {

    Long randomSeed;
    @Builder.Default
    Set<Integer> manualByeSlotNos = Collections.emptySet();
    @Builder.Default
    Set<Long> manualByeTeamIds = Collections.emptySet();
}
