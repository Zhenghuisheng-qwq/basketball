package com.zhs.basket.support.v4.bracket;

import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupCrossPlacementR6Test {

    private final GroupCrossPlacementStrategy strategy = new GroupCrossPlacementStrategy();

    @Test
    void fourGroupsTop2ExactCross() {
        BracketSlotPlan plan = strategy.place(request(groups("A", "B", "C", "D"), 2));
        assertEquals(8, plan.getCapacity().getParticipantCount());
        assertEquals(8, plan.getCapacity().getCapacity());
        assertEquals(0, plan.getCapacity().getByeCount());
        assertPairs(plan,
                "A1", "D2",
                "A2", "D1",
                "B1", "C2",
                "B2", "C1");
        plan.getSlots().forEach(slot -> {
            assertNull(slot.getEventTeamId());
            assertNull(slot.getResolvedEventTeamId());
            assertEquals("PENDING", slot.getResolutionStatus());
            assertEquals("GROUP_RANK", slot.getSourceType());
        });
    }

    @Test
    void twoGroupsTop2MirrorsAandB() {
        BracketSlotPlan plan = strategy.place(request(groups("A", "B"), 2));
        assertPairs(plan, "A1", "B2", "A2", "B1");
    }

    @Test
    void placeholderDoesNotNeedStanding() {
        BracketSlotPlan plan = strategy.place(request(groups("A", "B", "C", "D"), 2));
        assertEquals(8, plan.getSlots().size());
        assertTrue(plan.getSlots().stream().allMatch(BracketSlot::isUnresolvedPlaceholder));
    }

    @Test
    void oddGroupCountRejected() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> strategy.place(request(groups("A", "B", "C"), 2)));
        assertTrue(ex.getMessage().contains("MANUAL"));
    }

    @Test
    void advancePerGroupThreeRejected() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> strategy.place(request(groups("A", "B", "C", "D"), 3)));
        assertTrue(ex.getMessage().contains("MANUAL"));
    }

    @Test
    void twelveQualifiersByeCountAndRank1Priority() {
        BracketSlotPlan plan = strategy.place(request(groups("A", "B", "C", "D", "E", "F"), 2));
        assertEquals(12, plan.getCapacity().getParticipantCount());
        assertEquals(16, plan.getCapacity().getCapacity());
        assertEquals(4, plan.getCapacity().getByeCount());
        assertEquals(11, plan.getCapacity().getTotalActualSeries());
        long byeSlots = plan.getSlots().stream().filter(BracketSlot::isBye).count();
        assertEquals(4, byeSlots);
        plan.getSlots().stream().filter(BracketSlot::isBye).forEach(slot -> {
            assertNull(slot.getEventTeamId());
            assertNull(slot.getResolvedEventTeamId());
        });
        Set<String> byeOpponents = new HashSet<>();
        List<BracketSlot> slots = plan.getSlots();
        for (int i = 0; i < slots.size(); i += 2) {
            BracketSlot home = slots.get(i);
            BracketSlot away = slots.get(i + 1);
            if (home.isBye() && away.isBye()) {
                throw new AssertionError("BYE vs BYE");
            }
            if (home.isBye()) {
                byeOpponents.add(away.getDisplayCode());
            }
            if (away.isBye()) {
                byeOpponents.add(home.getDisplayCode());
            }
        }
        assertEquals(Set.of("A1", "B1", "C1", "D1"), byeOpponents);
        assertNoSameGroupFirstRound(plan);
        assertAllSourcesOnce(plan, 6, 2);
    }

    @Test
    void advancePerGroupOneFourGroups() {
        BracketSlotPlan plan = strategy.place(request(groups("A", "B", "C", "D"), 1));
        assertPairs(plan, "A1", "D1", "B1", "C1");
    }

    private static void assertPairs(BracketSlotPlan plan, String... codes) {
        List<BracketSlot> slots = plan.getSlots();
        assertEquals(codes.length, slots.size());
        for (int i = 0; i < codes.length; i++) {
            assertEquals(codes[i], slots.get(i).getDisplayCode());
        }
    }

    private static void assertNoSameGroupFirstRound(BracketSlotPlan plan) {
        List<BracketSlot> slots = plan.getSlots();
        for (int i = 0; i < slots.size(); i += 2) {
            BracketSlot home = slots.get(i);
            BracketSlot away = slots.get(i + 1);
            if (!home.isBye() && !away.isBye()) {
                assertTrue(!home.getSourceGroupId().equals(away.getSourceGroupId()),
                        home.getDisplayCode() + " vs " + away.getDisplayCode());
            }
        }
    }

    private static void assertAllSourcesOnce(BracketSlotPlan plan, int groupCount, int advance) {
        Set<String> codes = new HashSet<>();
        for (BracketSlot slot : plan.getSlots()) {
            if (!slot.isBye()) {
                assertTrue(codes.add(slot.getDisplayCode()), "duplicate " + slot.getDisplayCode());
            }
        }
        assertEquals(groupCount * advance, codes.size());
    }

    private static GroupCrossPlacementRequest request(List<GroupLayout> groups, int advance) {
        return GroupCrossPlacementRequest.builder()
                .sourceStageId(100L)
                .advancePerGroup(advance)
                .groups(groups)
                .build();
    }

    private static List<GroupLayout> groups(String... codes) {
        List<GroupLayout> list = new ArrayList<>();
        for (int i = 0; i < codes.length; i++) {
            list.add(new GroupLayout((long) (i + 1), codes[i], i + 1));
        }
        return list;
    }
}
