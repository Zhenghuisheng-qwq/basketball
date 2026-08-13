package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.domain.v4.BasketStagePairingEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderModelR6Test {

    @Test
    void groupRankSlotIsNotAFakeTeam() {
        BracketSlot slot = BracketSlot.groupRank(1, 10L, 1L, "A", 1);
        assertNull(slot.getEventTeamId());
        assertNull(slot.getResolvedEventTeamId());
        assertEquals("PENDING", slot.getResolutionStatus());
        assertTrue(slot.isUnresolvedPlaceholder());
        assertEquals("A1", slot.getDisplayCode());
        assertEquals("A组第1名", slot.getDisplayName());
    }

    @Test
    void byeIsNotPendingPlaceholder() {
        BracketSlot bye = BracketSlot.bye(2);
        assertTrue(bye.isBye());
        assertNull(bye.getEventTeamId());
        assertFalse(bye.isUnresolvedPlaceholder());
        assertEquals("BYE", bye.getResolutionStatus());
    }

    @Test
    void labelsComeFromBackend() {
        assertEquals("D2", ParticipantSourceLabels.displayCode("D", 2));
        assertEquals("D组第2名", ParticipantSourceLabels.displayName("D", 2));
    }

    @Test
    void unresolvedStatusIgnoresByeAndPendingGroupRankIsUnresolved() {
        BasketStagePairingEntry pending = new BasketStagePairingEntry();
        pending.setSourceType("GROUP_RANK");
        pending.setResolutionStatus("PENDING");
        BasketStagePairingEntry bye = new BasketStagePairingEntry();
        bye.setSourceType("BYE");
        bye.setIsBye(1);
        bye.setResolutionStatus("BYE");
        BasketStagePairingEntry resolved = new BasketStagePairingEntry();
        resolved.setSourceType("GROUP_RANK");
        resolved.setResolutionStatus("RESOLVED");
        resolved.setResolvedEventTeamId(1001L);

        assertTrue(StructureParticipantStatus.hasUnresolvedPlaceholders(List.of(pending, bye)));
        assertFalse(StructureParticipantStatus.isParticipantResolved(List.of(pending, resolved)));
        assertTrue(StructureParticipantStatus.isParticipantResolved(List.of(resolved, bye)));
        assertFalse(StructureParticipantStatus.hasUnresolvedPlaceholders(List.of(resolved, bye)));
    }

    @Test
    void groupRankPlacementOnlyWhenPreviousIsGrouped() {
        com.zhs.basket.domain.v4.BasketDivisionStage knockout = new com.zhs.basket.domain.v4.BasketDivisionStage();
        knockout.setPairingRule("GROUP_CROSS");
        knockout.setCompetitionFormat("SINGLE_ELIMINATION");
        com.zhs.basket.domain.v4.BasketDivisionStage group = new com.zhs.basket.domain.v4.BasketDivisionStage();
        group.setCompetitionFormat("GROUP_STAGE");
        group.setGroupingMode("POT_RANDOM");
        assertTrue(GroupRankPlacementSupport.usesGroupRankPlacement(knockout, group));

        com.zhs.basket.domain.v4.BasketDivisionStage rr = new com.zhs.basket.domain.v4.BasketDivisionStage();
        rr.setCompetitionFormat("ROUND_ROBIN");
        rr.setGroupingMode("NONE");
        assertFalse(GroupRankPlacementSupport.usesGroupRankPlacement(knockout, rr));
    }
}
