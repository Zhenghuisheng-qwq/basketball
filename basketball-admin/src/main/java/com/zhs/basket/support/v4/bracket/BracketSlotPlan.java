package com.zhs.basket.support.v4.bracket;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BracketSlotPlan {

    private EliminationCapacity capacity;
    private List<BracketSlot> slots = new ArrayList<>();
}
