package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.support.v4.stage.PlayoffBracketSupport;
import com.zhs.common.exception.ServiceException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Standard seeded bracket placement. Extends PlayoffBracketSupport; does not fork a second matrix.
 */
public final class SeededBracketPlacement {

    private SeededBracketPlacement() {
    }

    /**
     * Place seed 1..N into a capacity bracket. Seeds N+1..capacity are BYE (seed-priority BYE).
     */
    public static BracketSlotPlan placeBySeed(List<BracketParticipant> participants) {
        if (participants == null || participants.size() < 2) {
            throw new ServiceException("淘汰赛至少需要 2 支正式参赛队");
        }
        List<BracketParticipant> ordered = new ArrayList<>(participants);
        ordered.sort(Comparator
                .comparing(BracketParticipant::getSeedNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BracketParticipant::getRankingNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BracketParticipant::getEventTeamId));
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getSeedNo() == null) {
                ordered.get(i).setSeedNo(i + 1);
            }
        }
        return placeSeedMap(indexBySeed(ordered), ordered.size());
    }

    /**
     * Place explicit BYE teams into structural high-seed slots, remaining teams into leftover seeds.
     */
    public static BracketSlotPlan placeWithByeTeams(List<BracketParticipant> participants, Set<Long> byeTeamIds) {
        EliminationCapacity capacity = EliminationCapacity.of(participants.size());
        if (byeTeamIds == null) {
            byeTeamIds = Set.of();
        }
        if (byeTeamIds.size() != capacity.getByeCount()) {
            throw new ServiceException("BYE 队数必须等于 capacity - N");
        }
        List<BracketParticipant> ordered = new ArrayList<>(participants);
        ordered.sort(Comparator
                .comparing(BracketParticipant::getSeedNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BracketParticipant::getRankingNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BracketParticipant::getEventTeamId));

        List<BracketParticipant> byeTeams = new ArrayList<>();
        List<BracketParticipant> playTeams = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (BracketParticipant participant : ordered) {
            if (!seen.add(participant.getEventTeamId())) {
                throw new ServiceException("参赛队重复，无法分配签位");
            }
            if (byeTeamIds.contains(participant.getEventTeamId())) {
                byeTeams.add(participant);
            } else {
                playTeams.add(participant);
            }
        }
        if (byeTeams.size() != capacity.getByeCount()) {
            throw new ServiceException("BYE 队必须来自当前正式参赛名单");
        }

        Map<Integer, BracketParticipant> bySeed = new HashMap<>();
        int seed = 1;
        for (BracketParticipant byeTeam : byeTeams) {
            bySeed.put(seed++, byeTeam);
        }
        for (BracketParticipant playTeam : playTeams) {
            bySeed.put(seed++, playTeam);
        }
        return placeSeedMap(bySeed, participants.size());
    }

    public static BracketSlotPlan fromSlots(List<BracketSlot> slots, int participantCount) {
        EliminationCapacity capacity = EliminationCapacity.of(participantCount);
        if (slots == null || slots.size() != capacity.getCapacity()) {
            throw new ServiceException("签位数量必须等于 bracket capacity");
        }
        List<BracketSlot> ordered = new ArrayList<>(slots);
        ordered.sort(Comparator.comparingInt(BracketSlot::getSlotNo));
        BracketSlotPlan plan = new BracketSlotPlan();
        plan.setCapacity(capacity);
        plan.setSlots(ordered);
        return plan;
    }

    private static Map<Integer, BracketParticipant> indexBySeed(List<BracketParticipant> ordered) {
        Map<Integer, BracketParticipant> bySeed = new HashMap<>();
        int seed = 1;
        for (BracketParticipant participant : ordered) {
            int assigned = participant.getSeedNo() != null ? participant.getSeedNo() : seed;
            if (bySeed.put(assigned, participant) != null) {
                throw new ServiceException("种子号重复: " + assigned);
            }
            seed = Math.max(seed, assigned) + 1;
        }
        return bySeed;
    }

    private static BracketSlotPlan placeSeedMap(Map<Integer, BracketParticipant> bySeed, int participantCount) {
        EliminationCapacity capacity = EliminationCapacity.of(participantCount);
        int[] order = PlayoffBracketSupport.firstRoundSeedOrder(capacity.getCapacity());
        BracketSlotPlan plan = new BracketSlotPlan();
        plan.setCapacity(capacity);
        List<BracketSlot> slots = new ArrayList<>(order.length);
        for (int i = 0; i < order.length; i++) {
            int seed = order[i];
            BracketParticipant participant = bySeed.get(seed);
            if (participant != null) {
                BracketSlot slot = BracketSlot.team(i + 1, participant.getEventTeamId(),
                        seed, participant.getRankingNo(), participant.getSourceRank());
                slot.setSourceType(participant.getSourceType());
                slots.add(slot);
            } else {
                slots.add(BracketSlot.bye(i + 1));
            }
        }
        plan.setSlots(slots);
        return plan;
    }
}
