package com.zhs.basket.support.v4.bracket;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Legacy CUSTOM RANDOM_DRAW: shuffle then standard seeded placement.
 * Arena RANDOM_CONFIRM does not use this; it consumes a confirmed slot plan.
 */
@Component
public class RandomDrawSlotStrategy implements BracketSlotAllocationStrategy {

    @Override
    public BracketSlotPlan allocate(List<BracketParticipant> participants, SlotAllocationRequest request) {
        List<BracketParticipant> shuffled = new ArrayList<>(participants);
        long seed = request != null && request.getRandomSeed() != null
                ? request.getRandomSeed()
                : ThreadLocalRandom.current().nextLong();
        Collections.shuffle(shuffled, new Random(seed));
        for (int i = 0; i < shuffled.size(); i++) {
            shuffled.get(i).setSeedNo(i + 1);
        }
        return SeededBracketPlacement.placeBySeed(shuffled);
    }
}
