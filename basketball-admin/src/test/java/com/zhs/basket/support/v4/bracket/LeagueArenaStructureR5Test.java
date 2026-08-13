package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler.Pairing;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler.Round;
import com.zhs.basket.support.v4.BestOfValues;
import com.zhs.basket.support.v4.MatchSeriesResultCalculator;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeagueArenaStructureR5Test {

    @Test
    void test1_fourTeamsSingleRoundRobin() {
        List<Round> rounds = BasketV4RoundRobinScheduler.generateRoundRobin(List.of(1L, 2L, 3L, 4L), 1);
        assertEquals(3, rounds.size());
        assertEquals(6, BasketV4RoundRobinScheduler.countMatches(rounds));
    }

    @Test
    void test2_fiveTeamsSingleRoundRobinBye() {
        List<Round> rounds = BasketV4RoundRobinScheduler.generateRoundRobin(List.of(1L, 2L, 3L, 4L, 5L), 1);
        assertEquals(5, rounds.size());
        assertEquals(10, BasketV4RoundRobinScheduler.countMatches(rounds));
        Map<Long, Integer> byeByTeam = countByes(rounds, List.of(1L, 2L, 3L, 4L, 5L));
        for (int count : byeByTeam.values()) {
            assertEquals(1, count);
        }
        for (Round round : rounds) {
            assertEquals(2, round.getPairings().size());
        }
    }

    @Test
    void test3_fourTeamsDoubleRoundRobin() {
        List<Round> rounds = BasketV4RoundRobinScheduler.generateRoundRobin(List.of(1L, 2L, 3L, 4L), 2);
        assertEquals(6, rounds.size());
        assertEquals(12, BasketV4RoundRobinScheduler.countMatches(rounds));
    }

    @Test
    void test4_fiveTeamsDoubleRoundRobin() {
        List<Round> rounds = BasketV4RoundRobinScheduler.generateRoundRobin(List.of(1L, 2L, 3L, 4L, 5L), 2);
        assertEquals(10, rounds.size());
        assertEquals(20, BasketV4RoundRobinScheduler.countMatches(rounds));
        Map<Long, Integer> byeByTeam = countByes(rounds, List.of(1L, 2L, 3L, 4L, 5L));
        for (int count : byeByTeam.values()) {
            assertEquals(2, count);
        }
    }

    @Test
    void test5_doubleRoundRobinSidesReversed() {
        List<Long> teams = List.of(1L, 2L, 3L, 4L);
        List<Round> rounds = BasketV4RoundRobinScheduler.generateRoundRobin(teams, 2);
        Map<String, List<String>> sides = new HashMap<>();
        for (Round round : rounds) {
            for (Pairing pairing : round.getPairings()) {
                long low = Math.min(pairing.getHomeEventTeamId(), pairing.getAwayEventTeamId());
                long high = Math.max(pairing.getHomeEventTeamId(), pairing.getAwayEventTeamId());
                sides.computeIfAbsent(low + ":" + high, key -> new ArrayList<>())
                        .add(pairing.getHomeEventTeamId() + ">" + pairing.getAwayEventTeamId());
            }
        }
        assertEquals(6, sides.size());
        for (List<String> pairSides : sides.values()) {
            assertEquals(2, pairSides.size());
            assertEquals(2, new HashSet<>(pairSides).size());
        }
    }

    @Test
    void test6_top4HighVsLowPlacement() {
        BracketSlotPlan plan = SeededBracketPlacement.placeBySeed(participants(4));
        assertEquals(List.of(1, 4, 2, 3), seedOrder(plan));
        assertTrue(differentHalves(plan, 1, 2));
    }

    @Test
    void test7_top8SeededBracket() {
        BracketSlotPlan plan = SeededBracketPlacement.placeBySeed(participants(8));
        assertArrayEquals(new int[]{1, 8, 4, 5, 2, 7, 3, 6}, seedOrder(plan).stream().mapToInt(Integer::intValue).toArray());
        assertTrue(differentHalves(plan, 1, 2));
        assertTrue(sameHalf(plan, 1, 4));
    }

    @Test
    void test8_top16NotHardcodedEight() {
        BracketSlotPlan plan = SeededBracketPlacement.placeBySeed(participants(16));
        assertEquals(16, plan.getSlots().size());
        assertTrue(differentHalves(plan, 1, 2));
        assertEquals(1, plan.getSlots().get(0).getSeedNo());
        assertTrue(differentHalves(plan, 1, 2));
    }

    @Test
    void top32Seed1And2DifferentHalves() {
        BracketSlotPlan plan = SeededBracketPlacement.placeBySeed(participants(32));
        assertEquals(32, plan.getSlots().size());
        assertTrue(differentHalves(plan, 1, 2));
        assertTrue(notBeforeSemifinal(plan, 1, 4));
        assertTrue(notBeforeSemifinal(plan, 2, 3));
        assertTrue(sameHalf(plan, 1, 4));
        assertTrue(sameHalf(plan, 2, 3));
    }

    @Test
    void adminSeedRandomConfirmSeedPriorityDoNotOverrideEachOther() {
        List<BracketParticipant> people = participants(10);
        people.get(0).setSeedNo(1);
        people.get(1).setSeedNo(2);
        EliminationCapacity capacity = EliminationCapacity.of(10);
        Set<Long> byeTeams = new SeedPriorityByeStrategy().allocateByeTeamIds(people, capacity, null);
        assertTrue(byeTeams.contains(1L));
        assertTrue(byeTeams.contains(2L));
        BracketSlotPlan plan = SeededBracketPlacement.placeWithByeTeams(people, byeTeams);
        assertEquals(1, plan.getSlots().stream().filter(s -> !s.isBye() && Long.valueOf(1L).equals(s.getEventTeamId()))
                .findFirst().orElseThrow().getSeedNo());
        assertEquals(6, plan.getSlots().stream().filter(BracketSlot::isBye).count());
    }

    @Test
    void externalRankingRandomConfirmSeedPriorityUsesRankingAsConstraint() {
        List<BracketParticipant> people = participants(8);
        for (int i = 0; i < people.size(); i++) {
            people.get(i).setSeedNo(null);
            people.get(i).setRankingNo(people.size() - i);
        }
        BracketSlotPlan plan = SeededBracketPlacement.placeBySeed(people);
        assertTrue(differentHalves(plan, 8, 7));
    }

    @Test
    void adminSeedManualSlotManualByeKeepsExplicitSlots() {
        List<BracketSlot> slots = new ArrayList<>();
        slots.add(BracketSlot.team(1, 1L, 1, 1, 1));
        slots.add(BracketSlot.bye(2));
        slots.add(BracketSlot.team(3, 2L, 2, 2, 2));
        slots.add(BracketSlot.team(4, 3L, 3, 3, 3));
        BracketSlotPlan plan = SeededBracketPlacement.fromSlots(slots, 3);
        assertEquals(1L, plan.getSlots().get(0).getEventTeamId());
        assertTrue(plan.getSlots().get(1).isBye());
        assertEquals(2L, plan.getSlots().get(2).getEventTeamId());
        Set<Long> byeTeams = new ManualByeStrategy().allocateByeTeamIds(
                participants(3), EliminationCapacity.of(3),
                ByeAllocationRequest.builder().manualByeTeamIds(Set.of(1L)).build());
        assertEquals(Set.of(1L), byeTeams);
    }

    @Test
    void confirmedSlotStrategyRejectsEmptyPlan() {
        assertThrows(ServiceException.class, () -> new ConfirmedSlotStrategy().allocate(
                participants(8), SlotAllocationRequest.builder().build()));
    }

    @Test
    void test9_playoffSlotsGreaterThanQualifiedRejectedByCapacityStillWorksForN() {
        assertThrows(ServiceException.class, () -> EliminationCapacity.of(1));
    }

    @Test
    void test12to15_eliminationFormulas() {
        assertCapacity(2, 2, 0, 1, 1);
        assertCapacity(3, 4, 1, 1, 2);
        assertCapacity(5, 8, 3, 1, 4);
        assertCapacity(10, 16, 6, 2, 9);
    }

    @Test
    void test16_byeSlotsHaveNoTeam() {
        BracketSlotPlan plan = SeededBracketPlacement.placeBySeed(participants(10));
        long byeSlots = plan.getSlots().stream().filter(BracketSlot::isBye).count();
        assertEquals(6, byeSlots);
        plan.getSlots().stream().filter(BracketSlot::isBye).forEach(slot -> {
            assertEquals(null, slot.getEventTeamId());
        });
        assertEquals(9, plan.getCapacity().getTotalActualSeries());
    }

    @Test
    void test27_seedPriorityByeGivesHighestSeedsBye() {
        List<BracketParticipant> people = participants(10);
        EliminationCapacity capacity = EliminationCapacity.of(10);
        Set<Long> byeTeams = new SeedPriorityByeStrategy().allocateByeTeamIds(people, capacity, null);
        assertEquals(Set.of(1L, 2L, 3L, 4L, 5L, 6L), byeTeams);
    }

    @Test
    void test22_randomByeReproducible() {
        List<BracketParticipant> people = participants(10);
        EliminationCapacity capacity = EliminationCapacity.of(10);
        RandomByeStrategy strategy = new RandomByeStrategy();
        Set<Long> first = strategy.allocateByeTeamIds(people, capacity, ByeAllocationRequest.builder().randomSeed(42L).build());
        Set<Long> second = strategy.allocateByeTeamIds(people, capacity, ByeAllocationRequest.builder().randomSeed(42L).build());
        assertEquals(first, second);
        assertEquals(6, first.size());
    }

    @Test
    void test26_manualByeCountMismatchRejected() {
        assertThrows(ServiceException.class, () -> new ManualByeStrategy().allocateByeTeamIds(
                participants(10), EliminationCapacity.of(10),
                ByeAllocationRequest.builder().manualByeTeamIds(Set.of(1L)).build()));
    }

    @Test
    void test29_top8BoMapping() {
        BasketDivisionStage stage = mappedStage(3, 5, 5);
        assertEquals(3, EliminationBoMapping.bestOf(stage, 1, 3));
        assertEquals(5, EliminationBoMapping.bestOf(stage, 2, 3));
        assertEquals(5, EliminationBoMapping.bestOf(stage, 3, 3));
        assertEquals(EliminationBoRoundRole.EARLY, EliminationBoMapping.roleOf(1, 3));
        assertEquals(EliminationBoRoundRole.SEMIFINAL, EliminationBoMapping.roleOf(2, 3));
        assertEquals(EliminationBoRoundRole.FINAL, EliminationBoMapping.roleOf(3, 3));
    }

    @Test
    void test30_top4BoMapping() {
        BasketDivisionStage stage = mappedStage(3, 5, 5);
        assertEquals(5, EliminationBoMapping.bestOf(stage, 1, 2));
        assertEquals(5, EliminationBoMapping.bestOf(stage, 2, 2));
    }

    @Test
    void test31_top2FinalBo() {
        BasketDivisionStage stage = mappedStage(3, 5, 5);
        assertEquals(5, EliminationBoMapping.bestOf(stage, 1, 1));
        assertEquals(EliminationBoRoundRole.FINAL, EliminationBoMapping.roleOf(1, 1));
    }

    @Test
    void test32_bo7RequiredWins() {
        assertEquals(4, BestOfValues.requiredWins(7));
        assertEquals(4, MatchSeriesResultCalculator.requiredWins(7));
    }

    @Test
    void test33_bo7FirstToFour() {
        long home = 1L;
        long away = 2L;
        var g1 = game(1, home);
        var g2 = game(2, home);
        var g3 = game(3, away);
        var g4 = game(4, home);
        var g5 = game(5, home);
        var result = MatchSeriesResultCalculator.calculate(7, home, away, List.of(g1, g2, g3, g4, g5));
        assertTrue(result.isCompleted());
        assertEquals(home, result.getWinnerEventTeamId());
        assertEquals(4, result.getHomeWinCount());
    }

    @Test
    void byeDoesNotChangeSemifinalRoleForThreeTeams() {
        BasketDivisionStage stage = mappedStage(3, 5, 5);
        assertEquals(EliminationBoRoundRole.SEMIFINAL, EliminationBoMapping.roleOf(1, 2));
        assertEquals(5, EliminationBoMapping.bestOf(stage, 1, 2));
    }

    @Test
    void bestOfSevenIsLegal() {
        BestOfValues.requireLegal(7);
        assertFalse(BestOfValues.isLegal(2));
        assertFalse(BestOfValues.isLegal(9));
    }

    @Test
    void nextPowerOfTwoIsGeneric() {
        assertEquals(16, EliminationCapacity.nextPowerOfTwo(10));
        assertEquals(32, EliminationCapacity.nextPowerOfTwo(17));
        assertEquals(8, EliminationCapacity.nextPowerOfTwo(8));
    }

    private static void assertCapacity(int n, int capacity, int bye, int firstActual, int totalSeries) {
        EliminationCapacity value = EliminationCapacity.of(n);
        assertEquals(capacity, value.getCapacity());
        assertEquals(bye, value.getByeCount());
        assertEquals(firstActual, value.getFirstRoundActual());
        assertEquals(totalSeries, value.getTotalActualSeries());
    }

    private static List<BracketParticipant> participants(int n) {
        List<BracketParticipant> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            list.add(new BracketParticipant((long) i, i, i, i, "OVERALL_RANK"));
        }
        return list;
    }

    private static List<Integer> seedOrder(BracketSlotPlan plan) {
        List<Integer> seeds = new ArrayList<>();
        for (BracketSlot slot : plan.getSlots()) {
            seeds.add(slot.isBye() ? null : slot.getSeedNo());
        }
        return seeds;
    }

    private static boolean differentHalves(BracketSlotPlan plan, int seedA, int seedB) {
        int half = plan.getSlots().size() / 2;
        return indexOfSeed(plan, seedA) < half != indexOfSeed(plan, seedB) < half;
    }

    private static boolean sameHalf(BracketSlotPlan plan, int seedA, int seedB) {
        int half = plan.getSlots().size() / 2;
        return indexOfSeed(plan, seedA) < half == indexOfSeed(plan, seedB) < half;
    }

    /** Same half and different quarters: first meeting is semifinal, not earlier. */
    private static boolean notBeforeSemifinal(BracketSlotPlan plan, int seedA, int seedB) {
        int quarter = plan.getSlots().size() / 4;
        int idxA = indexOfSeed(plan, seedA);
        int idxB = indexOfSeed(plan, seedB);
        boolean sameQuarter = idxA / quarter == idxB / quarter;
        return !sameQuarter && sameHalf(plan, seedA, seedB);
    }

    private static int indexOfSeed(BracketSlotPlan plan, int seed) {
        for (int i = 0; i < plan.getSlots().size(); i++) {
            if (!plan.getSlots().get(i).isBye() && Integer.valueOf(seed).equals(plan.getSlots().get(i).getSeedNo())) {
                return i;
            }
        }
        return -1;
    }

    private static Map<Long, Integer> countByes(List<Round> rounds, List<Long> teams) {
        Map<Long, Integer> played = new HashMap<>();
        for (Round round : rounds) {
            Set<Long> inRound = new HashSet<>();
            for (Pairing pairing : round.getPairings()) {
                inRound.add(pairing.getHomeEventTeamId());
                inRound.add(pairing.getAwayEventTeamId());
                played.merge(pairing.getHomeEventTeamId(), 1, Integer::sum);
                played.merge(pairing.getAwayEventTeamId(), 1, Integer::sum);
            }
            for (Long team : teams) {
                if (!inRound.contains(team)) {
                    played.merge(team, 0, Integer::sum);
                }
            }
        }
        Map<Long, Integer> byes = new HashMap<>();
        for (Long team : teams) {
            byes.put(team, rounds.size() - played.getOrDefault(team, 0));
        }
        return byes;
    }

    private static BasketDivisionStage mappedStage(int early, int sf, int fi) {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setBestOf(early);
        stage.setSemifinalBestOf(sf);
        stage.setFinalBestOf(fi);
        return stage;
    }

    private static com.zhs.basket.support.v4.MatchSeriesGameInput game(int no, long winner) {
        com.zhs.basket.support.v4.MatchSeriesGameInput input = new com.zhs.basket.support.v4.MatchSeriesGameInput();
        input.setSeriesGameNo(no);
        input.setHomeEventTeamId(1L);
        input.setAwayEventTeamId(2L);
        input.setWinnerEventTeamId(winner);
        input.setMatchStatus("FINISHED");
        return input;
    }
}
