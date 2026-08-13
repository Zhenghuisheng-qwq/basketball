package com.zhs.basket.support.v4.roster;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RosterAllocationPlan {

    private final List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long registrationId;
        private String participationRole;
        private Integer candidateOrder;
    }

    public void add(Long registrationId, String participationRole, Integer candidateOrder) {
        Item item = new Item();
        item.setRegistrationId(registrationId);
        item.setParticipationRole(participationRole);
        item.setCandidateOrder(candidateOrder);
        items.add(item);
    }
}
